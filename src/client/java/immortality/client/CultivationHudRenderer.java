package immortality.client;

import immortality.Immortality;
import immortality.cultivation.CultivationData;
import immortality.cultivation.CultivationStage;
import immortality.technique.TechniqueService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

public final class CultivationHudRenderer {
	private static final int FIGURE_WIDTH = 48;
	private static final int FIGURE_HEIGHT = 72;
	private static final int TEXT_LINE_HEIGHT = 10;

	private static final Identifier FIGURE_MORTAL = texture("gui/cultivator_mortal");
	private static final Identifier FIGURE_QI_GATHERING = texture("gui/cultivator_qi_gathering");
	private static final Identifier FIGURE_FOUNDATION = texture("gui/cultivator_foundation_establishment");
	private static final Identifier FIGURE_CORE = texture("gui/cultivator_core_formation");
	private static final Identifier FIGURE_NASCENT_SOUL = texture("gui/cultivator_nascent_soul");
	private static final Identifier FIGURE_SPIRIT_SEVERING = texture("gui/cultivator_spirit_severing");
	private static final Identifier FIGURE_ASCENDANT = texture("gui/cultivator_ascendant");
	private static final Identifier FIGURE_ILLUSORY_YIN = texture("gui/cultivator_illusory_yin");
	private static final Identifier FIGURE_CORPOREAL_YANG = texture("gui/cultivator_corporeal_yang");
	private static final Identifier FIGURE_NIRVANA_SCRYER = texture("gui/cultivator_nirvana_scryer");
	private static final Identifier FIGURE_NIRVANA_CLEANSER = texture("gui/cultivator_nirvana_cleanser");
	private static final Identifier FIGURE_VOID_TRIBULANT = texture("gui/cultivator_void_tribulant");
	private static final Identifier HUD_FRAME = texture("gui/hud_background");

	private CultivationHudRenderer() {
	}

	public static void render(GuiGraphics graphics) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.options.hideGui || minecraft.player == null || minecraft.screen != null) {
			return;
		}

		CultivationData data = ClientCultivationState.get();
		double scale = ClientHudConfig.scale();
		int baseWidth = 110;
		int baseHeight = 190;
		int originX = graphics.guiWidth() - (int) Math.round(baseWidth * scale) - 20 + ClientHudConfig.offsetX();
		int originY = graphics.guiHeight() / 2 - (int) Math.round(baseHeight * scale / 2.0D) + ClientHudConfig.offsetY();

		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(originX, originY);
		pose.scale((float) scale, (float) scale);

		// Draw the beautiful themed HUD frame as a perfect circle (square dimensions)
		{
			Matrix3x2fStack p = graphics.pose();
			p.pushMatrix();
			float frameScale = (float) baseWidth / 1024.0F;
			p.scale(frameScale, frameScale);
			graphics.blit(RenderPipelines.GUI_TEXTURED, HUD_FRAME, 0, 0, 0.0F, 0.0F, 1024, 1024, 1024, 1024);
			p.popMatrix();
		}

		int figureX = 31;
		// Center the figure (72px height) vertically inside the 110px circle: (110 - 72) / 2 = 19
		int figureY = 19;

		renderAura(graphics, figureX, figureY, data);
		{
			Matrix3x2fStack p = graphics.pose();
			p.pushMatrix();
			p.translate(figureX, figureY);
			float scaleX = (float) FIGURE_WIDTH / 1024.0F;
			float scaleY = (float) FIGURE_HEIGHT / 1024.0F;
			p.scale(scaleX, scaleY);
			graphics.blit(RenderPipelines.GUI_TEXTURED, stageTexture(data.stage()), 0, 0, 0.0F, 0.0F, 1024, 1024, 1024, 1024);
			p.popMatrix();
		}

		// Start drawing text below the 110px circle with dropshadow and center alignment
		int textY = baseWidth + 6;
		int centerX = baseWidth / 2;

		drawCenteredText(graphics, minecraft, data.stage().displayNameComponent(), centerX, textY, 0xFFF2E9FF);
		drawCenteredText(graphics, minecraft, Component.translatable("hud.immortality.qi", data.currentQi(), data.maxQi()), centerX, textY + TEXT_LINE_HEIGHT, 0xFF9FE8FF);
		drawCenteredText(graphics, minecraft, Component.translatable("hud.immortality.purity", percent(data.purity())), centerX, textY + TEXT_LINE_HEIGHT * 2, 0xFFAEEBFF);
		drawCenteredText(graphics, minecraft, Component.translatable("hud.immortality.deviation", percent(data.deviation())), centerX, textY + TEXT_LINE_HEIGHT * 3, 0xFFFFBDCA);
		drawCenteredText(graphics, minecraft, Component.translatable("hud.immortality.body_short", data.body().displayNameComponent()), centerX, textY + TEXT_LINE_HEIGHT * 4, 0xFFCDBDE6);
		drawCenteredText(graphics, minecraft, Component.translatable("hud.immortality.manual_short", data.manual().titleComponent()), centerX, textY + TEXT_LINE_HEIGHT * 5, 0xFFE5D59A);
		drawCenteredText(graphics, minecraft, Component.translatable("hud.immortality.technique_short", TechniqueService.activeTechnique(data).titleComponent()), centerX, textY + TEXT_LINE_HEIGHT * 6, 0xFF9CF0B4);
		if (data.techniqueCooldown() > 0) {
			drawCenteredText(graphics, minecraft, Component.translatable("hud.immortality.technique_cooldown", data.techniqueCooldown() / 20), centerX, textY + TEXT_LINE_HEIGHT * 7, 0xFFE7B87D);
		}
		
		pose.popMatrix();
	}

	private static void renderAura(GuiGraphics graphics, int figureX, int figureY, CultivationData data) {
		int pulse = (int) (Math.abs(Math.sin((System.currentTimeMillis() % 5000L) / 5000.0D * Math.PI * 2.0D)) * 30.0D);
		int purityAlpha = 40 + (int) Math.round(data.purity() * 90.0D) + pulse;
		purityAlpha = Math.max(50, Math.min(160, purityAlpha));
		int deviationAlpha = 20 + (int) Math.round(data.deviation() * 110.0D);
		int deviationColor = (Math.max(0, Math.min(120, deviationAlpha)) << 24) | 0xD43758;

		// We do not draw flat rectangular fills behind the cultivator to preserve the beautiful round theme and pixel-art aura!
		if (data.deviation() > 0.0D) {
			graphics.fill(figureX + FIGURE_WIDTH - 6, figureY + 8, figureX + FIGURE_WIDTH - 2, figureY + FIGURE_HEIGHT - 8, deviationColor);
			graphics.fill(figureX - 2, figureY + FIGURE_HEIGHT - 10, figureX + 2, figureY + FIGURE_HEIGHT - 2, deviationColor);
		}
	}

	private static void drawCenteredText(GuiGraphics graphics, Minecraft minecraft, Component text, int centerX, int y, int color) {
		int width = minecraft.font.width(text);
		graphics.drawString(minecraft.font, text, centerX - width / 2, y, color, true);
	}

	private static int stageAuraColor(CultivationStage stage) {
		return switch (stage) {
			case MORTAL -> 0x6C647C;
			case QI_GATHERING -> 0x4E9EFF;
			case FOUNDATION_ESTABLISHMENT -> 0x7C73FF;
			case CORE_FORMATION -> 0xB870FF;
			case NASCENT_SOUL -> 0xFF8CF6;
			case SPIRIT_SEVERING -> 0xFF6FA8;
			case ASCENDANT -> 0xFF9A57;
			case ILLUSORY_YIN -> 0x7AFFF0;
			case CORPOREAL_YANG -> 0xFFE066;
			case NIRVANA_SCRYER -> 0x9CFF7A;
			case NIRVANA_CLEANSER -> 0xFFFFFF;
			case VOID_TRIBULANT -> 0x7A7A7A;
		};
	}

	private static Identifier stageTexture(CultivationStage stage) {
		return switch (stage) {
			case MORTAL -> FIGURE_MORTAL;
			case QI_GATHERING -> FIGURE_QI_GATHERING;
			case FOUNDATION_ESTABLISHMENT -> FIGURE_FOUNDATION;
			case CORE_FORMATION -> FIGURE_CORE;
			case NASCENT_SOUL -> FIGURE_NASCENT_SOUL;
			case SPIRIT_SEVERING -> FIGURE_SPIRIT_SEVERING;
			case ASCENDANT -> FIGURE_ASCENDANT;
			case ILLUSORY_YIN -> FIGURE_ILLUSORY_YIN;
			case CORPOREAL_YANG -> FIGURE_CORPOREAL_YANG;
			case NIRVANA_SCRYER -> FIGURE_NIRVANA_SCRYER;
			case NIRVANA_CLEANSER -> FIGURE_NIRVANA_CLEANSER;
			case VOID_TRIBULANT -> FIGURE_VOID_TRIBULANT;
		};
	}

	private static Identifier texture(String path) {
		return Identifier.fromNamespaceAndPath(Immortality.MOD_ID, "textures/" + path + ".png");
	}

	private static String percent(double value) {
		return Math.round(value * 100.0D) + "%";
	}
}
