package immortality.client;

import immortality.Immortality;
import immortality.network.ResearchActionPayload;
import immortality.network.TechniqueActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class CultivationHudControls {
	private static KeyMapping MOVE_LEFT;
	private static KeyMapping MOVE_RIGHT;
	private static KeyMapping MOVE_UP;
	private static KeyMapping MOVE_DOWN;
	private static KeyMapping SCALE_DOWN;
	private static KeyMapping SCALE_UP;
	private static KeyMapping RESET;
	private static KeyMapping NEXT_TECHNIQUE;
	private static KeyMapping CLEAR_TECHNIQUE;
	private static KeyMapping INVOKE_TECHNIQUE;
	private static KeyMapping OPEN_EFFECTS;
	private static boolean initialized;

	private CultivationHudControls() {
	}

	public static void init() {
		if (initialized) {
			return;
		}

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Immortality.MOD_ID, "hud_controls"));
		MOVE_LEFT = register("key.immortality.hud_left", GLFW.GLFW_KEY_J, category);
		MOVE_RIGHT = register("key.immortality.hud_right", GLFW.GLFW_KEY_L, category);
		MOVE_UP = register("key.immortality.hud_up", GLFW.GLFW_KEY_I, category);
		MOVE_DOWN = register("key.immortality.hud_down", GLFW.GLFW_KEY_K, category);
		SCALE_DOWN = register("key.immortality.hud_scale_down", GLFW.GLFW_KEY_LEFT_BRACKET, category);
		SCALE_UP = register("key.immortality.hud_scale_up", GLFW.GLFW_KEY_RIGHT_BRACKET, category);
		RESET = register("key.immortality.hud_reset", GLFW.GLFW_KEY_O, category);
		NEXT_TECHNIQUE = register("key.immortality.technique_next", GLFW.GLFW_KEY_P, category);
		CLEAR_TECHNIQUE = register("key.immortality.technique_clear", GLFW.GLFW_KEY_APOSTROPHE, category);
		INVOKE_TECHNIQUE = register("key.immortality.technique_invoke", GLFW.GLFW_KEY_SEMICOLON, category);
		OPEN_EFFECTS = register("key.immortality.effects_open", GLFW.GLFW_KEY_U, category);
		initialized = true;
	}

	public static void tick(Minecraft minecraft) {
		if (!initialized || minecraft.player == null) {
			return;
		}

		int movedX = 0;
		int movedY = 0;
		while (MOVE_LEFT.consumeClick()) {
			movedX -= 4;
		}
		while (MOVE_RIGHT.consumeClick()) {
			movedX += 4;
		}
		while (MOVE_UP.consumeClick()) {
			movedY -= 4;
		}
		while (MOVE_DOWN.consumeClick()) {
			movedY += 4;
		}
		if (movedX != 0 || movedY != 0) {
			ClientHudConfig.nudge(minecraft, movedX, movedY);
			minecraft.player.displayClientMessage(Component.translatable("message.immortality.hud_moved", ClientHudConfig.offsetX(), ClientHudConfig.offsetY()), true);
		}
		boolean scaled = false;
		while (SCALE_DOWN.consumeClick()) {
			ClientHudConfig.adjustScale(minecraft, -0.1D);
			scaled = true;
		}
		while (SCALE_UP.consumeClick()) {
			ClientHudConfig.adjustScale(minecraft, 0.1D);
			scaled = true;
		}
		if (scaled) {
			minecraft.player.displayClientMessage(Component.translatable("message.immortality.hud_scaled", percent(ClientHudConfig.scale())), true);
		}
		while (RESET.consumeClick()) {
			ClientHudConfig.reset(minecraft);
			minecraft.player.displayClientMessage(Component.translatable("message.immortality.hud_reset"), true);
		}
		while (NEXT_TECHNIQUE.consumeClick()) {
			ClientPlayNetworking.send(new TechniqueActionPayload(TechniqueActionPayload.ACTION_CYCLE_NEXT, ""));
		}
		while (CLEAR_TECHNIQUE.consumeClick()) {
			ClientPlayNetworking.send(new TechniqueActionPayload(TechniqueActionPayload.ACTION_CLEAR, ""));
		}
		while (INVOKE_TECHNIQUE.consumeClick()) {
			ClientPlayNetworking.send(new TechniqueActionPayload(TechniqueActionPayload.ACTION_INVOKE, ""));
		}
		while (OPEN_EFFECTS.consumeClick()) {
			ClientPlayNetworking.send(new ResearchActionPayload(ResearchActionPayload.ACTION_OPEN_EFFECTS, ""));
		}
	}

	private static KeyMapping register(String translationKey, int keyCode, KeyMapping.Category category) {
		return KeyBindingHelper.registerKeyBinding(new KeyMapping(
			translationKey,
			keyCode,
			category
		));
	}

	private static String percent(double value) {
		return Math.round(value * 100.0D * 100.0D) / 100L + "x";
	}
}
