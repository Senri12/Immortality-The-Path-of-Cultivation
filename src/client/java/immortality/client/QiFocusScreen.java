package immortality.client;

import immortality.cultivation.CultivationStage;
import immortality.cultivation.QiFocus;
import immortality.network.QiFocusActionPayload;
import immortality.network.ResearchActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2fStack;
import java.util.LinkedHashSet;
import java.util.Set;
import immortality.Immortality;

public final class QiFocusScreen extends Screen {
	private static final Identifier BOOK_PAGE = Identifier.fromNamespaceAndPath(Immortality.MOD_ID, "textures/gui/book_page.png");
	private static final Identifier SILHOUETTE_TEXTURE = Identifier.fromNamespaceAndPath(Immortality.MOD_ID, "textures/gui/cultivator_silhouette.png");
	private static final int PANEL_WIDTH = 256;
	private static final int PANEL_HEIGHT = 256;
	private static final float TEXT_SCALE = 0.60F;
	private static final int LEFT_PAGE_X = 22;
	private static final int RIGHT_PAGE_X = 138;
	private static final int PAGE_WIDTH = 96;

	private static final int[][] BODY_PIXELS = {
		{0, 0, 1, 1, 1, 1, 0, 0},
		{0, 1, 1, 1, 1, 1, 1, 0},
		{0, 1, 1, 1, 1, 1, 1, 0},
		{0, 0, 1, 1, 1, 1, 0, 0},
		{0, 0, 0, 2, 2, 0, 0, 0},
		{3, 3, 0, 2, 2, 0, 3, 3},
		{3, 3, 2, 2, 2, 2, 3, 3},
		{3, 3, 2, 2, 2, 2, 3, 3},
		{0, 0, 2, 2, 2, 2, 0, 0},
		{0, 0, 2, 2, 2, 2, 0, 0},
		{0, 0, 2, 2, 2, 2, 0, 0},
		{0, 0, 0, 4, 4, 0, 0, 0},
		{0, 0, 4, 4, 4, 4, 0, 0},
		{0, 0, 4, 4, 4, 4, 0, 0},
		{0, 0, 4, 4, 4, 4, 0, 0},
		{0, 0, 4, 4, 4, 4, 0, 0}
	};

	private final Set<QiFocus> focuses;
	private final CultivationStage stage;
	private final int currentQi;
	private final int maxQi;
	private final int strengthLevel;
	private final int speedLevel;
	private final int jumpLevel;
	private final boolean flightUnlocked;
	private final int sightRadius;

	private Region hoveredRegion = Region.NONE;
	private Button clearButton;
	private Button backButton;
	private Button doneButton;

	public QiFocusScreen(CompoundTag data) {
		super(Component.translatable("screen.immortality.effects.title"));
		this.stage = CultivationStage.valueOf(data.getString("Stage").orElse(CultivationStage.MORTAL.name()));
		this.focuses = parseFocuses(data.getString("Focus").orElse(""));
		this.currentQi = data.getInt("CurrentQi").orElse(0);
		this.maxQi = data.getInt("MaxQi").orElse(0);
		this.strengthLevel = data.getInt("StrengthAmplifier").orElse(0);
		this.speedLevel = data.getInt("SpeedAmplifier").orElse(0);
		this.jumpLevel = data.getInt("JumpAmplifier").orElse(0);
		this.flightUnlocked = data.getBoolean("FlightUnlocked").orElse(false);
		this.sightRadius = data.getInt("SightRadius").orElse(0);
	}

	@Override
	protected void init() {
		super.init();
		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;

		this.clearButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.immortality.effects.clear"), button -> {
			ClientPlayNetworking.send(new QiFocusActionPayload("clear"));
			onClose();
		}).bounds(left + 139, top + 196, 45, 20).build());

		this.backButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.immortality.effects.back"), button ->
			ClientPlayNetworking.send(new ResearchActionPayload(ResearchActionPayload.ACTION_OPEN_RESEARCH, ""))
		).bounds(left + 188, top + 196, 45, 20).build());

		this.doneButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
			.bounds(left + PANEL_WIDTH / 2 - 25, top + PANEL_HEIGHT + 4, 50, 20)
			.build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		hoveredRegion = regionAt(mouseX, mouseY);
		graphics.fill(0, 0, this.width, this.height, 0x7A08060E);
		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;

		// Draw themed book background
		graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK_PAGE, left, top, 0.0F, 0.0F, PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);

		// Left page: Silhouette
		int ly = top + 24;
		drawScaledText(graphics, Component.translatable("screen.immortality.effects.silhouette"), left + 34, ly, TEXT_SCALE, 0xFF3C1F10);
		ly += 10;
		drawScaledText(graphics, Component.translatable("screen.immortality.effects.stage", this.stage.displayNameComponent()), left + 34, ly, 0.50F, 0xFF7F5C1B);

		drawBody(graphics, left + 34, top + 46);

		// Right page: Details
		int ry = top + 24;
		drawScaledText(graphics, Component.translatable("screen.immortality.effects.details"), left + RIGHT_PAGE_X, ry, TEXT_SCALE, 0xFF3C1F10);
		ry += 10;
		drawScaledText(graphics, Component.translatable("screen.immortality.effects.qi", this.currentQi, this.maxQi), left + RIGHT_PAGE_X, ry, 0.55F, 0xFF1B5A8F);
		ry += 8;
		drawScaledText(graphics, Component.translatable("screen.immortality.effects.current", currentFocusText()), left + RIGHT_PAGE_X, ry, 0.55F, 0xFF7F5C1B);
		ry += 12;

		drawDetails(graphics, left + RIGHT_PAGE_X, ry);

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		Region region = regionAt((int) event.x(), (int) event.y());
		if (region.focus != QiFocus.NONE) {
			selectFocus(region.focus);
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void selectFocus(QiFocus selected) {
		ClientPlayNetworking.send(new QiFocusActionPayload(selected.id()));
	}

	private void drawBody(GuiGraphics graphics, int x, int y) {
		// Draw the newly generated pixel art body silhouette!
		graphics.blit(RenderPipelines.GUI_TEXTURED, SILHOUETTE_TEXTURE, x, y, 0.0F, 0.0F, 80, 160, 80, 160);

		// Draw body overlays
		for (Region r : Region.values()) {
			if (r == Region.NONE) continue;
			int color = colorFor(r);
			int alpha = 0;
			if (this.focuses.contains(r.focus)) {
				alpha = 0x25; // Subtle focus fill
			} else if (this.hoveredRegion == r) {
				alpha = 0x15; // Even subtler hover fill
			}
			if (alpha > 0) {
				int overlayColor = (alpha << 24) | (color & 0xFFFFFF);
				int outlineColor = (0x9F << 24) | (color & 0xFFFFFF);
				if (r == Region.HEAD) {
					graphics.fill(x + 28, y + 10, x + 52, y + 42, overlayColor);
					graphics.renderOutline(x + 28, y + 10, 24, 32, outlineColor);
				} else if (r == Region.HANDS) {
					graphics.fill(x + 24, y + 95, x + 56, y + 114, overlayColor);
					graphics.renderOutline(x + 24, y + 95, 32, 19, outlineColor);
				} else if (r == Region.TORSO) {
					graphics.fill(x + 24, y + 43, x + 56, y + 94, overlayColor);
					graphics.renderOutline(x + 24, y + 43, 32, 51, outlineColor);
				} else if (r == Region.LEGS) {
					graphics.fill(x + 12, y + 115, x + 68, y + 155, overlayColor);
					graphics.renderOutline(x + 12, y + 115, 56, 40, outlineColor);
				}
			}
		}

		drawCallout(graphics, x + 40, y + 25, x - 16, y - 4, Region.HEAD);
		drawCallout(graphics, x + 30, y + 105, x - 16, y + 62, Region.HANDS);
		drawCallout(graphics, x + 40, y + 65, x + 82, y + 74, Region.TORSO);
		drawCallout(graphics, x + 68, y + 135, x + 82, y + 142, Region.LEGS);
	}

	private void drawCallout(GuiGraphics graphics, int fromX, int fromY, int toX, int toY, Region region) {
		int lineColor = this.hoveredRegion == region || this.focuses.contains(region.focus) ? 0xFF5C1D13 : 0xFF4A3A2A;
		graphics.hLine(Math.min(fromX, toX), Math.max(fromX, toX), fromY, lineColor);
		graphics.vLine(toX, Math.min(fromY, toY), Math.max(fromY, toY), lineColor);
		drawScaledText(graphics, Component.translatable(region.labelKey), toX + 3, toY - 4, 0.50F, lineColor);
	}

	private void drawDetails(GuiGraphics graphics, int x, int y) {
		Region detailsRegion = this.hoveredRegion == Region.NONE ? firstSelectedRegion() : this.hoveredRegion;
		drawScaledText(graphics, Component.translatable(detailsRegion.labelKey), x, y, TEXT_SCALE, 0xFF7F5C1B);
		int currentY = y + 12;
		for (Component line : detailsLines(detailsRegion)) {
			// Wrap details text on book page
			for (FormattedCharSequence splitLine : this.font.split(line, PAGE_WIDTH)) {
				drawScaledText(graphics, splitLine, x, currentY, 0.55F, 0xFF4A3A2A);
				currentY += 8;
			}
		}
	}

	private Component[] detailsLines(Region region) {
		return switch (region) {
			case HEAD -> new Component[] {
				Component.translatable("screen.immortality.effects.head_1"),
				Component.translatable("screen.immortality.effects.head_2", this.sightRadius),
				Component.translatable("screen.immortality.effects.head_3")
			};
			case HANDS -> new Component[] {
				Component.translatable("screen.immortality.effects.hands_1", Math.max(0, this.strengthLevel)),
				Component.translatable("screen.immortality.effects.hands_2"),
				Component.translatable("screen.immortality.effects.hands_3")
			};
			case TORSO -> new Component[] {
				Component.translatable(this.flightUnlocked ? "screen.immortality.effects.torso_1_on" : "screen.immortality.effects.torso_1_off"),
				Component.translatable("screen.immortality.effects.torso_2"),
				Component.translatable("screen.immortality.effects.torso_3")
			};
			case LEGS -> new Component[] {
				Component.translatable("screen.immortality.effects.legs_1", Math.max(0, this.speedLevel)),
				Component.translatable("screen.immortality.effects.legs_2", Math.max(0, this.jumpLevel)),
				Component.translatable("screen.immortality.effects.legs_3")
			};
			default -> new Component[] {
				Component.translatable("screen.immortality.effects.none_1"),
				Component.translatable("screen.immortality.effects.none_2"),
				Component.translatable("screen.immortality.effects.none_3")
			};
		};
	}

	private int colorFor(Region region) {
		if (this.focuses.contains(region.focus)) {
			return 0xFFE2C06F;
		}
		if (this.hoveredRegion == region) {
			return 0xFFC38BFF;
		}
		return switch (region) {
			case HEAD -> 0xFF6BA9D8;
			case HANDS -> 0xFFD86B6B;
			case TORSO -> 0xFF7CCF8A;
			case LEGS -> 0xFFC8B16D;
			default -> 0xFF514958;
		};
	}

	private Region regionAt(int mouseX, int mouseY) {
		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		int x = mouseX - (left + 34);
		int y = mouseY - (top + 46);
		if (x >= 28 && x <= 52 && y >= 10 && y <= 42) {
			return Region.HEAD;
		}
		if (x >= 24 && x <= 56 && y >= 95 && y <= 114) {
			return Region.HANDS;
		}
		if (x >= 24 && x <= 56 && y >= 43 && y <= 94) {
			return Region.TORSO;
		}
		if (x >= 12 && x <= 68 && y >= 115 && y <= 155) {
			return Region.LEGS;
		}
		return Region.NONE;
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

	private enum Region {
		NONE(QiFocus.NONE, "screen.immortality.effects.none"),
		HEAD(QiFocus.HEAD, "focus.immortality.head"),
		HANDS(QiFocus.HANDS, "focus.immortality.hands"),
		TORSO(QiFocus.TORSO, "focus.immortality.torso"),
		LEGS(QiFocus.LEGS, "focus.immortality.legs");

		private final QiFocus focus;
		private final String labelKey;

		Region(QiFocus focus, String labelKey) {
			this.focus = focus;
			this.labelKey = labelKey;
		}

		private static Region byPart(int part) {
			return switch (part) {
				case 1 -> HEAD;
				case 2 -> TORSO;
				case 3 -> HANDS;
				case 4 -> LEGS;
				default -> NONE;
			};
		}

		private static Region byFocus(QiFocus focus) {
			for (Region value : values()) {
				if (value.focus == focus) {
					return value;
				}
			}
			return NONE;
		}
	}

	private Component currentFocusText() {
		if (this.focuses.isEmpty()) {
			return QiFocus.NONE.displayName();
		}
		Component result = Component.empty();
		boolean first = true;
		for (QiFocus focus : java.util.List.of(QiFocus.HEAD, QiFocus.HANDS, QiFocus.TORSO, QiFocus.LEGS)) {
			if (!this.focuses.contains(focus)) {
				continue;
			}
			if (!first) {
				result = Component.empty().append(result).append(", ");
			}
			result = Component.empty().append(result).append(focus.displayName());
			first = false;
		}
		return result;
	}

	private Region firstSelectedRegion() {
		for (QiFocus focus : java.util.List.of(QiFocus.HEAD, QiFocus.HANDS, QiFocus.TORSO, QiFocus.LEGS)) {
			if (this.focuses.contains(focus)) {
				return Region.byFocus(focus);
			}
		}
		return Region.NONE;
	}

	private static Set<QiFocus> parseFocuses(String csv) {
		Set<QiFocus> result = new LinkedHashSet<>();
		if (csv == null || csv.isBlank()) {
			return result;
		}
		for (String token : csv.split(",")) {
			QiFocus focus = QiFocus.byId(token.trim());
			if (focus != QiFocus.NONE) {
				result.add(focus);
			}
		}
		return result;
	}
}
