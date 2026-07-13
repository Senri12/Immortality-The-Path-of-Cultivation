package immortality.client;

import immortality.cultivation.CultivationStage;
import immortality.cultivation.QiAspectDefinition;
import immortality.cultivation.QiAspectRegistry;
import immortality.network.ManualActionPayload;
import immortality.network.ResearchActionPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2fStack;
import immortality.Immortality;

public final class ManualScreen extends Screen {
	private static final Identifier BOOK_PAGE = Identifier.fromNamespaceAndPath(Immortality.MOD_ID, "textures/gui/book_page.png");
	private static final Identifier BOOK_TABS = Identifier.fromNamespaceAndPath(Immortality.MOD_ID, "textures/gui/book_tabs.png");

	private static final float UI_TEXT_SCALE = 0.60F;
	private static final int PANEL_WIDTH = 256;
	private static final int PANEL_HEIGHT = 256;

	private static final int LEFT_PAGE_X = 22;
	private static final int RIGHT_PAGE_X = 138;
	private static final int PAGE_WIDTH = 96;

	private static final int ROW_SPACING = 9;
	private static final int SECTION_SPACING = 6;
	private static final int INSIGHTS_PER_PAGE = 12;
	private static final int RESEARCH_LINE_BUDGET = 14;
	private static final int ASPECTS_PER_PAGE = 4;
	private static final int TUTORIAL_TIP_PAGES = 3;

	private final String manualId;
	private final String activeTechniqueId;
	private final CultivationStage stage;
	private final CultivationStage maxStage;
	private final boolean active;
	private final double breakthroughBonus;
	private final double deviationModifier;
	private final List<InsightEntry> insights;
	private final List<TechniqueEntry> techniques;
	private final List<ResearchEntry> researches;
	private final List<AspectHintEntry> aspectHints;

	private ManualSection section = ManualSection.HOME;
	private Button previousPageButton;
	private Button nextPageButton;
	private Button comprehendButton;
	private Button toAltarButton;
	private Button techniquesButton;
	private Button doneButton;

	private int insightPage;
	private int researchPage;
	private int aspectPage;

	public ManualScreen(CompoundTag data) {
		super(Component.translatable("screen.immortality.manual.title"));
		this.manualId = data.getString("ManualId").orElse("none");
		this.activeTechniqueId = data.getString("ActiveTechnique").orElse("none");
		this.stage = CultivationStage.valueOf(data.getString("Stage").orElse(CultivationStage.MORTAL.name()));
		this.maxStage = CultivationStage.valueOf(data.getString("MaxStage").orElse(CultivationStage.MORTAL.name()));
		this.active = data.getBoolean("IsActive").orElse(false);
		this.breakthroughBonus = data.getDouble("BreakthroughBonus").orElse(0.0D);
		this.deviationModifier = data.getDouble("DeviationModifier").orElse(0.0D);
		this.insights = readInsights(data.getList("Insights").orElseGet(ListTag::new));
		this.techniques = readTechniques(data.getList("Techniques").orElseGet(ListTag::new));
		this.researches = readResearches(data.getList("Researches").orElseGet(ListTag::new));
		this.aspectHints = QiAspectRegistry.all().stream().map(AspectHintEntry::fromDefinition).toList();
	}

	@Override
	protected void init() {
		super.init();
		int left = panelLeft();
		int top = panelTop();

		this.previousPageButton = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> {
			switch (this.section) {
				case INSIGHTS -> this.insightPage = Math.max(0, this.insightPage - 1);
				case RESEARCHES -> this.researchPage = Math.max(0, this.researchPage - 1);
				case TIPS -> this.aspectPage = Math.max(0, this.aspectPage - 1);
				default -> {
				}
			}
			refreshButtons();
		}).bounds(left + RIGHT_PAGE_X, top + 172, 16, 16).build());

		this.nextPageButton = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> {
			switch (this.section) {
				case INSIGHTS -> this.insightPage = Math.min(maxInsightPage(), this.insightPage + 1);
				case RESEARCHES -> this.researchPage = Math.min(maxResearchPage(), this.researchPage + 1);
				case TIPS -> this.aspectPage = Math.min(maxAspectPage(), this.aspectPage + 1);
				default -> {
				}
			}
			refreshButtons();
		}).bounds(left + RIGHT_PAGE_X + 80, top + 172, 16, 16).build());

		this.comprehendButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.immortality.manual.comprehend"), button ->
			ClientPlayNetworking.send(new ManualActionPayload(ManualActionPayload.ACTION_COMPREHEND, this.manualId))
		).bounds(left + 22, top + 196, 96, 20).build());

		this.toAltarButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.immortality.manual.to_altar"), button ->
			ClientPlayNetworking.send(new ResearchActionPayload(ResearchActionPayload.ACTION_OPEN_RESEARCH, ""))
		).bounds(left + 132, top + 196, 50, 20).build());

		this.techniquesButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.immortality.manual.techniques_button"), button ->
			ClientPlayNetworking.send(new ResearchActionPayload(ResearchActionPayload.ACTION_OPEN_TECHNIQUES, ""))
		).bounds(left + 184, top + 196, 50, 20).build());

		this.doneButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
			.bounds(left + PANEL_WIDTH / 2 - 25, top + PANEL_HEIGHT + 4, 50, 20)
			.build());

		refreshButtons();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, this.width, this.height, 0x7A08060E);
		int left = panelLeft();
		int top = panelTop();

		// Draw side tabs
		ManualSection[] sections = ManualSection.values();
		for (int i = 0; i < sections.length; i++) {
			int tabY = top + 24 + i * 24;
			int activeOffset = this.section == sections[i] ? 20 : 0;
			graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK_TABS, left - 20, tabY, 0.0F, activeOffset, 24, 16, 32, 128);
			String tabKey = switch (sections[i]) {
				case HOME -> "screen.immortality.manual.tab.home";
				case INSIGHTS -> "screen.immortality.manual.tab.insights";
				case TECHNIQUES -> "screen.immortality.manual.tab.techniques";
				case RESEARCHES -> "screen.immortality.manual.tab.researches";
				case TIPS -> "screen.immortality.manual.tab.tips";
				case HISTORY -> "screen.immortality.manual.tab.history";
			};
			String label = Component.translatable(tabKey).getString();
			int textColor = this.section == sections[i] ? 0xFF3C1F10 : 0xFFDFD5C6;
			drawScaledText(graphics, Component.literal(label), left - 15, tabY + 4, 0.55F, textColor);
		}

		// Draw open book
		graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK_PAGE, left, top, 0.0F, 0.0F, PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);

		// Render tooltips for hovered tabs
		for (int i = 0; i < sections.length; i++) {
			int tabY = top + 24 + i * 24;
			if (mouseX >= left - 20 && mouseX < left && mouseY >= tabY && mouseY < tabY + 16) {
				String titleKey = switch (sections[i]) {
					case HOME -> "screen.immortality.manual.home";
					case INSIGHTS -> "screen.immortality.manual.insights";
					case TECHNIQUES -> "screen.immortality.manual.techniques";
					case RESEARCHES -> "screen.immortality.manual.researches";
					case TIPS -> "screen.immortality.manual.tips";
					case HISTORY -> "screen.immortality.manual.history";
				};
				graphics.setTooltipForNextFrame(Component.translatable(titleKey), mouseX, mouseY);
			}
		}

		// Left page: Main manual info
		int ly = top + 24;
		graphics.drawString(this.font, this.title, left + LEFT_PAGE_X, ly, 0xFF3C1F10, false);
		ly += 12;
		drawScaledText(graphics, title(), left + LEFT_PAGE_X, ly, UI_TEXT_SCALE, 0xFFA89AAE);
		ly += 12;

		ly = drawRow(graphics, left + LEFT_PAGE_X, ly, Component.translatable("screen.immortality.manual.row_active"), yesNo(this.active), this.active ? 0xFF1B5E20 : 0xFFB71C1C);
		ly = drawRow(graphics, left + LEFT_PAGE_X, ly, Component.translatable("screen.immortality.manual.row_stage_limit"), this.maxStage.displayNameComponent(), stageLimitColor());
		ly = drawRow(graphics, left + LEFT_PAGE_X, ly, Component.translatable("screen.immortality.manual.row_breakthrough"), signedPercent(this.breakthroughBonus), this.breakthroughBonus >= 0.0D ? 0xFF1B5E20 : 0xFFB71C1C);
		drawRow(graphics, left + LEFT_PAGE_X, ly, Component.translatable("screen.immortality.manual.row_deviation"), signedPercent(this.deviationModifier), this.deviationModifier <= 0.0D ? 0xFF1B5E20 : 0xFFB71C1C);

		// Right page: Section details
		drawSection(graphics, left, top);

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int left = panelLeft();
		int top = panelTop();
		ManualSection[] sections = ManualSection.values();
		for (int i = 0; i < sections.length; i++) {
			int tabY = top + 24 + i * 24;
			if (event.x() >= left - 20 && event.x() < left && event.y() >= tabY && event.y() < tabY + 16) {
				this.section = sections[i];
				refreshButtons();
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void drawSection(GuiGraphics graphics, int left, int top) {
		switch (this.section) {
			case HOME -> drawHome(graphics, left, top);
			case INSIGHTS -> drawInsights(graphics, left, top);
			case TECHNIQUES -> drawTechniques(graphics, left, top);
			case RESEARCHES -> drawResearches(graphics, left, top);
			case TIPS -> drawTips(graphics, left, top);
			case HISTORY -> drawHistory(graphics, left, top);
		}
	}

	private void drawHome(GuiGraphics graphics, int left, int top) {
		int y = top + 24;
		drawSectionTitle(graphics, left + RIGHT_PAGE_X, y, "screen.immortality.manual.home_summary");
		y += SECTION_SPACING;
		drawWrapped(graphics, Component.translatable("screen.immortality.manual.home_text"), left + RIGHT_PAGE_X, y, PAGE_WIDTH, 0xFF4A3A2A, 12);
	}

	private void drawInsights(GuiGraphics graphics, int left, int top) {
		int y = top + 24;
		drawSectionTitle(graphics, left + RIGHT_PAGE_X, y, "screen.immortality.manual.insights");
		y += SECTION_SPACING + 2;

		if (this.insights.isEmpty()) {
			drawScaledText(graphics, Component.translatable("screen.immortality.manual.insights_none"), left + RIGHT_PAGE_X, y, UI_TEXT_SCALE, 0xFF4A3A2A);
			return;
		}

		int start = this.insightPage * INSIGHTS_PER_PAGE;
		int end = Math.min(start + INSIGHTS_PER_PAGE, this.insights.size());
		for (int i = start; i < end; i++) {
			InsightEntry insight = this.insights.get(i);
			int color = insight.known() ? 0xFF1B5E20 : 0xFF4A3A2A;
			drawScaledText(graphics, Component.literal(insight.known() ? "+ " : "- ").append(Component.translatable("insight.immortality." + insight.id())), left + RIGHT_PAGE_X, y, UI_TEXT_SCALE, color);
			y += ROW_SPACING;
		}
	}

	private void drawTechniques(GuiGraphics graphics, int left, int top) {
		int y = top + 24;
		drawSectionTitle(graphics, left + RIGHT_PAGE_X, y, "screen.immortality.manual.techniques");
		y += SECTION_SPACING + 2;

		if (this.techniques.isEmpty()) {
			drawScaledText(graphics, Component.translatable("screen.immortality.manual.techniques_none"), left + RIGHT_PAGE_X, y, UI_TEXT_SCALE, 0xFF4A3A2A);
			return;
		}

		for (TechniqueEntry technique : this.techniques) {
			boolean activeTech = technique.id().equals(this.activeTechniqueId);
			int color = activeTech ? 0xFF7F5C1B : technique.known() ? 0xFF1B5E20 : 0xFF4A3A2A;
			drawScaledText(graphics, Component.literal(activeTech ? "> " : technique.known() ? "+ " : "- ").append(technique.title()), left + RIGHT_PAGE_X, y, UI_TEXT_SCALE, color);
			y += ROW_SPACING;
		}
	}

	private void drawResearches(GuiGraphics graphics, int left, int top) {
		int y = top + 24;
		drawSectionTitle(graphics, left + RIGHT_PAGE_X, y, "screen.immortality.manual.researches");
		y += SECTION_SPACING + 2;

		if (this.researches.isEmpty()) {
			drawScaledText(graphics, Component.translatable("screen.immortality.manual.researches_none"), left + RIGHT_PAGE_X, y, UI_TEXT_SCALE, 0xFF4A3A2A);
			return;
		}

		List<Integer> starts = researchPageStarts();
		int pageIndex = Math.min(this.researchPage, starts.size() - 1);
		int start = starts.get(pageIndex);
		int end = pageIndex + 1 < starts.size() ? starts.get(pageIndex + 1) : this.researches.size();

		for (int i = start; i < end; i++) {
			ResearchEntry research = this.researches.get(i);
			int color = research.known() ? 0xFF1B5E20 : research.insightReady() ? 0xFF7F5C1B : 0xFF4A3A2A;
			y = drawWrappedFull(graphics, research.title(), left + RIGHT_PAGE_X, y, PAGE_WIDTH, color);
			drawScaledText(
				graphics,
				Component.translatable("screen.immortality.manual.research_line", research.requiredStage().displayNameComponent(), statusText(research)),
				left + RIGHT_PAGE_X,
				y,
				UI_TEXT_SCALE,
				0xFF4A3A2A
			);
			y += ROW_SPACING + 1;
		}
	}

	private void drawTips(GuiGraphics graphics, int left, int top) {
		int y = top + 24;
		if (this.aspectPage < TUTORIAL_TIP_PAGES) {
			drawTutorialTip(graphics, left, y, this.aspectPage);
			return;
		}
		if (this.aspectHints.isEmpty()) {
			drawScaledText(graphics, Component.translatable("screen.immortality.manual.aspect_links_none"), left + RIGHT_PAGE_X, y, UI_TEXT_SCALE, 0xFF4A3A2A);
			return;
		}
		int hintIndex = Math.min(this.aspectPage - TUTORIAL_TIP_PAGES, Math.max(0, this.aspectHints.size() - 1));
		AspectHintEntry hint = this.aspectHints.get(hintIndex);
		drawSectionTitle(graphics, left + RIGHT_PAGE_X, y, "screen.immortality.manual.aspect_links");
		y += SECTION_SPACING;
		drawScaledText(graphics, hint.title(), left + RIGHT_PAGE_X, y, UI_TEXT_SCALE, 0xFF3C1F10);
		y += ROW_SPACING;
		drawWrapped(graphics, Component.translatable("screen.immortality.manual.aspect_links_line", hint.title(), hint.connectionsText()), left + RIGHT_PAGE_X, y, PAGE_WIDTH, 0xFF4A3A2A, 6);
	}

	private void drawTutorialTip(GuiGraphics graphics, int left, int y, int pageIdx) {
		String titleKey = switch (pageIdx) {
			case 0 -> "screen.immortality.manual.tips_intro_title";
			case 1 -> "screen.immortality.manual.tips_types_title";
			default -> "screen.immortality.manual.tips_routes_title";
		};
		String bodyKey = switch (pageIdx) {
			case 0 -> "screen.immortality.manual.tips_intro_text";
			case 1 -> "screen.immortality.manual.tips_types_text";
			default -> "screen.immortality.manual.tips_routes_text";
		};
		drawSectionTitle(graphics, left + RIGHT_PAGE_X, y, titleKey);
		y += SECTION_SPACING;
		drawWrapped(graphics, Component.translatable(bodyKey), left + RIGHT_PAGE_X, y, PAGE_WIDTH, 0xFF4A3A2A, 12);
	}

	private void drawHistory(GuiGraphics graphics, int left, int top) {
		int y = top + 24;
		drawSectionTitle(graphics, left + RIGHT_PAGE_X, y, "screen.immortality.manual.history");
		y += SECTION_SPACING;
		String key = "manual.immortality." + this.manualId + ".history";
		drawWrapped(graphics, Component.translatable(key), left + RIGHT_PAGE_X, y, PAGE_WIDTH, 0xFF4A3A2A, 14);
	}

	private int drawRow(GuiGraphics graphics, int x, int y, Component label, Component value, int color) {
		drawScaledText(graphics, label, x, y, UI_TEXT_SCALE, 0xFF4A3A2A);
		drawScaledText(graphics, value, x + 50, y, UI_TEXT_SCALE, color);
		return y + ROW_SPACING;
	}

	private void drawSectionTitle(GuiGraphics graphics, int x, int y, String key) {
		drawScaledText(graphics, Component.translatable(key), x, y, UI_TEXT_SCALE, 0xFF3C1F10);
	}

	private int drawWrapped(GuiGraphics graphics, Component text, int x, int y, int width, int color, int maxLines) {
		List<FormattedCharSequence> lines = this.font.split(text, width);
		int count = Math.min(lines.size(), maxLines);
		int currentY = y;
		for (int i = 0; i < count; i++) {
			drawScaledText(graphics, lines.get(i), x, currentY, UI_TEXT_SCALE, color);
			currentY += ROW_SPACING - 1;
		}
		return currentY;
	}

	private int drawWrappedFull(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
		List<FormattedCharSequence> lines = this.font.split(text, width);
		int currentY = y;
		for (FormattedCharSequence line : lines) {
			drawScaledText(graphics, line, x, currentY, UI_TEXT_SCALE, color);
			currentY += ROW_SPACING - 1;
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

	private Component title() {
		return Component.translatable("manual.immortality." + this.manualId);
	}

	private int stageLimitColor() {
		return this.stage.tier() <= this.maxStage.tier() ? 0xFF1B5E20 : 0xFFB71C1C;
	}

	private Component yesNo(boolean value) {
		return Component.translatable(value ? "screen.immortality.manual.active_yes" : "screen.immortality.manual.active_no");
	}

	private Component signedPercent(double value) {
		String prefix = value >= 0.0D ? "+" : "";
		return Component.literal(prefix + percent(value));
	}

	private Component statusText(ResearchEntry research) {
		if (research.known()) {
			return Component.translatable("screen.immortality.manual.research_known");
		}
		if (research.insightReady()) {
			return Component.translatable("screen.immortality.manual.research_ready");
		}
		return Component.translatable("screen.immortality.manual.research_blocked");
	}

	private boolean showsPagination() {
		return this.section == ManualSection.INSIGHTS || this.section == ManualSection.RESEARCHES || this.section == ManualSection.TIPS;
	}

	private int currentPage() {
		return switch (this.section) {
			case INSIGHTS -> this.insightPage;
			case RESEARCHES -> this.researchPage;
			case TIPS -> this.aspectPage;
			default -> 0;
		};
	}

	private int maxPageForSection() {
		return switch (this.section) {
			case INSIGHTS -> maxInsightPage();
			case RESEARCHES -> maxResearchPage();
			case TIPS -> maxAspectPage();
			default -> 0;
		};
	}

	private int maxInsightPage() {
		return Math.max(0, (this.insights.size() - 1) / INSIGHTS_PER_PAGE);
	}

	private int maxResearchPage() {
		return Math.max(0, researchPageStarts().size() - 1);
	}

	private int maxAspectPage() {
		return Math.max(0, TUTORIAL_TIP_PAGES + Math.max(0, this.aspectHints.size()) - 1);
	}

	private void refreshButtons() {
		if (this.previousPageButton != null) {
			boolean paged = showsPagination();
			this.previousPageButton.visible = paged;
			this.nextPageButton.visible = paged;
			this.previousPageButton.active = paged && currentPage() > 0;
			this.nextPageButton.active = paged && currentPage() < maxPageForSection();
		}
	}

	private int panelLeft() {
		return (this.width - PANEL_WIDTH) / 2;
	}

	private int panelTop() {
		return (this.height - PANEL_HEIGHT) / 2;
	}

	private static String percent(double value) {
		return Math.round(value * 100.0D) + "%";
	}

	private static List<InsightEntry> readInsights(ListTag list) {
		List<InsightEntry> result = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entry = list.getCompound(i).orElseGet(CompoundTag::new);
			result.add(new InsightEntry(entry.getString("Id").orElse(""), entry.getBoolean("Known").orElse(false)));
		}
		return result;
	}

	private static List<InsightEntry> readInsights(ListTag list, boolean dummy) {
		return readInsights(list);
	}

	private static List<TechniqueEntry> readTechniques(ListTag list) {
		List<TechniqueEntry> result = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entry = list.getCompound(i).orElseGet(CompoundTag::new);
			result.add(new TechniqueEntry(entry.getString("Id").orElse(""), entry.getBoolean("Known").orElse(false)));
		}
		return result;
	}

	private static List<ResearchEntry> readResearches(ListTag list) {
		List<ResearchEntry> result = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entry = list.getCompound(i).orElseGet(CompoundTag::new);
			result.add(new ResearchEntry(
				entry.getString("Id").orElse(""),
				entry.getBoolean("Known").orElse(false),
				entry.getBoolean("InsightReady").orElse(false),
				CultivationStage.valueOf(entry.getString("RequiredStage").orElse(CultivationStage.MORTAL.name()))
			));
		}
		return result;
	}

	private List<Integer> researchPageStarts() {
		List<Integer> starts = new ArrayList<>();
		starts.add(0);
		int usedLines = 0;
		for (int i = 0; i < this.researches.size(); i++) {
			ResearchEntry research = this.researches.get(i);
			int neededLines = Math.max(1, this.font.split(research.title(), PAGE_WIDTH).size()) + 1;
			if (usedLines > 0 && usedLines + neededLines > RESEARCH_LINE_BUDGET) {
				starts.add(i);
				usedLines = 0;
			}
			usedLines += neededLines;
		}
		return starts;
	}

	private enum ManualSection {
		HOME("screen.immortality.manual.home"),
		INSIGHTS("screen.immortality.manual.insights"),
		TECHNIQUES("screen.immortality.manual.techniques"),
		RESEARCHES("screen.immortality.manual.researches"),
		TIPS("screen.immortality.manual.tips"),
		HISTORY("screen.immortality.manual.history");

		private final String titleKey;

		ManualSection(String titleKey) {
			this.titleKey = titleKey;
		}
	}

	private record InsightEntry(String id, boolean known) {
	}

	private record TechniqueEntry(String id, boolean known) {
		Component title() {
			return Component.translatable("technique.immortality." + this.id);
		}
	}

	private record ResearchEntry(String id, boolean known, boolean insightReady, CultivationStage requiredStage) {
		Component title() {
			return Component.translatable("research.immortality." + this.id + ".title");
		}
	}

	private record AspectHintEntry(String id, List<String> connections) {
		static AspectHintEntry fromDefinition(QiAspectDefinition definition) {
			return new AspectHintEntry(definition.id(), definition.connections());
		}

		Component title() {
			return Component.translatable("aspect.immortality." + this.id);
		}

		MutableComponent connectionsText() {
			MutableComponent text = Component.empty();
			boolean first = true;
			for (String connection : this.connections) {
				if (!first) {
					text.append(", ");
				}
				text.append(Component.translatable("aspect.immortality." + connection));
				first = false;
			}
			return text;
		}
	}
}
