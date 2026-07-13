package immortality.client;

import immortality.cultivation.BodyDefinition;
import immortality.cultivation.BodyRegistry;
import immortality.cultivation.CultivationStage;
import immortality.network.ResearchActionPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2fStack;
import immortality.Immortality;

public final class BreakthroughScreen extends Screen {
	private static final Identifier BOOK_PAGE = Identifier.fromNamespaceAndPath(Immortality.MOD_ID, "textures/gui/book_page.png");
	private static final float UI_TEXT_SCALE = 0.60F;
	private static final int PANEL_WIDTH = 256;
	private static final int PANEL_HEIGHT = 256;

	private static final int LEFT_PAGE_X = 22;
	private static final int RIGHT_PAGE_X = 138;
	private static final int PAGE_WIDTH = 96;
	private static final int RIGHT_LINES_PER_PAGE = 12;
	private static final int ROW_SPACING = 9;
	private static final int SECTION_SPACING = 6;

	private final CultivationStage stage;
	private final CultivationStage nextStage;
	private final int currentQi;
	private final int requiredQi;
	private final double purity;
	private final double stability;
	private final double deviation;
	private final BodyDefinition body;
	private final boolean peakReached;
	private final boolean cooldownActive;
	private final boolean researchMet;
	private final boolean qiMet;
	private final boolean coreRequired;
	private final boolean coreMet;
	private final double chance;
	private final double baseChance;
	private final double purityBonus;
	private final double stabilityBonus;
	private final double deviationPenalty;
	private final double bodyBonus;
	private final double manualBonus;
	private final double techniqueBonus;
	private final double coreBonus;
	private final String requiredResearchId;
	private final String coreName;
	private final String coreCompatibility;
	private final double coreStabilityBonus;
	private final double coreDeviationPenalty;
	private final double corePurityBonus;
	private final int cooldown;

	private Button attemptButton;
	private Button backButton;
	private Button previousPageButton;
	private Button nextPageButton;
	private int page;

	public BreakthroughScreen(CompoundTag data) {
		super(Component.translatable("screen.immortality.breakthrough.title"));
		this.stage = CultivationStage.valueOf(data.getString("Stage").orElse(CultivationStage.MORTAL.name()));
		this.nextStage = CultivationStage.valueOf(data.getString("NextStage").orElse(this.stage.name()));
		this.currentQi = data.getInt("CurrentQi").orElse(0);
		this.requiredQi = data.getInt("RequiredQi").orElse(0);
		this.purity = data.getDouble("Purity").orElse(0.0D);
		this.stability = data.getDouble("Stability").orElse(0.0D);
		this.deviation = data.getDouble("Deviation").orElse(0.0D);
		this.body = BodyRegistry.get(data.getString("Body").orElse(BodyRegistry.NONE_ID));
		this.peakReached = data.getBoolean("PeakReached").orElse(false);
		this.cooldownActive = data.getBoolean("CooldownActive").orElse(false);
		this.researchMet = data.getBoolean("ResearchMet").orElse(false);
		this.qiMet = data.getBoolean("QiMet").orElse(false);
		this.coreRequired = data.getBoolean("CoreRequired").orElse(false);
		this.coreMet = data.getBoolean("CoreMet").orElse(false);
		this.chance = data.getDouble("Chance").orElse(0.0D);
		this.baseChance = data.getDouble("BaseChance").orElse(0.0D);
		this.purityBonus = data.getDouble("PurityBonus").orElse(0.0D);
		this.stabilityBonus = data.getDouble("StabilityBonus").orElse(0.0D);
		this.deviationPenalty = data.getDouble("DeviationPenalty").orElse(0.0D);
		this.bodyBonus = data.getDouble("BodyBonus").orElse(0.0D);
		this.manualBonus = data.getDouble("ManualBonus").orElse(0.0D);
		this.techniqueBonus = data.getDouble("TechniqueBonus").orElse(0.0D);
		this.coreBonus = data.getDouble("CoreBonus").orElse(0.0D);
		this.requiredResearchId = data.getString("RequiredResearchId").orElse("");
		this.coreName = data.getString("CoreName").orElse("");
		this.coreCompatibility = data.getString("CoreBodyCompatibility").orElse("");
		this.coreStabilityBonus = data.getDouble("CoreStabilityBonus").orElse(0.0D);
		this.coreDeviationPenalty = data.getDouble("CoreDeviationPenalty").orElse(0.0D);
		this.corePurityBonus = data.getDouble("CorePurityBonus").orElse(0.0D);
		this.cooldown = data.getInt("Cooldown").orElse(0);
	}

	@Override
	protected void init() {
		super.init();
		int panelLeft = (this.width - PANEL_WIDTH) / 2;
		int panelTop = (this.height - PANEL_HEIGHT) / 2;
		this.page = Math.min(this.page, maxRightPage());

		this.previousPageButton = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> {
			this.page = Math.max(0, this.page - 1);
			refreshButtons();
		}).bounds(panelLeft + RIGHT_PAGE_X, panelTop + 172, 16, 16).build());

		this.nextPageButton = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> {
			this.page = Math.min(maxRightPage(), this.page + 1);
			refreshButtons();
		}).bounds(panelLeft + RIGHT_PAGE_X + 80, panelTop + 172, 16, 16).build());

		this.attemptButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.immortality.breakthrough.attempt"), button ->
			ClientPlayNetworking.send(new ResearchActionPayload(ResearchActionPayload.ACTION_BREAKTHROUGH, ""))
		).bounds(panelLeft + 22, panelTop + 196, 96, 20).build());
		this.attemptButton.active = canAttempt();

		this.backButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.immortality.breakthrough.back"), button ->
			ClientPlayNetworking.send(new ResearchActionPayload(ResearchActionPayload.ACTION_OPEN_RESEARCH, ""))
		).bounds(panelLeft + 138, panelTop + 196, 96, 20).build());

		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
			.bounds(panelLeft + PANEL_WIDTH / 2 - 25, panelTop + PANEL_HEIGHT + 4, 50, 20)
			.build());

		refreshButtons();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, this.width, this.height, 0x7A08060E);
		int panelLeft = (this.width - PANEL_WIDTH) / 2;
		int panelTop = (this.height - PANEL_HEIGHT) / 2;

		// Draw themed book background
		graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK_PAGE, panelLeft, panelTop, 0.0F, 0.0F, PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);

		drawLeftPanel(graphics, panelLeft, panelTop);
		drawRightPanel(graphics, panelLeft, panelTop);
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void drawLeftPanel(GuiGraphics graphics, int panelLeft, int panelTop) {
		int y = panelTop + 24;
		drawSectionTitle(graphics, panelLeft + LEFT_PAGE_X, y, "screen.immortality.breakthrough.overview");
		y += SECTION_SPACING;

		y = drawRow(graphics, panelLeft + LEFT_PAGE_X, y, Component.translatable("screen.immortality.breakthrough.row_chance"), percentComponent(this.chance), 0xFF7F5C1B);
		y = drawRow(graphics, panelLeft + LEFT_PAGE_X, y, Component.translatable("screen.immortality.breakthrough.row_qi"), Component.literal(this.currentQi + "/" + this.requiredQi), this.qiMet ? 0xFF1B5E20 : 0xFFB71C1C);
		y = drawRow(graphics, panelLeft + LEFT_PAGE_X, y, Component.translatable("screen.immortality.breakthrough.row_body"), this.body.displayNameComponent(), 0xFF3F4F8F);
		y = drawRow(graphics, panelLeft + LEFT_PAGE_X, y, Component.translatable("screen.immortality.breakthrough.row_purity"), percentComponent(this.purity), 0xFF1B5A8F);
		y = drawRow(graphics, panelLeft + LEFT_PAGE_X, y, Component.translatable("screen.immortality.breakthrough.row_stability"), percentComponent(this.stability), 0xFF2E6F2F);
		y = drawRow(graphics, panelLeft + LEFT_PAGE_X, y, Component.translatable("screen.immortality.breakthrough.row_deviation"), percentComponent(this.deviation), 0xFFB71C1C);
		y += SECTION_SPACING;

		drawSectionTitle(graphics, panelLeft + LEFT_PAGE_X, y, "screen.immortality.breakthrough.requirements");
		y += SECTION_SPACING;

		y = drawRequirement(graphics, panelLeft + LEFT_PAGE_X, y, this.researchMet, this.requiredResearchId.isEmpty()
			? Component.translatable("screen.immortality.breakthrough.requirement_no_research")
			: Component.translatable("screen.immortality.breakthrough.requirement_research", Component.translatable("research.immortality." + this.requiredResearchId + ".title")));
		y = drawRequirement(graphics, panelLeft + LEFT_PAGE_X, y, this.qiMet, Component.translatable("screen.immortality.breakthrough.requirement_qi", this.requiredQi));

		if (this.coreRequired) {
			y = drawRequirement(graphics, panelLeft + LEFT_PAGE_X, y, this.coreMet, this.coreName.isEmpty()
				? Component.translatable("screen.immortality.breakthrough.requirement_core_missing")
				: Component.translatable("screen.immortality.breakthrough.requirement_core", this.coreName));
			if (!this.coreName.isEmpty()) {
				drawCompatibility(graphics, panelLeft + LEFT_PAGE_X, y);
			}
		}
	}

	private void drawRightPanel(GuiGraphics graphics, int panelLeft, int panelTop) {
		int y = panelTop + 24;
		drawSectionTitle(graphics, panelLeft + RIGHT_PAGE_X, y, "screen.immortality.breakthrough.summary");
		y += SECTION_SPACING;

		List<DetailLine> lines = buildRightLines();
		int start = this.page * RIGHT_LINES_PER_PAGE;
		int end = Math.min(start + RIGHT_LINES_PER_PAGE, lines.size());
		for (int i = start; i < end; i++) {
			DetailLine line = lines.get(i);
			drawScaledText(graphics, line.text(), panelLeft + RIGHT_PAGE_X, y, UI_TEXT_SCALE, line.color());
			y += ROW_SPACING;
		}
	}

	private List<DetailLine> buildRightLines() {
		List<DetailLine> lines = new ArrayList<>();
		appendWrappedLines(lines, Component.translatable("screen.immortality.breakthrough.factors"), 0xFF3C1F10);
		appendWrappedLines(lines, factorLine("screen.immortality.breakthrough.factor_base", this.baseChance, true), 0xFF1B5E20);
		appendWrappedLines(lines, factorLine("screen.immortality.breakthrough.factor_purity", this.purityBonus, true), colorForSigned(this.purityBonus));
		appendWrappedLines(lines, factorLine("screen.immortality.breakthrough.factor_stability", this.stabilityBonus, true), colorForSigned(this.stabilityBonus));
		appendWrappedLines(lines, factorLine("screen.immortality.breakthrough.factor_body", this.bodyBonus, this.bodyBonus >= 0.0D), colorForSigned(this.bodyBonus));
		appendWrappedLines(lines, factorLine("screen.immortality.breakthrough.factor_manual", this.manualBonus, this.manualBonus >= 0.0D), colorForSigned(this.manualBonus));
		appendWrappedLines(lines, factorLine("screen.immortality.breakthrough.factor_technique", this.techniqueBonus, this.techniqueBonus >= 0.0D), colorForSigned(this.techniqueBonus));
		appendWrappedLines(lines, factorLine("screen.immortality.breakthrough.factor_core", this.coreBonus, this.coreBonus >= 0.0D), colorForSigned(this.coreBonus));
		appendWrappedLines(lines, factorLine("screen.immortality.breakthrough.factor_deviation", this.deviationPenalty, false), 0xFFB71C1C);

		if (!this.coreName.isEmpty()) {
			appendWrappedLines(lines, Component.empty(), 0xFF4A3A2A);
			appendWrappedLines(lines, Component.translatable("screen.immortality.breakthrough.core_effects"), 0xFF3C1F10);
			appendWrappedLines(lines, Component.translatable("screen.immortality.breakthrough.requirement_core", this.coreName), 0xFF7F5C1B);
			appendWrappedLines(lines, Component.translatable(compatibilityKey()), compatibilityColor());
			appendWrappedLines(lines, factorLine("screen.immortality.breakthrough.factor_core_stability", this.coreStabilityBonus, this.coreStabilityBonus >= 0.0D), colorForSigned(this.coreStabilityBonus));
			appendWrappedLines(lines, factorLine("screen.immortality.breakthrough.factor_core_purity", this.corePurityBonus, this.corePurityBonus >= 0.0D), colorForSigned(this.corePurityBonus));
			appendWrappedLines(lines, factorLine("screen.immortality.breakthrough.factor_core_deviation", this.coreDeviationPenalty, false), 0xFFB71C1C);
		}

		appendWrappedLines(lines, Component.empty(), 0xFF4A3A2A);
		appendWrappedLines(lines, Component.translatable("screen.immortality.breakthrough.consequences"), 0xFF3C1F10);
		appendWrappedLines(lines, Component.translatable("screen.immortality.breakthrough.consequence_failure"), 0xFF4A3A2A);
		appendWrappedLines(lines, Component.translatable("screen.immortality.breakthrough.consequence_severe"), 0xFFB71C1C);
		if (this.cooldownActive) {
			appendWrappedLines(lines, Component.translatable("screen.immortality.breakthrough.cooldown", this.cooldown / 20), 0xFFB71C1C);
		} else if (this.peakReached) {
			appendWrappedLines(lines, Component.translatable("message.immortality.breakthrough.peak"), 0xFFB71C1C);
		}
		return lines;
	}

	private void appendWrappedLines(List<DetailLine> lines, Component text, int color) {
		if (text.getString().isEmpty()) {
			lines.add(new DetailLine(FormattedCharSequence.forward(" ", net.minecraft.network.chat.Style.EMPTY), color));
			return;
		}
		for (FormattedCharSequence line : this.font.split(text, PAGE_WIDTH)) {
			lines.add(new DetailLine(line, color));
		}
	}

	private Component factorLine(String key, double val, boolean positive) {
		String sign = positive ? "+" : "-";
		return Component.translatable(key).append(Component.literal(": " + sign + percent(Math.abs(val))));
	}

	private int drawRow(GuiGraphics graphics, int x, int y, Component label, Component value, int color) {
		drawScaledText(graphics, label, x, y, UI_TEXT_SCALE, 0xFF4A3A2A);
		drawScaledText(graphics, value, x + 50, y, UI_TEXT_SCALE, color);
		return y + ROW_SPACING;
	}

	private int drawRequirement(GuiGraphics graphics, int x, int y, boolean met, Component text) {
		List<FormattedCharSequence> splitLines = this.font.split(Component.literal(met ? "+ " : "- ").append(text), PAGE_WIDTH);
		for (FormattedCharSequence line : splitLines) {
			drawScaledText(graphics, line, x, y, UI_TEXT_SCALE, met ? 0xFF1B5E20 : 0xFFB71C1C);
			y += ROW_SPACING;
		}
		return y;
	}

	private void drawCompatibility(GuiGraphics graphics, int x, int y) {
		drawScaledText(graphics, Component.translatable(compatibilityKey()), x, y, UI_TEXT_SCALE, compatibilityColor());
	}

	private void drawSectionTitle(GuiGraphics graphics, int x, int y, String key) {
		drawScaledText(graphics, Component.translatable(key), x, y, UI_TEXT_SCALE, 0xFF3C1F10);
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

	private boolean canAttempt() {
		return !this.peakReached && !this.cooldownActive && this.researchMet && this.qiMet && this.coreMet;
	}

	private int maxRightPage() {
		return Math.max(0, (buildRightLines().size() - 1) / RIGHT_LINES_PER_PAGE);
	}

	private void refreshButtons() {
		if (this.previousPageButton != null) {
			this.previousPageButton.active = this.page > 0;
			this.nextPageButton.active = this.page < maxRightPage();
		}
	}

	private String compatibilityKey() {
		return switch (this.coreCompatibility) {
			case "good" -> "screen.immortality.breakthrough.compatibility_good";
			case "bad" -> "screen.immortality.breakthrough.compatibility_bad";
			default -> "screen.immortality.breakthrough.compatibility_neutral";
		};
	}

	private int compatibilityColor() {
		return switch (this.coreCompatibility) {
			case "good" -> 0xFF1B5E20;
			case "bad" -> 0xFFB71C1C;
			default -> 0xFF4A3A2A;
		};
	}

	private int colorForSigned(double value) {
		return value >= 0.0D ? 0xFF1B5E20 : 0xFFB71C1C;
	}

	private static Component percentComponent(double value) {
		return Component.literal(percent(value));
	}

	private static String percent(double value) {
		return Math.round(value * 100.0D) + "%";
	}

	private record DetailLine(FormattedCharSequence text, int color) {
	}
}
