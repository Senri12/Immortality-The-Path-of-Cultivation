package immortality.client;

import immortality.cultivation.BodyDefinition;
import immortality.cultivation.BodyRegistry;
import immortality.cultivation.CultivationStage;
import immortality.network.ResearchActionPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import org.joml.Matrix3x2fStack;
import immortality.Immortality;

public final class AltarResearchScreen extends Screen {
	private static final Identifier BOOK_MAP = Identifier.fromNamespaceAndPath(Immortality.MOD_ID, "textures/gui/book_map.png");
	private static final Identifier BOOK_PAGE = Identifier.fromNamespaceAndPath(Immortality.MOD_ID, "textures/gui/book_page.png");
	private static final Identifier NODE_FRAMES = Identifier.fromNamespaceAndPath(Immortality.MOD_ID, "textures/gui/node_frames.png");
	private static final Identifier BOOK_TABS = Identifier.fromNamespaceAndPath(Immortality.MOD_ID, "textures/gui/book_tabs.png");

	private static final float UI_TEXT_SCALE = 0.60F;
	private static final int PANEL_WIDTH = 256;
	private static final int PANEL_HEIGHT = 256;
	
	private static final int NODE_WIDTH = 24;
	private static final int NODE_HEIGHT = 24;
	private static final int COLUMN_STEP = 32;
	private static final int ROW_STEP = 32;
	private static final int ROW_SPACING = 9;

	private static final String[] CATEGORIES = { "qi", "body", "breakthrough", "beast", "soul", "dao" };
	private static final String[] CATEGORY_LABELS = { "Qi", "Bd", "Bt", "Bs", "Sl", "Do" };

	private final CultivationStage stage;
	private final int currentQi;
	private final int maxQi;
	private final double purity;
	private final double stability;
	private final double deviation;
	private final BodyDefinition body;
	private final String manualId;
	private final String preparedResearchId;
	private final List<ResearchEntry> researches;
	private final Map<String, ResearchEntry> researchById;

	private final List<Button> nodeButtons = new ArrayList<>();
	private Button studyButton;
	private Button breakthroughButton;
	private Button backToMapButton;
	private Button closeButton;
	private Button prevDetailsButton;
	private Button nextDetailsButton;

	private String currentCategory = "qi";
	private int selectedIndex = 0;
	private double scrollX = 40.0D;
	private double scrollY = 60.0D;
	private boolean pageViewMode = false;
	private int detailsPage = 0;
	private static final int DETAILS_LINES_PER_PAGE = 7;

	// Drag state fields
	private boolean isDragging = false;
	private double lastDragX = 0.0D;
	private double lastDragY = 0.0D;

	public AltarResearchScreen(CompoundTag data) {
		super(Component.translatable("screen.immortality.altar.title"));
		this.stage = CultivationStage.valueOf(data.getString("Stage").orElse(CultivationStage.MORTAL.name()));
		this.currentQi = data.getInt("CurrentQi").orElse(0);
		this.maxQi = data.getInt("MaxQi").orElse(this.stage.qiCapacity());
		this.purity = data.getDouble("Purity").orElse(0.0D);
		this.stability = data.getDouble("Stability").orElse(0.0D);
		this.deviation = data.getDouble("Deviation").orElse(0.0D);
		this.body = BodyRegistry.get(data.getString("Body").orElse(BodyRegistry.NONE_ID));
		this.manualId = data.getString("Manual").orElse("none");
		this.preparedResearchId = data.getString("PreparedResearch").orElse("");
		this.researches = readResearches(data.getList("Researches").orElseGet(ListTag::new));
		this.researchById = new HashMap<>();
		for (ResearchEntry research : this.researches) {
			this.researchById.put(research.id(), research);
		}
	}

	@Override
	protected void init() {
		super.init();
		this.nodeButtons.clear();
		int panelLeft = this.width / 2 - PANEL_WIDTH / 2;
		int panelTop = this.height / 2 - PANEL_HEIGHT / 2;

		// Create invisible node buttons to overlay on top of scrollable canvas
		for (int i = 0; i < this.researches.size(); i++) {
			int index = i;
			Button button = Button.builder(Component.empty(), value -> {
				this.selectedIndex = index;
				this.pageViewMode = true;
				this.detailsPage = 0;
				this.isDragging = false;
				refreshWidgets();
			}).bounds(panelLeft, panelTop, NODE_WIDTH, NODE_HEIGHT).build();
			this.nodeButtons.add(this.addWidget(button));
		}

		// Study/prepare button
		this.studyButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.immortality.altar.prepare"), value -> {
			ResearchEntry entry = selectedResearch();
			if (entry != null && !entry.known()) {
				ClientPlayNetworking.send(new ResearchActionPayload(ResearchActionPayload.ACTION_PREPARE, entry.id()));
			}
		}).bounds(panelLeft + 132, panelTop + 196, 52, 20).build());

		// Breakthrough screen button
		this.breakthroughButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.immortality.altar.breakthrough"), value ->
			ClientPlayNetworking.send(new ResearchActionPayload(ResearchActionPayload.ACTION_OPEN_BREAKTHROUGH, ""))
		).bounds(panelLeft + 190, panelTop + 196, 44, 20).build());

		// Back to map button
		this.backToMapButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.immortality.manual.to_altar"), value -> {
			this.pageViewMode = false;
			refreshWidgets();
		}).bounds(panelLeft + 24, panelTop + 196, 52, 20).build());

		// Page turners for details list inside pageViewMode
		this.prevDetailsButton = this.addRenderableWidget(Button.builder(Component.literal("<"), value -> {
			this.detailsPage = Math.max(0, this.detailsPage - 1);
			refreshWidgets();
		}).bounds(panelLeft + 132, panelTop + 172, 16, 16).build());

		this.nextDetailsButton = this.addRenderableWidget(Button.builder(Component.literal(">"), value -> {
			this.detailsPage = Math.min(maxDetailsPage(), this.detailsPage + 1);
			refreshWidgets();
		}).bounds(panelLeft + 218, panelTop + 172, 16, 16).build());

		// Map Close Button
		this.closeButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), value -> onClose())
			.bounds(panelLeft + PANEL_WIDTH / 2 - 25, panelTop + PANEL_HEIGHT + 4, 50, 20)
			.build());

		refreshWidgets();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// Update drag panning delta on render frame
		if (this.isDragging && !this.pageViewMode) {
			double dx = mouseX - this.lastDragX;
			double dy = mouseY - this.lastDragY;
			this.scrollX += dx;
			this.scrollY += dy;
			this.lastDragX = mouseX;
			this.lastDragY = mouseY;
			// Limit map scrolling bounds
			this.scrollX = Math.max(-300, Math.min(150, this.scrollX));
			this.scrollY = Math.max(-250, Math.min(150, this.scrollY));
			refreshWidgets();
		}

		graphics.fill(0, 0, this.width, this.height, 0x7A08060E);
		int panelLeft = this.width / 2 - PANEL_WIDTH / 2;
		int panelTop = this.height / 2 - PANEL_HEIGHT / 2;

		if (this.pageViewMode) {
			// Render detailed book view mode
			graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK_PAGE, panelLeft, panelTop, 0.0F, 0.0F, PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);
			drawDetailsBook(graphics, panelLeft, panelTop);
		} else {
			// Render category tabs on the left edge of the wood frame
			for (int i = 0; i < CATEGORIES.length; i++) {
				int tabY = panelTop + 24 + i * 24;
				int activeOffset = CATEGORIES[i].equals(this.currentCategory) ? 20 : 0;
				graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK_TABS, panelLeft - 20, tabY, 0.0F, activeOffset, 24, 16, 32, 128);
				String tabKey = "screen.immortality.altar.tab." + CATEGORIES[i];
				String label = Component.translatable(tabKey).getString();
				int textColor = CATEGORIES[i].equals(this.currentCategory) ? 0xFF3C1F10 : 0xFFDFD5C6;
				drawScaledText(graphics, Component.literal(label), panelLeft - 15, tabY + 4, 0.55F, textColor);
			}

			// Render book map frame
			graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK_MAP, panelLeft, panelTop, 0.0F, 0.0F, PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);

			// Draw Map contents (clipped to the viewport area inside the wood frame)
			int viewportLeft = panelLeft + 12;
			int viewportTop = panelTop + 12;
			int viewportWidth = 232;
			int viewportHeight = 232;

			graphics.enableScissor(viewportLeft, viewportTop, viewportLeft + viewportWidth, viewportTop + viewportHeight);
			drawConnections(graphics, viewportLeft, viewportTop);
			drawNodes(graphics, viewportLeft, viewportTop);
			graphics.disableScissor();

			// Draw current category overlay
			drawScaledText(graphics, Component.translatable("screen.immortality.altar.category_" + this.currentCategory), panelLeft + 16, panelTop + 16, UI_TEXT_SCALE, 0xFF3C1F10);

			// Render tooltips for hovered category tabs
			for (int i = 0; i < CATEGORIES.length; i++) {
				int tabY = panelTop + 24 + i * 24;
				if (mouseX >= panelLeft - 20 && mouseX < panelLeft && mouseY >= tabY && mouseY < tabY + 16) {
					String key = "screen.immortality.altar.category_" + CATEGORIES[i];
					graphics.setTooltipForNextFrame(Component.translatable(key), mouseX, mouseY);
				}
			}
		}

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (!this.pageViewMode) {
			int panelLeft = this.width / 2 - PANEL_WIDTH / 2;
			int panelTop = this.height / 2 - PANEL_HEIGHT / 2;
			
			// Click tabs check
			for (int i = 0; i < CATEGORIES.length; i++) {
				int tabY = panelTop + 24 + i * 24;
				if (event.x() >= panelLeft - 20 && event.x() < panelLeft && event.y() >= tabY && event.y() < tabY + 16) {
					this.currentCategory = CATEGORIES[i];
					this.scrollX = 40.0D;
					this.scrollY = 60.0D;
					this.isDragging = false;
					refreshWidgets();
					return true;
				}
			}

			// Map viewport check to start dragging
			int viewportLeft = panelLeft + 12;
			int viewportTop = panelTop + 12;
			int viewportWidth = 232;
			int viewportHeight = 232;
			if (event.x() >= viewportLeft && event.x() < viewportLeft + viewportWidth &&
				event.y() >= viewportTop && event.y() < viewportTop + viewportHeight) {
				this.isDragging = true;
				this.lastDragX = event.x();
				this.lastDragY = event.y();
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (event.button() == 0) {
			this.isDragging = false;
		}
		return super.mouseReleased(event);
	}

	private void drawConnections(GuiGraphics graphics, int viewportLeft, int viewportTop) {
		for (ResearchEntry entry : this.researches) {
			if (!entry.category().equals(this.currentCategory)) {
				continue;
			}
			int startX = (int) (viewportLeft + this.scrollX + entry.column() * COLUMN_STEP + NODE_WIDTH / 2);
			int startY = (int) (viewportTop + this.scrollY + entry.row() * ROW_STEP + NODE_HEIGHT / 2);
			for (String prerequisiteId : entry.prerequisites()) {
				ResearchEntry prerequisite = this.researchById.get(prerequisiteId);
				if (prerequisite == null || !prerequisite.category().equals(this.currentCategory)) {
					continue;
				}
				int endX = (int) (viewportLeft + this.scrollX + prerequisite.column() * COLUMN_STEP + NODE_WIDTH / 2);
				int endY = (int) (viewportTop + this.scrollY + prerequisite.row() * ROW_STEP + NODE_HEIGHT / 2);
				int color = entry.prerequisitesMet() ? 0xFF7FD48B : 0xFF5C495E;
				drawConnectionLine(graphics, endX, endY, startX, startY, color);
			}
		}
	}

	private void drawConnectionLine(GuiGraphics graphics, int startX, int startY, int endX, int endY, int color) {
		int horizontalStart = Math.min(startX, endX);
		int horizontalEnd = Math.max(startX, endX);
		graphics.fill(horizontalStart, startY - 1, horizontalEnd, startY + 1, color);
		int verticalStart = Math.min(startY, endY);
		int verticalEnd = Math.max(startY, endY);
		graphics.fill(endX - 1, verticalStart, endX + 1, verticalEnd, color);
	}

	private void drawNodes(GuiGraphics graphics, int viewportLeft, int viewportTop) {
		for (int i = 0; i < this.researches.size(); i++) {
			ResearchEntry entry = this.researches.get(i);
			if (!entry.category().equals(this.currentCategory)) {
				continue;
			}
			int nx = (int) (viewportLeft + this.scrollX + entry.column() * COLUMN_STEP);
			int ny = (int) (viewportTop + this.scrollY + entry.row() * ROW_STEP);

			// Determine node style (circle = basic, hexagon = milestone)
			boolean isMilestone = entry.id().contains("method") || entry.id().contains("blueprint") || entry.id().contains("seal");
			int vOffset = isMilestone ? 24 : 0;
			int uOffset = entry.known() ? 48 : entry.prepared() ? 24 : 0;

			// Draw frame
			graphics.blit(RenderPipelines.GUI_TEXTURED, NODE_FRAMES, nx, ny, uOffset, vOffset, NODE_WIDTH, NODE_HEIGHT, 72, 48);

			// Draw abbreviated text
			drawScaledText(graphics, entry.shortTitle(), nx + 4, ny + 9, 0.50F, 0xFF3C1F10);
		}
	}

	private void drawDetailsBook(GuiGraphics graphics, int panelLeft, int panelTop) {
		ResearchEntry entry = selectedResearch();
		if (entry == null) {
			return;
		}

		// Left page details (Title, category, description)
		int ly = panelTop + 24;
		graphics.drawString(this.font, entry.title(), panelLeft + 22, ly, 0xFF3C1F10, false);
		ly += 12;
		drawScaledText(graphics, Component.translatable(categoryKey(entry.category())), panelLeft + 22, ly, UI_TEXT_SCALE, categoryColor(entry.category()));
		ly += 10;
		ly = drawWrapped(graphics, entry.description(), panelLeft + 22, ly, 92, 0xFF4A3A2A, 5);
		ly += 4;

		// Status rows on the left page
		ly = drawTableRow(graphics, panelLeft + 22, ly, Component.translatable("screen.immortality.altar.row_status"), statusComponent(entry), statusColor(entry));
		ly = drawTableRow(graphics, panelLeft + 22, ly, Component.translatable("screen.immortality.altar.row_cost"), Component.literal(String.valueOf(entry.qiCost())), entry.qiMet() ? 0xFF1B5E20 : 0xFFB71C1C);
		drawTableRow(graphics, panelLeft + 22, ly, Component.translatable("screen.immortality.altar.row_required_stage"), CultivationStage.valueOf(entry.requiredStage()).displayNameComponent(), entry.stageMet() ? 0xFF1B5E20 : 0xFFB71C1C);

		// Right page (Requirements)
		int ry = panelTop + 24;
		drawScaledText(graphics, Component.translatable("screen.immortality.altar.requirements"), panelLeft + 138, ry, UI_TEXT_SCALE, 0xFF3C1F10);
		ry += 10;
		drawRequirementPage(graphics, panelLeft + 138, ry, entry);
	}

	private void drawRequirementPage(GuiGraphics graphics, int x, int y, ResearchEntry entry) {
		List<DetailLine> lines = requirementLines(entry);
		int start = this.detailsPage * DETAILS_LINES_PER_PAGE;
		int end = Math.min(start + DETAILS_LINES_PER_PAGE, lines.size());
		int currentY = y;
		for (int i = start; i < end; i++) {
			DetailLine line = lines.get(i);
			drawScaledText(graphics, line.text(), x, currentY, UI_TEXT_SCALE, line.color());
			currentY += ROW_SPACING;
		}
	}

	private int drawTableRow(GuiGraphics graphics, int x, int y, Component label, Component value, int valueColor) {
		drawScaledText(graphics, label, x, y, UI_TEXT_SCALE, 0xFF4A3A2A);
		drawScaledText(graphics, value, x + 50, y, UI_TEXT_SCALE, valueColor);
		return y + ROW_SPACING;
	}

	private int drawWrapped(GuiGraphics graphics, Component text, int x, int y, int maxWidth, int color, int maxLines) {
		int lineY = y;
		List<FormattedCharSequence> lines = this.font.split(text, maxWidth);
		int count = Math.min(lines.size(), maxLines);
		for (int i = 0; i < count; i++) {
			drawScaledText(graphics, lines.get(i), x, lineY, UI_TEXT_SCALE, color);
			lineY += ROW_SPACING - 1;
		}
		return lineY;
	}

	private void drawScaledText(GuiGraphics graphics, Component text, int x, int y, float scale, int color) {
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.scale(scale, scale);
		graphics.drawString(this.font, text, Math.round(x / scale), Math.round(y / scale), color, false);
		pose.popMatrix();
	}

	private void drawScaledText(GuiGraphics graphics, FormattedCharSequence text, int x, int y, float scale, int color) {
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.scale(scale, scale);
		graphics.drawString(this.font, text, Math.round(x / scale), Math.round(y / scale), color, false);
		pose.popMatrix();
	}

	private void refreshWidgets() {
		int panelLeft = this.width / 2 - PANEL_WIDTH / 2;
		int panelTop = this.height / 2 - PANEL_HEIGHT / 2;

		int viewportLeft = panelLeft + 12;
		int viewportTop = panelTop + 12;
		int viewportWidth = 232;
		int viewportHeight = 232;

		// Move node buttons dynamically
		for (int i = 0; i < this.researches.size(); i++) {
			ResearchEntry entry = this.researches.get(i);
			Button button = this.nodeButtons.get(i);
			if (this.pageViewMode || !entry.category().equals(this.currentCategory)) {
				button.visible = false;
				continue;
			}
			int nx = (int) (viewportLeft + this.scrollX + entry.column() * COLUMN_STEP);
			int ny = (int) (viewportTop + this.scrollY + entry.row() * ROW_STEP);
			button.setX(nx);
			button.setY(ny);
			boolean inViewport = nx >= viewportLeft && nx + NODE_WIDTH <= viewportLeft + viewportWidth &&
				ny >= viewportTop && ny + NODE_HEIGHT <= viewportTop + viewportHeight;
			button.visible = inViewport;
		}

		// Visibility of system buttons
		if (this.pageViewMode) {
			this.studyButton.visible = true;
			this.breakthroughButton.visible = true;
			this.backToMapButton.visible = true;
			this.closeButton.visible = false;

			int maxReqPages = maxDetailsPage();
			this.prevDetailsButton.visible = maxReqPages > 0;
			this.nextDetailsButton.visible = maxReqPages > 0;
			this.prevDetailsButton.active = this.detailsPage > 0;
			this.nextDetailsButton.active = this.detailsPage < maxReqPages;
		} else {
			this.studyButton.visible = false;
			this.breakthroughButton.visible = false;
			this.backToMapButton.visible = false;
			this.closeButton.visible = true;
			this.prevDetailsButton.visible = false;
			this.nextDetailsButton.visible = false;
		}
	}

	private ResearchEntry selectedResearch() {
		if (this.researches.isEmpty() || this.selectedIndex < 0 || this.selectedIndex >= this.researches.size()) {
			return null;
		}
		return this.researches.get(this.selectedIndex);
	}

	private int maxDetailsPage() {
		ResearchEntry entry = selectedResearch();
		if (entry == null) {
			return 0;
		}
		return Math.max(0, (requirementLines(entry).size() - 1) / DETAILS_LINES_PER_PAGE);
	}

	private static String categoryKey(String category) {
		return "screen.immortality.altar.category_" + category;
	}

	private static int categoryColor(String category) {
		return switch (category) {
			case "qi" -> 0xFF1B5A8F;
			case "body" -> 0xFF7F5C1B;
			case "breakthrough" -> 0xFF2A6F1D;
			case "beast" -> 0xFF8F361F;
			case "soul" -> 0xFF3F4F8F;
			case "dao" -> 0xFF7A1F8F;
			default -> 0xFF4A3A2A;
		};
	}

	private static int statusColor(ResearchEntry entry) {
		if (entry.known()) return 0xFF1B5E20;
		if (entry.prepared()) return 0xFF0D47A1;
		if (entry.available()) return 0xFF7F5C1B;
		return 0xFFB71C1C;
	}

	private static Component statusComponent(ResearchEntry entry) {
		if (entry.known()) return Component.translatable("screen.immortality.altar.status_known");
		if (entry.prepared()) return Component.translatable("screen.immortality.altar.status_prepared");
		if (entry.available()) return Component.translatable("screen.immortality.altar.status_available");
		return Component.translatable("screen.immortality.altar.status_locked");
	}

	private List<DetailLine> requirementLines(ResearchEntry entry) {
		List<DetailLine> lines = new ArrayList<>();
		if (entry.prerequisites().isEmpty() && entry.requiredItemId().isEmpty() && entry.requiredInsights().isEmpty() && entry.manualMet()) {
			appendWrappedLines(lines, Component.translatable("screen.immortality.altar.requirements_none"), 0xFF1B5E20);
			return lines;
		}
		if (!entry.manualMet()) {
			appendWrappedLines(lines, Component.translatable("screen.immortality.altar.requirements_manual"), 0xFFB71C1C);
		}
		for (String prerequisiteId : entry.prerequisites()) {
			ResearchEntry prerequisite = this.researchById.get(prerequisiteId);
			if (prerequisite != null) {
				int color = prerequisite.known() ? 0xFF1B5E20 : 0xFFB71C1C;
				appendWrappedLines(lines, Component.translatable("screen.immortality.altar.requirements_research", prerequisite.title()), color);
			}
		}
		for (String insightId : entry.requiredInsights()) {
			int color = entry.insightsMet() ? 0xFF1B5E20 : 0xFFB71C1C;
			appendWrappedLines(lines, Component.translatable("screen.immortality.altar.requirements_insight", Component.translatable("insight.immortality." + insightId)), color);
		}
		if (!entry.requiredItemId().isEmpty()) {
			int color = entry.itemMet() ? 0xFF1B5E20 : 0xFFB71C1C;
			appendWrappedLines(lines, Component.translatable("screen.immortality.altar.requirements_item", itemName(entry.requiredItemId())), color);
		}
		return lines;
	}

	private void appendWrappedLines(List<DetailLine> lines, Component text, int color) {
		for (FormattedCharSequence splitLine : this.font.split(text, 100)) {
			lines.add(new DetailLine(splitLine, color));
		}
	}

	private static List<ResearchEntry> readResearches(ListTag list) {
		List<ResearchEntry> values = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {
			CompoundTag compound = list.getCompound(i).orElseGet(CompoundTag::new);
			values.add(new ResearchEntry(
				compound.getString("Id").orElse(""),
				compound.getBoolean("Known").orElse(false),
				compound.getBoolean("Available").orElse(false),
				compound.getBoolean("Prepared").orElse(false),
				compound.getBoolean("StageMet").orElse(false),
				compound.getBoolean("PrerequisitesMet").orElse(false),
				compound.getBoolean("InsightsMet").orElse(false),
				compound.getBoolean("ManualMet").orElse(false),
				compound.getBoolean("QiMet").orElse(false),
				compound.getBoolean("ItemMet").orElse(false),
				compound.getInt("QiCost").orElse(0),
				compound.getInt("Column").orElse(0),
				compound.getInt("Row").orElse(0),
				compound.getString("Category").orElse(""),
				compound.getString("RequiredStage").orElse(""),
				compound.getString("RequiredItemId").orElse(""),
				readStrings(compound.getList("Prerequisites").orElseGet(ListTag::new)),
				readStrings(compound.getList("RequiredInsights").orElseGet(ListTag::new))
			));
		}
		values.sort((a, b) -> Integer.compare(a.column(), b.column()));
		return values;
	}

	private static List<String> readStrings(ListTag list) {
		List<String> values = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {
			String value = list.getString(i).orElse("");
			if (!value.isEmpty()) {
				values.add(value);
			}
		}
		return values;
	}

	private static Component itemName(String itemId) {
		Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId));
		return item == null ? Component.literal(itemId) : item.getName();
	}

	private record DetailLine(FormattedCharSequence text, int color) {
	}

	private record ResearchEntry(
		String id,
		boolean known,
		boolean available,
		boolean prepared,
		boolean stageMet,
		boolean prerequisitesMet,
		boolean insightsMet,
		boolean manualMet,
		boolean qiMet,
		boolean itemMet,
		int qiCost,
		int column,
		int row,
		String category,
		String requiredStage,
		String requiredItemId,
		List<String> prerequisites,
		List<String> requiredInsights
	) {
		Component title() {
			return Component.translatable("research.immortality." + this.id + ".title");
		}

		Component description() {
			return Component.translatable("research.immortality." + this.id + ".description");
		}

		Component shortTitle() {
			return Component.literal(shortName(this.id));
		}

		private static String shortName(String id) {
			return switch (id) {
				case "qi_sense" -> Component.translatable("screen.immortality.altar.node_qi").getString();
				case "meridian_cycle" -> Component.translatable("screen.immortality.altar.node_meridian").getString();
				case "foundation_blueprint" -> Component.translatable("screen.immortality.altar.node_foundation").getString();
				case "core_seal" -> Component.translatable("screen.immortality.altar.node_core").getString();
				case "golden_core_method" -> Component.translatable("screen.immortality.altar.node_golden").getString();
				case "core_flame_refinement" -> Component.translatable("screen.immortality.altar.node_flame").getString();
				case "nascent_soul_seed" -> Component.translatable("screen.immortality.altar.node_seed").getString();
				case "nascent_soul_manifestation" -> Component.translatable("screen.immortality.altar.node_soul").getString();
				case "dao_heart_trial" -> Component.translatable("screen.immortality.altar.node_dao").getString();
				case "spirit_severing_art" -> Component.translatable("screen.immortality.altar.node_sever").getString();
				case "yin_comprehension" -> Component.translatable("screen.immortality.altar.node_yin").getString();
				case "yang_manifestation" -> Component.translatable("screen.immortality.altar.node_yang").getString();
				case "nirvana_karma_thread" -> Component.translatable("screen.immortality.altar.node_karma").getString();
				case "nirvana_cycle_method" -> Component.translatable("screen.immortality.altar.node_nirvana").getString();
				case "void_tribulation_mark" -> Component.translatable("screen.immortality.altar.node_void").getString();
				case "iron_body_method" -> Component.translatable("screen.immortality.altar.node_iron").getString();
				case "spirit_vessel_method" -> Component.translatable("screen.immortality.altar.node_spirit").getString();
				case "water_qi_attunement" -> Component.translatable("screen.immortality.altar.node_water").getString();
				case "wood_qi_attunement" -> Component.translatable("screen.immortality.altar.node_wood").getString();
				case "earth_qi_attunement" -> Component.translatable("screen.immortality.altar.node_earth").getString();
				case "metal_qi_attunement" -> Component.translatable("screen.immortality.altar.node_metal").getString();
				case "fire_qi_attunement" -> Component.translatable("screen.immortality.altar.node_fire").getString();
				case "yin_night_attunement" -> Component.translatable("screen.immortality.altar.node_yin_night").getString();
				case "yang_sun_attunement" -> Component.translatable("screen.immortality.altar.node_yang_sun").getString();
				case "spirit_listening" -> Component.translatable("screen.immortality.altar.node_spirit_listen").getString();
				case "body_domain_forging" -> Component.translatable("screen.immortality.altar.node_body_domain").getString();
				case "fire_trial_rite" -> Component.translatable("screen.immortality.altar.node_trial_flame").getString();
				case "void_scar_theory" -> Component.translatable("screen.immortality.altar.node_void_scar").getString();
				case "karma_mirror_pool" -> Component.translatable("screen.immortality.altar.node_mirror").getString();
				default -> id.length() > 4 ? id.substring(0, 4) : id;
			};
		}
	}

}
