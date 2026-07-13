package immortality.client;

import immortality.cultivation.CultivationStage;
import immortality.network.ResearchActionPayload;
import immortality.network.TechniqueActionPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2fStack;
import immortality.Immortality;

public final class TechniqueScreen extends Screen {
	private static final Identifier BOOK_PAGE = Identifier.fromNamespaceAndPath(Immortality.MOD_ID, "textures/gui/book_page.png");
	private static final float UI_TEXT_SCALE = 0.60F;
	private static final float DETAIL_TEXT_SCALE = 0.50F;
	private static final int PANEL_WIDTH = 256;
	private static final int PANEL_HEIGHT = 256;

	private static final int LEFT_PAGE_X = 22;
	private static final int RIGHT_PAGE_X = 138;
	private static final int PAGE_WIDTH = 96;
	private static final int TECHNIQUES_PER_PAGE = 6;
	private static final int ROW_SPACING = 9;
	private static final int SECTION_SPACING = 6;

	private final CultivationStage stage;
	private final int currentQi;
	private final int maxQi;
	private final String activeTechniqueId;
	private final int techniqueCooldown;
	private final List<TechniqueEntry> techniques;
	private int page;
	private int selectedIndex;
	private Button previousPageButton;
	private Button nextPageButton;
	private Button setButton;
	private Button invokeButton;
	private Button clearButton;
	private Button backButton;
	private Button doneButton;
	private final List<Button> techniqueButtons = new ArrayList<>();

	public TechniqueScreen(CompoundTag data) {
		super(Component.translatable("screen.immortality.technique.title"));
		this.stage = CultivationStage.valueOf(data.getString("Stage").orElse(CultivationStage.MORTAL.name()));
		this.currentQi = data.getInt("CurrentQi").orElse(0);
		this.maxQi = data.getInt("MaxQi").orElse(0);
		this.activeTechniqueId = data.getString("ActiveTechnique").orElse("none");
		this.techniqueCooldown = data.getInt("TechniqueCooldown").orElse(0);
		this.techniques = readTechniques(data.getList("Techniques").orElseGet(ListTag::new));
		this.selectedIndex = Math.max(0, this.techniques.indexOf(this.techniques.stream().filter(TechniqueEntry::active).findFirst().orElse(null)));
	}

	@Override
	protected void init() {
		super.init();
		int panelLeft = (this.width - PANEL_WIDTH) / 2;
		int panelTop = (this.height - PANEL_HEIGHT) / 2;
		this.page = Math.min(this.page, maxPage());
		this.selectedIndex = clampSelectedIndex(this.selectedIndex);
		this.techniqueButtons.clear();

		this.previousPageButton = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> {
			this.page = Math.max(0, this.page - 1);
			this.selectedIndex = Math.min(this.selectedIndex, clampSelectedIndex(this.page * TECHNIQUES_PER_PAGE));
			updateTechniqueButtons();
			refreshButtons();
		}).bounds(panelLeft + LEFT_PAGE_X, panelTop + 172, 16, 16).build());

		this.nextPageButton = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> {
			this.page = Math.min(maxPage(), this.page + 1);
			this.selectedIndex = Math.min(clampSelectedIndex(this.page * TECHNIQUES_PER_PAGE), clampSelectedIndex(this.selectedIndex));
			updateTechniqueButtons();
			refreshButtons();
		}).bounds(panelLeft + LEFT_PAGE_X + 80, panelTop + 172, 16, 16).build());

		for (int slot = 0; slot < TECHNIQUES_PER_PAGE; slot++) {
			final int slotIndex = slot;
			Button techniqueButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
				int index = this.page * TECHNIQUES_PER_PAGE + slotIndex;
				if (index < this.techniques.size()) {
					this.selectedIndex = index;
					updateTechniqueButtons();
					refreshButtons();
				}
			}).bounds(panelLeft + 18, panelTop + 40 + slot * 15, 94, 13).build());
			this.techniqueButtons.add(techniqueButton);
		}

		this.setButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.immortality.technique.set_active"), button -> {
			TechniqueEntry selected = selectedTechnique();
			if (selected != null) {
				ClientPlayNetworking.send(new TechniqueActionPayload(TechniqueActionPayload.ACTION_SET, selected.id()));
				ClientPlayNetworking.send(new ResearchActionPayload(ResearchActionPayload.ACTION_OPEN_TECHNIQUES, ""));
			}
		}).bounds(panelLeft + 22, panelTop + 196, 46, 20).build());

		this.invokeButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.immortality.technique.invoke"), button -> {
			ClientPlayNetworking.send(new TechniqueActionPayload(TechniqueActionPayload.ACTION_INVOKE, ""));
			ClientPlayNetworking.send(new ResearchActionPayload(ResearchActionPayload.ACTION_OPEN_TECHNIQUES, ""));
		}).bounds(panelLeft + 72, panelTop + 196, 46, 20).build());

		this.clearButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.immortality.technique.clear"), button -> {
			ClientPlayNetworking.send(new TechniqueActionPayload(TechniqueActionPayload.ACTION_CLEAR, ""));
			ClientPlayNetworking.send(new ResearchActionPayload(ResearchActionPayload.ACTION_OPEN_TECHNIQUES, ""));
		}).bounds(panelLeft + 138, panelTop + 196, 46, 20).build());

		this.backButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.immortality.technique.back"), button ->
			ClientPlayNetworking.send(new ResearchActionPayload(ResearchActionPayload.ACTION_OPEN_RESEARCH, ""))
		).bounds(panelLeft + 188, panelTop + 196, 46, 20).build());

		this.doneButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
			.bounds(panelLeft + PANEL_WIDTH / 2 - 25, panelTop + PANEL_HEIGHT + 4, 50, 20)
			.build());

		updateTechniqueButtons();
		refreshButtons();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, this.width, this.height, 0x7A08060E);
		int panelLeft = (this.width - PANEL_WIDTH) / 2;
		int panelTop = (this.height - PANEL_HEIGHT) / 2;

		// Draw themed book background
		graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK_PAGE, panelLeft, panelTop, 0.0F, 0.0F, PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);

		drawLeftPanel(graphics, panelLeft, panelTop, mouseX, mouseY);
		drawRightPanel(graphics, panelLeft, panelTop);
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void drawLeftPanel(GuiGraphics graphics, int panelLeft, int panelTop, int mouseX, int mouseY) {
		int y = panelTop + 24;
		drawSectionTitle(graphics, panelLeft + LEFT_PAGE_X, y, "screen.immortality.technique.known");
		y += SECTION_SPACING + 4;
		if (this.techniques.isEmpty()) {
			drawScaledText(graphics, Component.translatable("screen.immortality.technique.none"), panelLeft + LEFT_PAGE_X, y, UI_TEXT_SCALE, 0xFF4A3A2A);
			return;
		}
		int start = this.page * TECHNIQUES_PER_PAGE;
		int end = Math.min(start + TECHNIQUES_PER_PAGE, this.techniques.size());
		for (int i = start; i < end; i++) {
			TechniqueEntry technique = this.techniques.get(i);
			boolean selected = i == this.selectedIndex;
			int color = technique.active() ? 0xFF7F5C1B : technique.stageMet() ? 0xFF1B5E20 : 0xFFB71C1C;
			if (selected || technique.active()) {
				// Soft golden glow for active/selected slot highlight
				graphics.fill(panelLeft + 18, y - 2, panelLeft + 118, y + 11, 0x40DDBA7A);
			}
			MutableComponent line = Component.literal(technique.active() ? "> " : technique.stageMet() ? "+ " : "- ");
			line.append(technique.title());
			drawScaledText(graphics, line, panelLeft + LEFT_PAGE_X, y, UI_TEXT_SCALE, color);
			y += ROW_SPACING + 6;
		}
	}

	private void drawRightPanel(GuiGraphics graphics, int panelLeft, int panelTop) {
		int y = panelTop + 24;
		drawSectionTitle(graphics, panelLeft + RIGHT_PAGE_X, y, "screen.immortality.technique.details");
		y += SECTION_SPACING;

		y = drawRow(graphics, panelLeft + RIGHT_PAGE_X, y, Component.translatable("screen.immortality.technique.row_stage"), this.stage.displayNameComponent(), 0xFF3F4F8F);
		y = drawRow(graphics, panelLeft + RIGHT_PAGE_X, y, Component.translatable("screen.immortality.technique.row_qi"), Component.literal(this.currentQi + "/" + this.maxQi), 0xFF1B5A8F);
		y = drawRow(graphics, panelLeft + RIGHT_PAGE_X, y, Component.translatable("screen.immortality.technique.row_cooldown"), Component.literal(Math.max(0, this.techniqueCooldown / 20) + "s"), this.techniqueCooldown > 0 ? 0xFFB71C1C : 0xFF1B5E20);
		y = drawRow(graphics, panelLeft + RIGHT_PAGE_X, y, Component.translatable("screen.immortality.technique.row_active_effect"), activeEffectLabel(), 0xFF7F5C1B);
		y += SECTION_SPACING - 3;

		TechniqueEntry selected = selectedTechnique();
		if (selected == null) {
			drawScaledText(graphics, Component.translatable("screen.immortality.technique.none"), panelLeft + RIGHT_PAGE_X, y, UI_TEXT_SCALE, 0xFF4A3A2A);
			return;
		}

		drawScaledText(graphics, selected.title(), panelLeft + RIGHT_PAGE_X, y, UI_TEXT_SCALE, selected.active() ? 0xFF7F5C1B : 0xFF3C1F10);
		y += ROW_SPACING;
		y = drawWrapped(graphics, selected.description(), panelLeft + RIGHT_PAGE_X, y, PAGE_WIDTH, 0xFF4A3A2A, 3);
		y += 1;
		y = drawRow(graphics, panelLeft + RIGHT_PAGE_X, y, Component.translatable("screen.immortality.technique.row_required_stage"), selected.requiredStage().displayNameComponent(), selected.stageMet() ? 0xFF1B5E20 : 0xFFB71C1C);
		y = drawRow(graphics, panelLeft + RIGHT_PAGE_X, y, Component.translatable("screen.immortality.technique.row_activation_qi"), Component.literal(String.valueOf(selected.activationQiCost())), this.currentQi >= selected.activationQiCost() ? 0xFF1B5E20 : 0xFFB71C1C);
		y = drawRow(graphics, panelLeft + RIGHT_PAGE_X, y, Component.translatable("screen.immortality.technique.row_activation_cooldown"), Component.literal(seconds(selected.activationCooldownTicks())), 0xFF1B5A8F);
		y = drawRow(graphics, panelLeft + RIGHT_PAGE_X, y, Component.translatable("screen.immortality.technique.row_activation_duration"), Component.literal(seconds(selected.activationDurationTicks())), 0xFF2E6F2F);
		y = drawRow(graphics, panelLeft + RIGHT_PAGE_X, y, Component.translatable("screen.immortality.technique.row_activation_effect"), Component.translatable("screen.immortality.technique.effect." + selected.activationEffect()), 0xFF7F5C1B);
		y += 2;
		y = drawBonus(graphics, panelLeft + RIGHT_PAGE_X, y, "screen.immortality.technique.bonus_qi", selected.qiGainBonus());
		y = drawBonus(graphics, panelLeft + RIGHT_PAGE_X, y, "screen.immortality.technique.bonus_purity", selected.purityBonus());
		y = drawBonus(graphics, panelLeft + RIGHT_PAGE_X, y, "screen.immortality.technique.bonus_stability", selected.stabilityBonus());
		y = drawBonus(graphics, panelLeft + RIGHT_PAGE_X, y, "screen.immortality.technique.bonus_breakthrough", selected.breakthroughBonus());
		drawBonus(graphics, panelLeft + RIGHT_PAGE_X, y, "screen.immortality.technique.bonus_deviation", selected.deviationModifier());
	}

	private int drawRow(GuiGraphics graphics, int x, int y, Component label, Component value, int color) {
		drawScaledText(graphics, label, x, y, DETAIL_TEXT_SCALE, 0xFF4A3A2A);
		drawScaledText(graphics, value, x + 50, y, DETAIL_TEXT_SCALE, color);
		return y + 7;
	}

	private int drawBonus(GuiGraphics graphics, int x, int y, String key, double value) {
		int color = value >= 0.0D ? 0xFF1B5E20 : 0xFFB71C1C;
		String prefix = value >= 0.0D ? "+" : "";
		drawScaledText(graphics, Component.translatable(key), x, y, DETAIL_TEXT_SCALE, 0xFF4A3A2A);
		drawScaledText(graphics, Component.literal(prefix + percent(value)), x + 50, y, DETAIL_TEXT_SCALE, color);
		return y + 7;
	}

	private void drawSectionTitle(GuiGraphics graphics, int x, int y, String key) {
		drawScaledText(graphics, Component.translatable(key), x, y, UI_TEXT_SCALE, 0xFF3C1F10);
	}

	private int drawWrapped(GuiGraphics graphics, Component text, int x, int y, int width, int color, int maxLines) {
		List<FormattedCharSequence> lines = this.font.split(text, width);
		int count = Math.min(lines.size(), maxLines);
		int currentY = y;
		for (int i = 0; i < count; i++) {
			drawScaledText(graphics, lines.get(i), x, currentY, DETAIL_TEXT_SCALE, color);
			currentY += 7;
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

	private TechniqueEntry selectedTechnique() {
		if (this.techniques.isEmpty()) {
			return null;
		}
		return this.techniques.get(clampSelectedIndex(this.selectedIndex));
	}

	private int clampSelectedIndex(int index) {
		return Math.max(0, Math.min(Math.max(0, this.techniques.size() - 1), index));
	}

	private int maxPage() {
		return Math.max(0, (this.techniques.size() - 1) / TECHNIQUES_PER_PAGE);
	}

	private void refreshButtons() {
		if (this.previousPageButton != null) {
			this.previousPageButton.active = this.page > 0;
			this.nextPageButton.active = this.page < maxPage();
			TechniqueEntry selected = selectedTechnique();
			boolean hasSelection = selected != null;
			this.setButton.active = hasSelection && !selected.active() && selected.stageMet();
			this.invokeButton.active = hasSelection && selected.active() && selected.stageMet() && this.techniqueCooldown <= 0 && this.currentQi >= selected.activationQiCost();
			this.clearButton.active = !"none".equals(this.activeTechniqueId);
		}
	}

	private void updateTechniqueButtons() {
		for (int slot = 0; slot < this.techniqueButtons.size(); slot++) {
			Button button = this.techniqueButtons.get(slot);
			int index = this.page * TECHNIQUES_PER_PAGE + slot;
			if (index >= this.techniques.size()) {
				button.visible = false;
				button.active = false;
				button.setMessage(Component.empty());
				continue;
			}
			TechniqueEntry technique = this.techniques.get(index);
			button.visible = true;
			button.active = true;
			MutableComponent line = Component.literal(technique.active() ? "> " : technique.stageMet() ? "+ " : "- ");
			line.append(technique.title());
			button.setMessage(line);
		}
	}

	private static List<TechniqueEntry> readTechniques(ListTag list) {
		List<TechniqueEntry> result = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entry = list.getCompound(i).orElseGet(CompoundTag::new);
			result.add(new TechniqueEntry(
				entry.getString("Id").orElse(""),
				entry.getBoolean("Active").orElse(false),
				entry.getBoolean("StageMet").orElse(false),
				CultivationStage.valueOf(entry.getString("RequiredStage").orElse(CultivationStage.MORTAL.name())),
				entry.getInt("ActivationQiCost").orElse(0),
				entry.getInt("ActivationCooldownTicks").orElse(0),
				entry.getInt("ActivationDurationTicks").orElse(0),
				entry.getInt("ActivationAmplifier").orElse(0),
				entry.getString("ActivationEffect").orElse("none"),
				entry.getDouble("QiGainBonus").orElse(0.0D),
				entry.getDouble("PurityBonus").orElse(0.0D),
				entry.getDouble("StabilityBonus").orElse(0.0D),
				entry.getDouble("BreakthroughBonus").orElse(0.0D),
				entry.getDouble("DeviationModifier").orElse(0.0D)
			));
		}
		return result;
	}

	private static String percent(double value) {
		return Math.round(value * 100.0D) + "%";
	}

	private static String seconds(int ticks) {
		return Math.max(0, ticks / 20) + "s";
	}

	private Component activeEffectLabel() {
		TechniqueEntry selected = selectedTechnique();
		if (selected == null || !selected.active()) {
			return Component.translatable("screen.immortality.technique.effect.none_selected");
		}
		return selected.title();
	}

	private record TechniqueEntry(
		String id,
		boolean active,
		boolean stageMet,
		CultivationStage requiredStage,
		int activationQiCost,
		int activationCooldownTicks,
		int activationDurationTicks,
		int activationAmplifier,
		String activationEffect,
		double qiGainBonus,
		double purityBonus,
		double stabilityBonus,
		double breakthroughBonus,
		double deviationModifier
	) {
		Component title() {
			return Component.translatable("technique.immortality." + this.id);
		}

		Component description() {
			return Component.translatable("technique.immortality." + this.id + ".description");
		}
	}
}
