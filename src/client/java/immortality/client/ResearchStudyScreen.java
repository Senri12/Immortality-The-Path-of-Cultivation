package immortality.client;

import immortality.cultivation.CultivationStage;
import immortality.cultivation.QiAspectRegistry;
import immortality.cultivation.QiAspectDefinition;
import immortality.network.ResearchLinkPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2fStack;
import immortality.Immortality;

public final class ResearchStudyScreen extends Screen {
	private static final Identifier BOOK_PAGE = Identifier.fromNamespaceAndPath(Immortality.MOD_ID, "textures/gui/book_page.png");
	private static final Identifier ASPECTS_TEXTURE = Identifier.fromNamespaceAndPath(Immortality.MOD_ID, "textures/gui/aspects.png");
	private static final List<String> ASPECTS_LIST = List.of(
		"water", "fire", "wood", "earth", "metal",
		"yin", "yang", "body", "soul", "spirit",
		"void", "mist", "bloom", "ember", "ash",
		"ore", "thunder", "frost", "blood", "echo",
		"dream", "dawn", "abyss", "marrow", "karma"
	);
	private static final int PANEL_WIDTH = 480;
	private static final int PANEL_HEIGHT = 340;
	private static final float TEXT_SCALE = 0.70F;
	private static final int PALETTE_PER_PAGE = 16;
	private static final int LEFT_PAGE_X = 40;
	private static final int RIGHT_PAGE_X = 260;
	private static final int PAGE_WIDTH = 180;

	private final String researchId;
	private final String aspectStart;
	private final String aspectEnd;
	private final int qiCost;
	private final boolean qiMet;
	private final String requiredItemId;
	private final boolean itemMet;
	private final CultivationStage stage;
	private final int currentQi;
	private final int maxQi;
	private final int boardWidth;
	private final int boardHeight;
	private final int boardQiLimit;
	private final String boardVictoryMode;
	private final int yinYangTolerance;
	private final List<Point> starts;
	private final List<Point> finishes;
	private final List<Point> requiredNodes;
	private final Set<Point> blocked;
	private final Map<Point, CellEffect> effects;
	private final List<String> availableAspects;
	private final List<PlacedAspect> placed = new ArrayList<>();

	private Button studyButton;
	private Button resetButton;
	private Button doneButton;
	private Button previousPageButton;
	private Button nextPageButton;
	private int page;
	private String draggedAspectId;
	private Point draggedFrom;
	private int mouseX;
	private int mouseY;

	public ResearchStudyScreen(CompoundTag data) {
		super(Component.translatable("screen.immortality.study.title"));
		this.researchId = data.getString("ResearchId").orElse("");
		this.aspectStart = data.getString("AspectStart").orElse("");
		this.aspectEnd = data.getString("AspectEnd").orElse("");
		this.qiCost = data.getInt("QiCost").orElse(0);
		this.qiMet = data.getBoolean("QiMet").orElse(false);
		this.requiredItemId = data.getString("RequiredItemId").orElse("");
		this.itemMet = data.getBoolean("ItemMet").orElse(false);
		this.stage = CultivationStage.valueOf(data.getString("Stage").orElse(CultivationStage.MORTAL.name()));
		this.currentQi = data.getInt("CurrentQi").orElse(0);
		this.maxQi = data.getInt("MaxQi").orElse(0);
		this.boardWidth = Math.max(1, data.getInt("BoardWidth").orElse(10));
		this.boardHeight = Math.max(1, data.getInt("BoardHeight").orElse(10));
		this.boardQiLimit = Math.max(0, data.getInt("BoardQiLimit").orElse(0));
		this.boardVictoryMode = data.getString("BoardVictoryMode").orElse("single_path");
		this.yinYangTolerance = data.getInt("YinYangTolerance").orElse(99);
		this.starts = readPoints(data.getList("BoardStarts").orElseGet(ListTag::new));
		this.finishes = readPoints(data.getList("BoardFinishes").orElseGet(ListTag::new));
		this.requiredNodes = readPoints(data.getList("BoardRequiredNodes").orElseGet(ListTag::new));
		this.blocked = new HashSet<>(readPoints(data.getList("BoardBlocked").orElseGet(ListTag::new)));
		this.effects = readEffects(data.getList("BoardEffects").orElseGet(ListTag::new));
		this.availableAspects = readStrings(data.getList("AvailableAspects").orElseGet(ListTag::new));
	}

	@Override
	protected void init() {
		super.init();
		int left = panelLeft();
		int top = panelTop();

		this.previousPageButton = this.addRenderableWidget(Button.builder(Component.literal("<"), value -> {
			this.page = Math.max(0, this.page - 1);
			refreshButtons();
		}).bounds(left + 40, top + 250, 20, 20).build());

		this.nextPageButton = this.addRenderableWidget(Button.builder(Component.literal(">"), value -> {
			this.page = Math.min(maxPage(), this.page + 1);
			refreshButtons();
		}).bounds(left + 160, top + 250, 20, 20).build());

		this.resetButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.immortality.study.reset"), value -> {
			this.placed.clear();
			this.draggedAspectId = null;
			this.draggedFrom = null;
			refreshButtons();
		}).bounds(left + 260, top + 250, 55, 20).build());

		this.studyButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.immortality.study.study"), value ->
			ClientPlayNetworking.send(new ResearchLinkPayload(this.researchId, placementsToSend()))
		).bounds(left + 325, top + 250, 55, 20).build());

		this.doneButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), value -> onClose())
			.bounds(left + 385, top + 250, 55, 20)
			.build());

		refreshButtons();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		graphics.fill(0, 0, this.width, this.height, 0x7A08060E);
		int left = panelLeft();
		int top = panelTop();

		// Draw themed book background
		graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK_PAGE, left, top, 0.0F, 0.0F, PANEL_WIDTH, PANEL_HEIGHT, 256, 256, 256, 256);

		if (this.researchId.isBlank()) {
			drawScaledText(graphics, Component.translatable("screen.immortality.study.none"), left + LEFT_PAGE_X, top + 40, TEXT_SCALE, 0xFFB71C1C);
			super.render(graphics, mouseX, mouseY, partialTick);
			return;
		}

		drawInfo(graphics, left, top);
		drawBoard(graphics, left, top);
		drawPalette(graphics, left, top);

		if (this.draggedAspectId != null) {
			CellMetrics metrics = boardMetrics();
			drawHex(graphics, mouseX, mouseY, metrics.radius(), this.draggedAspectId, 0xFFE0C37D, 0xE03F2F24, true);
		}

		super.render(graphics, mouseX, mouseY, partialTick);

		Point hovered = boardCellAt(mouseX, mouseY);
		if (hovered != null) {
			if (this.effects.containsKey(hovered)) {
				CellEffect effect = this.effects.get(hovered);
				List<net.minecraft.util.FormattedCharSequence> tooltipText = new java.util.ArrayList<>();
				Component t = effect.tooltip();
				Component r = effect.ruleText();
				if (t != null && !t.getString().isEmpty()) {
					tooltipText.addAll(this.font.split(t, 180));
				}
				if (r != null && !r.getString().isEmpty()) {
					tooltipText.addAll(this.font.split(r, 180));
				}
				if (!tooltipText.isEmpty()) {
					graphics.setTooltipForNextFrame(this.font, tooltipText, mouseX, mouseY);
				}
			} else if (this.finishes.contains(hovered)) {
				List<net.minecraft.util.FormattedCharSequence> tooltipText = new java.util.ArrayList<>();
				Component t = Component.translatable("screen.immortality.study.finish.tooltip");
				Component r = Component.translatable("screen.immortality.study.finish.rule", Component.translatable("aspect.immortality." + this.aspectEnd));
				tooltipText.addAll(this.font.split(t, 180));
				tooltipText.addAll(this.font.split(r, 180));
				graphics.setTooltipForNextFrame(this.font, tooltipText, mouseX, mouseY);
			}
		}

		String hoveredAspect = hoveredPaletteAspect();
		boolean hasCellTooltip = false;
		if (hoveredAspect == null && hovered != null) {
			PlacedAspect p = placedAt(hovered);
			if (p != null) {
				hoveredAspect = p.aspectId();
				hasCellTooltip = this.effects.containsKey(hovered) || this.finishes.contains(hovered);
			}
		}
		if (hoveredAspect != null) {
			drawAspectConnectionsTooltip(graphics, hoveredAspect, mouseX, mouseY, hasCellTooltip);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (handleBoardClick(event.x(), event.y(), event.button())) {
			return true;
		}
		if (handlePaletteClick(event.x(), event.y(), event.button())) {
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (this.draggedAspectId == null || event.button() != 0) {
			return super.mouseReleased(event);
		}
		Point target = boardCellAt(event.x(), event.y());
		if (target != null && canPlaceAt(target)) {
			removePlaced(target);
			this.placed.add(new PlacedAspect(target, this.draggedAspectId));
		} else if (this.draggedFrom != null) {
			this.placed.add(new PlacedAspect(this.draggedFrom, this.draggedAspectId));
		}
		this.draggedAspectId = null;
		this.draggedFrom = null;
		refreshButtons();
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void drawInfo(GuiGraphics graphics, int left, int top) {
		int y = top + 24;
		drawScaledText(graphics, Component.translatable("research.immortality." + this.researchId + ".title"), left + LEFT_PAGE_X, y, TEXT_SCALE, 0xFF3C1F10);
		y += 10;
		y = drawWrapped(graphics, Component.translatable("research.immortality." + this.researchId + ".description"), left + LEFT_PAGE_X, y, PAGE_WIDTH, 4, 0xFF4A3A2A);
		y += 2;
		y = drawRow(graphics, left + LEFT_PAGE_X, y, Component.translatable("screen.immortality.study.row_stage"), this.stage.displayNameComponent(), 0xFF3F4F8F);
		y = drawRow(graphics, left + LEFT_PAGE_X, y, Component.translatable("screen.immortality.study.row_qi"), Component.literal(this.currentQi + "/" + this.maxQi + " | " + this.qiCost), this.qiMet ? 0xFF1B5E20 : 0xFFB71C1C);
		y = drawRow(graphics, left + LEFT_PAGE_X, y, Component.translatable("screen.immortality.study.row_board_qi"), Component.literal(boardQiUsage() + "/" + this.boardQiLimit), remainingBoardQi() >= 0 ? 0xFF1B5A8F : 0xFFB71C1C);
		y = drawRow(graphics, left + LEFT_PAGE_X, y, Component.translatable("screen.immortality.study.row_mode"), Component.translatable("screen.immortality.study.mode." + this.boardVictoryMode), 0xFF5C3F8F);
		if (this.yinYangTolerance < 90) {
			int[] yinYang = calculateYinYang();
			int diff = Math.abs(yinYang[0] - yinYang[1]);
			boolean balanced = diff <= this.yinYangTolerance;
			int color = balanced ? 0xFF1B5E20 : 0xFFB71C1C;
			drawRow(graphics, left + LEFT_PAGE_X, y, Component.translatable("screen.immortality.study.row_polarity"), Component.literal(yinYang[0] + " И / " + yinYang[1] + " Я (доп. " + this.yinYangTolerance + ")"), color);
		}
	}

	private void drawBoard(GuiGraphics graphics, int left, int top) {
		CellMetrics metrics = boardMetrics();
		int yStart = top + 24;
		drawScaledText(graphics, Component.translatable("screen.immortality.study.row_aspects"), left + RIGHT_PAGE_X, yStart, TEXT_SCALE, 0xFF3C1F10);
		yStart += 10;

		Set<Point> connectedPoints = calculateConnectedPoints();

		for (int y = 0; y < this.boardHeight; y++) {
			for (int x = 0; x < this.boardWidth; x++) {
				Point point = new Point(x, y);
				int centerX = metrics.cellCenterX(point);
				int centerY = metrics.cellCenterY(point);
				if (this.blocked.contains(point)) {
					drawHex(graphics, centerX, centerY, metrics.radius(), "", 0xFF2B2630, 0xE01A161E, true);
					continue;
				}
				if (this.starts.contains(point)) {
					drawHex(graphics, centerX, centerY, metrics.radius(), this.aspectStart, 0xFF7AC3FF, 0xE0253340, true);
					continue;
				}
				if (this.finishes.contains(point)) {
					boolean conn = connectedPoints.contains(point);
					int border = conn ? 0xFFA5D5FF : 0xFF444444;
					int fill = conn ? 0xE0222E3A : 0xB0111111;
					drawHex(graphics, centerX, centerY, metrics.radius(), this.aspectEnd, border, fill, conn);
					continue;
				}
				PlacedAspect placedAspect = placedAt(point);
				boolean hovered = point.equals(boardCellAt(this.mouseX, this.mouseY));
				CellEffect effect = this.effects.get(point);
				boolean conn = placedAspect == null || connectedPoints.contains(point);
				int border = hovered ? 0xFFE0C37D : this.requiredNodes.contains(point) ? (conn ? 0xFFE4A7FF : 0xFF553355) : effect == null ? (conn ? 0xFF6B586E : 0xFF444444) : effect.borderColor();
				int fill = effect == null ? (placedAspect != null ? (conn ? 0xE03A2B22 : 0xE0222222) : 0xB0221B22) : effect.fillColor();
				if (placedAspect != null) {
					fill = effect != null ? effect.placedFillColor(placedAspect.aspectId()) : (conn ? 0xE03A2B22 : 0xE0222222);
				}
				if (!conn && effect != null) {
					border = 0xFF444444;
					fill = 0xB0111111;
				}
				String label = placedAspect == null
					? this.requiredNodes.contains(point) ? "R" : effect != null && effect.transformTo() != null && !effect.transformTo().isBlank() ? "*" : ""
					: (effect != null ? effect.transform(placedAspect.aspectId()) : placedAspect.aspectId());
				drawHex(graphics, centerX, centerY, metrics.radius(), label, border, fill, conn);
			}
		}

		int legendY = top + 228;
		drawHex(graphics, left + 265, legendY, 6, "", 0xFF7CCECF, 0xE0223A3E, true);
		drawScaledText(graphics, Component.translatable("screen.immortality.study.legend.transform"), left + 273, legendY - 3, 0.6F, 0xFF4A3A2A);

		drawHex(graphics, left + 355, legendY, 6, "", 0xFFE4A7FF, 0xE03F2240, true);
		drawScaledText(graphics, Component.translatable("screen.immortality.study.legend.required"), left + 363, legendY - 3, 0.6F, 0xFF4A3A2A);
	}

	private void drawPalette(GuiGraphics graphics, int left, int top) {
		int startY = top + 105;
		drawScaledText(graphics, Component.translatable("screen.immortality.study.available"), left + LEFT_PAGE_X, startY, TEXT_SCALE, 0xFF3C1F10);

		for (int i = 0; i < PALETTE_PER_PAGE; i++) {
			String aspectId = paletteAspectAt(i);
			if (aspectId == null) {
				continue;
			}
			int x = left + 45 + (i % 4) * 35;
			int y = top + 122 + (i / 4) * 26;
			drawHex(graphics, x, y, 11, aspectId, hoveredPalette(i) ? 0xFFE0C37D : 0xFFD0B57F, 0xD02A2430, true);
		}

		String hoveredAspect = hoveredPaletteAspect();
		if (hoveredAspect != null) {
			drawScaledText(graphics, Component.translatable("aspect.immortality." + hoveredAspect), left + LEFT_PAGE_X, top + 230, 0.70F, 0xFF7F5C1B);
		}
		
		Component pageText = Component.translatable("screen.immortality.common.page", this.page + 1, maxPage() + 1);
		int textX = left + 97 - Math.round((this.font.width(pageText) * 0.70F) / 2.0F);
		drawScaledText(graphics, pageText, textX, top + 252, 0.70F, 0xFF4A3A2A);
	}

	private boolean handleBoardClick(double mouseX, double mouseY, int button) {
		Point point = boardCellAt(mouseX, mouseY);
		if (point == null) {
			return false;
		}
		PlacedAspect placedAspect = placedAt(point);
		if (button == 0) {
			if (placedAspect != null) {
				this.draggedAspectId = placedAspect.aspectId();
				this.draggedFrom = point;
				removePlaced(point);
				refreshButtons();
				return true;
			}
		} else if (button == 1) {
			if (placedAspect != null) {
				removePlaced(point);
				refreshButtons();
				return true;
			}
		}
		return false;
	}

	private boolean handlePaletteClick(double mouseX, double mouseY, int button) {
		if (button != 0 || remainingBoardQi() <= 0) {
			return false;
		}
		for (int i = 0; i < PALETTE_PER_PAGE; i++) {
			String aspectId = paletteAspectAt(i);
			if (aspectId == null) {
				continue;
			}
			int x = panelLeft() + 45 + (i % 4) * 35;
			int y = panelTop() + 122 + (i / 4) * 26;
			if (insideHex(mouseX, mouseY, x, y, 11)) {
				this.draggedAspectId = aspectId;
				this.draggedFrom = null;
				return true;
			}
		}
		return false;
	}

	private void refreshButtons() {
		this.previousPageButton.active = this.page > 0;
		this.nextPageButton.active = this.page < maxPage();
		this.studyButton.active = !this.researchId.isBlank() && this.qiMet && this.itemMet;
	}

	private int maxPage() {
		return Math.max(0, (this.availableAspects.size() - 1) / PALETTE_PER_PAGE);
	}

	private int remainingBoardQi() {
		return this.boardQiLimit <= 0 ? 999 : this.boardQiLimit - boardQiUsage();
	}

	private int boardQiUsage() {
		int usage = 0;
		for (PlacedAspect placedAspect : this.placed) {
			CellEffect effect = this.effects.get(placedAspect.point());
			usage += effect != null ? effect.qiCost(placedAspect.aspectId()) : 1;
		}
		return usage;
	}

	private boolean canPlaceAt(Point point) {
		return !this.blocked.contains(point)
			&& !this.starts.contains(point)
			&& !this.finishes.contains(point)
			&& hasBudgetFor(point, this.draggedAspectId);
	}

	private boolean hasBudgetFor(Point point, String aspectId) {
		if (aspectId == null || this.boardQiLimit <= 0) {
			return true;
		}
		int current = boardQiUsage();
		if (this.draggedFrom != null) {
			PlacedAspect existing = new PlacedAspect(this.draggedFrom, aspectId);
			current -= costOf(existing);
		}
		PlacedAspect replaced = placedAt(point);
		if (replaced != null) {
			current -= costOf(replaced);
		}
		current += costOf(new PlacedAspect(point, aspectId));
		return current <= this.boardQiLimit;
	}

	private int costOf(PlacedAspect placedAspect) {
		CellEffect effect = this.effects.get(placedAspect.point());
		return effect != null ? effect.qiCost(placedAspect.aspectId()) : 1;
	}

	private List<ResearchLinkPayload.Placement> placementsToSend() {
		return this.placed.stream()
			.map(placedAspect -> new ResearchLinkPayload.Placement(placedAspect.point().x(), placedAspect.point().y(), placedAspect.aspectId()))
			.toList();
	}

	private PlacedAspect placedAt(Point point) {
		for (PlacedAspect placedAspect : this.placed) {
			if (placedAspect.point().equals(point)) {
				return placedAspect;
			}
		}
		return null;
	}

	private void removePlaced(Point point) {
		this.placed.removeIf(placedAspect -> placedAspect.point().equals(point));
	}

	private String paletteAspectAt(int indexOnPage) {
		int index = this.page * PALETTE_PER_PAGE + indexOnPage;
		return index >= 0 && index < this.availableAspects.size() ? this.availableAspects.get(index) : null;
	}

	private boolean hoveredPalette(int indexOnPage) {
		String aspectId = paletteAspectAt(indexOnPage);
		if (aspectId == null) {
			return false;
		}
		int x = panelLeft() + 45 + (indexOnPage % 4) * 35;
		int y = panelTop() + 122 + (indexOnPage / 4) * 26;
		return insideHex(this.mouseX, this.mouseY, x, y, 11);
	}

	private String hoveredPaletteAspect() {
		for (int i = 0; i < PALETTE_PER_PAGE; i++) {
			if (hoveredPalette(i)) {
				return paletteAspectAt(i);
			}
		}
		return null;
	}

	private Point boardCellAt(double mouseX, double mouseY) {
		CellMetrics metrics = boardMetrics();
		for (int y = 0; y < this.boardHeight; y++) {
			for (int x = 0; x < this.boardWidth; x++) {
				Point point = new Point(x, y);
				if (insideHex(mouseX, mouseY, metrics.cellCenterX(point), metrics.cellCenterY(point), metrics.radius())) {
					return point;
				}
			}
		}
		return null;
	}

	private boolean insideHex(double mouseX, double mouseY, int centerX, int centerY, int radius) {
		double dx = Math.abs(mouseX - centerX);
		double dy = Math.abs(mouseY - centerY);
		if (dx > radius || dy > radius) {
			return false;
		}
		return dy <= radius * 0.58D || dx + dy * 0.62D <= radius * 1.25D;
	}

	private CellMetrics boardMetrics() {
		int boardLeft = panelLeft() + 260;
		int boardTop = panelTop() + 40;
		int boardWidthPx = 180;
		int boardHeightPx = 180;
		int radiusByWidth = Math.max(3, boardWidthPx / Math.max(3, this.boardWidth * 2 + 1));
		int radiusByHeight = Math.max(3, boardHeightPx / Math.max(3, this.boardHeight * 2));
		int radius = Math.max(2, Math.min(13, Math.min(radiusByWidth, radiusByHeight)));
		return new CellMetrics(boardLeft, boardTop, radius);
	}

	private void drawHex(GuiGraphics graphics, int centerX, int centerY, int radius, String aspectId, int borderColor, int fillColor, boolean connected) {
		fillHex(graphics, centerX, centerY, radius, borderColor);
		fillHex(graphics, centerX, centerY, Math.max(2, radius - 1), fillColor);
		if (!aspectId.isBlank()) {
			int aspectIndex = ASPECTS_LIST.indexOf(aspectId);
			if (aspectIndex >= 0) {
				int col = aspectIndex % 5;
				int row = aspectIndex / 5;
				float u = col * 51.2F;
				float v = row * 51.2F;
				int size = Math.round(radius * 1.3F);
				int iconX = centerX - size / 2;
				int iconY = centerY - size / 2;
				
				Matrix3x2fStack pose = graphics.pose();
				pose.pushMatrix();
				pose.translate(iconX, iconY);
				float scale = size / 51.0F;
				pose.scale(scale, scale);
				int colorTint = connected ? 0xFFFFFFFF : 0x66909090;
				graphics.blit(RenderPipelines.GUI_TEXTURED, ASPECTS_TEXTURE, 0, 0, u, v, 51, 51, 256, 256, colorTint);
				pose.popMatrix();
			} else {
				String text = aspectId.equals("F") ? "F" : (aspectId.length() > 2 ? aspectId.substring(0, 2) : aspectId);
				int color = connected ? 0xFFF5E9D2 : 0xFF777777;
				int textX = centerX - this.font.width(text) / 2;
				graphics.drawString(this.font, text, textX, centerY - 4, color, false);
			}
		}
	}

	private void drawAspectConnectionsTooltip(GuiGraphics graphics, String aspectId, int mouseX, int mouseY, boolean offsetDown) {
		QiAspectDefinition def = QiAspectRegistry.get(aspectId);
		if (def == null) {
			return;
		}

		List<String> connects = def.connections();
		int n = connects.size();

		Component polarityComp = Component.translatable("screen.immortality.study.polarity." + (isYinFamily(aspectId) ? "yin" : isYangFamily(aspectId) ? "yang" : "neutral"));
		Component header = Component.translatable("aspect.immortality." + aspectId).copy().append(polarityComp);
		int headerWidth = Math.round(this.font.width(header) * 0.70F);

		Component label = Component.translatable("screen.immortality.study.connections");
		int labelWidth = Math.round(this.font.width(label) * 0.60F);

		int boxHeight = 36;
		int spacing = 18;
		int boxWidth = Math.max(headerWidth + 12, labelWidth + 12 + n * spacing + 6);
		boxWidth = Math.max(100, boxWidth);

		int boxX = mouseX + 12;
		int boxY = offsetDown ? mouseY + 26 : mouseY - 12;
		if (boxX + boxWidth > this.width) {
			boxX = mouseX - boxWidth - 12;
		}
		if (boxY + boxHeight > this.height) {
			boxY = this.height - boxHeight - 4;
		}

		graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xE51A161E);
		graphics.renderOutline(boxX, boxY, boxWidth, boxHeight, 0xFFD0B57F);

		drawScaledText(graphics, header, boxX + 6, boxY + 5, 0.70F, 0xFFFFFFFF);
		drawScaledText(graphics, label, boxX + 6, boxY + 20, 0.60F, 0xFFD0B57F);

		int startX = boxX + labelWidth + 12 + 8;
		for (int i = 0; i < n; i++) {
			int cx = startX + i * spacing;
			int cy = boxY + 22;
			drawHex(graphics, cx, cy, 8, connects.get(i), 0xFFD0B57F, 0xD02A2430, true);
		}
	}

	private void fillHex(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
		int h = Math.max(1, (int) Math.round(radius * 0.75D));
		for (int dy = -h; dy <= h; dy++) {
			double pct = (double) Math.abs(dy) / h;
			int w = radius - (int) Math.round(pct * radius * 0.4D);
			graphics.fill(centerX - w, centerY + dy, centerX + w + 1, centerY + dy + 1, color);
		}
	}

	private String aspectShort(String aspectId) {
		String name = Component.translatable("aspect.immortality." + aspectId).getString();
		return name.length() > 2 ? name.substring(0, 2) : name;
	}

	private String itemKey() {
		return "item.immortality." + this.requiredItemId.replace("immortality:", "");
	}

	private int panelLeft() {
		return (this.width - PANEL_WIDTH) / 2;
	}

	private int panelTop() {
		return (this.height - PANEL_HEIGHT) / 2;
	}

	private int drawRow(GuiGraphics graphics, int x, int y, Component label, Component value, int color) {
		drawScaledText(graphics, label, x, y, TEXT_SCALE, 0xFF4A3A2A);
		drawScaledText(graphics, value, x + 85, y, TEXT_SCALE, color);
		return y + 11;
	}

	private int drawWrapped(GuiGraphics graphics, Component text, int x, int y, int width, int maxLines, int color) {
		List<FormattedCharSequence> lines = this.font.split(text, width);
		int count = Math.min(lines.size(), maxLines);
		int currentY = y;
		for (int i = 0; i < count; i++) {
			drawScaledText(graphics, lines.get(i), x, currentY, TEXT_SCALE, color);
			currentY += 10;
		}
		return currentY;
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

	private static List<Point> readPoints(ListTag list) {
		List<Point> result = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {
			CompoundTag compound = list.getCompound(i).orElseGet(CompoundTag::new);
			result.add(new Point(compound.getInt("X").orElse(0), compound.getInt("Y").orElse(0)));
		}
		return result;
	}

	private static List<String> readStrings(ListTag list) {
		List<String> result = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {
			String val = list.getString(i).orElse("");
			if (!val.isEmpty()) {
				result.add(val);
			}
		}
		return result;
	}

	private static Map<Point, CellEffect> readEffects(ListTag list) {
		Map<Point, CellEffect> result = new HashMap<>();
		for (int i = 0; i < list.size(); i++) {
			CompoundTag compound = list.getCompound(i).orElseGet(CompoundTag::new);
			Point point = new Point(compound.getInt("X").orElse(0), compound.getInt("Y").orElse(0));
			
			List<String> favoredAspects = new java.util.ArrayList<>();
			ListTag favored = compound.getList("FavoredAspects").orElseGet(ListTag::new);
			for (int j = 0; j < favored.size(); j++) {
				favoredAspects.add(favored.getString(j).orElse(""));
			}
			
			List<String> hostileAspects = new java.util.ArrayList<>();
			ListTag hostile = compound.getList("HostileAspects").orElseGet(ListTag::new);
			for (int j = 0; j < hostile.size(); j++) {
				hostileAspects.add(hostile.getString(j).orElse(""));
			}

			result.put(point, new CellEffect(
				favoredAspects,
				hostileAspects,
				compound.getInt("FavoredQiDelta").orElse(0),
				compound.getInt("HostileQiDelta").orElse(0),
				compound.getInt("NeutralQiDelta").orElse(0),
				compound.getString("TransformTo").orElse(""),
				compound.getString("RequiredAdjacentAspect").orElse(""),
				compound.getString("RequiredPolarity").orElse("none")
			));
		}
		return result;
	}

	private record Point(int x, int y) {
	}

	private record PlacedAspect(Point point, String aspectId) {
	}

	private record CellEffect(
		List<String> favoredAspects,
		List<String> hostileAspects,
		int favoredQiDelta,
		int hostileQiDelta,
		int neutralQiDelta,
		String transformTo,
		String requiredAdjacentAspect,
		String requiredPolarity
	) {
		int borderColor() {
			if (!this.requiredAdjacentAspect.isEmpty()) return 0xFFE4A7FF;
			if (this.favoredQiDelta < 0) return 0xFF7CCECF;
			return 0xFF6B586E;
		}

		int fillColor() {
			if (!this.requiredAdjacentAspect.isEmpty()) return 0xE03F2240;
			if (this.favoredQiDelta < 0) return 0xE0223A3E;
			return 0xB0221B22;
		}

		int placedFillColor(String aspectId) {
			return fillColor();
		}

		int qiCost(String aspectId) {
			if (this.favoredAspects.contains(aspectId)) {
				return Math.max(0, 1 + this.favoredQiDelta);
			}
			if (this.hostileAspects.contains(aspectId)) {
				return Math.max(0, 1 + this.hostileQiDelta);
			}
			return Math.max(0, 1 + this.neutralQiDelta);
		}

		String transform(String aspectId) {
			return this.transformTo != null && !this.transformTo.isEmpty() ? this.transformTo : aspectId;
		}

		Component tooltip() {
			if (this.favoredQiDelta < 0) {
				return Component.translatable("cell_effect.immortality.qi_source.tooltip");
			}
			if (!this.requiredAdjacentAspect.isEmpty()) {
				return Component.translatable("cell_effect.immortality.required_adjacent.tooltip");
			}
			if (this.transformTo != null && !this.transformTo.isEmpty()) {
				return Component.translatable("cell_effect.immortality.transform.tooltip");
			}
			return Component.empty();
		}

		Component ruleText() {
			if (this.favoredQiDelta < 0) {
				return Component.translatable("cell_effect.immortality.qi_source.rule");
			}
			if (!this.requiredAdjacentAspect.isEmpty()) {
				return Component.translatable("cell_effect.immortality.required_adjacent.rule", this.requiredAdjacentAspect);
			}
			if (this.transformTo != null && !this.transformTo.isEmpty()) {
				return Component.translatable("cell_effect.immortality.transform.rule", Component.translatable("aspect.immortality." + this.transformTo));
			}
			return Component.empty();
		}
	}

	private Set<Point> calculateConnectedPoints() {
		Set<Point> connected = new HashSet<>();
		if (this.starts.isEmpty()) {
			return connected;
		}

		Map<Point, String> placedMap = new HashMap<>();
		for (PlacedAspect pa : this.placed) {
			placedMap.put(pa.point(), pa.aspectId());
		}

		java.util.Queue<Point> queue = new java.util.LinkedList<>();
		for (Point start : this.starts) {
			connected.add(start);
			queue.add(start);
		}

		while (!queue.isEmpty()) {
			Point current = queue.poll();
			String currentAspect = this.starts.contains(current) 
				? this.aspectStart 
				: (this.effects.containsKey(current) 
					? this.effects.get(current).transform(placedMap.get(current)) 
					: placedMap.get(current));

			if (currentAspect == null || currentAspect.isEmpty()) {
				continue;
			}

			for (Point neighbor : getNeighbors(current)) {
				if (connected.contains(neighbor)) {
					continue;
				}

				String neighborAspect = null;
				if (this.finishes.contains(neighbor)) {
					neighborAspect = this.aspectEnd;
				} else if (placedMap.containsKey(neighbor)) {
					String placedId = placedMap.get(neighbor);
					neighborAspect = this.effects.containsKey(neighbor) 
						? this.effects.get(neighbor).transform(placedId) 
						: placedId;
				}

				if (neighborAspect == null || neighborAspect.isEmpty()) {
					continue;
				}

				if (aspectsConnect(currentAspect, neighborAspect)) {
					connected.add(neighbor);
					queue.add(neighbor);
				}
			}
		}
		return connected;
	}

	private boolean aspectsConnect(String a, String b) {
		String realA = "F".equals(a) ? this.aspectEnd : a;
		String realB = "F".equals(b) ? this.aspectEnd : b;
		var aspectA = QiAspectRegistry.get(realA);
		var aspectB = QiAspectRegistry.get(realB);
		return (aspectA != null && aspectA.connectsTo(realB)) || (aspectB != null && aspectB.connectsTo(realA));
	}

	private List<Point> getNeighbors(Point point) {
		int x = point.x();
		int y = point.y();
		if ((y & 1) == 0) {
			return List.of(
				new Point(x - 1, y),
				new Point(x + 1, y),
				new Point(x, y - 1),
				new Point(x, y + 1),
				new Point(x - 1, y - 1),
				new Point(x - 1, y + 1)
			);
		}
		return List.of(
			new Point(x - 1, y),
			new Point(x + 1, y),
			new Point(x, y - 1),
			new Point(x, y + 1),
			new Point(x + 1, y - 1),
			new Point(x + 1, y + 1)
		);
	}

	private int[] calculateYinYang() {
		Set<Point> connected = calculateConnectedPoints();
		int yin = isYinFamily(this.aspectStart) ? 1 : 0;
		int yang = isYangFamily(this.aspectStart) ? 1 : 0;
		for (Point point : connected) {
			if (this.starts.contains(point) || this.finishes.contains(point)) {
				continue;
			}
			PlacedAspect placedAspect = placedAt(point);
			if (placedAspect != null) {
				CellEffect effect = this.effects.get(point);
				String effective = effect != null ? effect.transform(placedAspect.aspectId()) : placedAspect.aspectId();
				if (isYinFamily(effective)) {
					yin++;
				}
				if (isYangFamily(effective)) {
					yang++;
				}
			}
		}
		boolean finishConnected = false;
		for (Point f : this.finishes) {
			if (connected.contains(f)) {
				finishConnected = true;
				break;
			}
		}
		if (finishConnected) {
			if (isYinFamily(this.aspectEnd)) {
				yin++;
			}
			if (isYangFamily(this.aspectEnd)) {
				yang++;
			}
		}
		return new int[]{yin, yang};
	}

	private boolean isYinFamily(String aspectId) {
		return Set.of("yin", "water", "mist", "frost", "dream", "abyss", "void", "soul").contains(aspectId);
	}

	private boolean isYangFamily(String aspectId) {
		return Set.of("yang", "fire", "ember", "blood", "dawn", "thunder", "metal", "karma").contains(aspectId);
	}

	private static final class CellMetrics {
		private final int boardLeft;
		private final int boardTop;
		private final int radius;
		private final double hexWidth;
		private final double hexHeight;

		CellMetrics(int boardLeft, int boardTop, int radius) {
			this.boardLeft = boardLeft;
			this.boardTop = boardTop;
			this.radius = radius;
			this.hexWidth = radius * 2.0D;
			this.hexHeight = radius * 1.732D;
		}

		int cellCenterX(Point p) {
			double offset = (p.y() % 2) * (this.hexWidth * 0.5D);
			return (int) Math.round(this.boardLeft + p.x() * this.hexWidth + offset + this.radius);
		}

		int cellCenterY(Point p) {
			return (int) Math.round(this.boardTop + p.y() * (this.hexHeight * 0.75D) + (this.hexHeight * 0.5D));
		}

		int radius() {
			return this.radius;
		}
	}
}
