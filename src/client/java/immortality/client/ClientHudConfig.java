package immortality.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;

public final class ClientHudConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static HudLayout layout = new HudLayout(0, 0, 1.0D);
	private static final double MIN_SCALE = 0.5D;
	private static final double MAX_SCALE = 2.0D;

	private ClientHudConfig() {
	}

	public static void load(Minecraft minecraft) {
		Path path = configPath(minecraft);
		if (!Files.exists(path)) {
			save(minecraft);
			return;
		}

		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			HudLayout loaded = GSON.fromJson(reader, HudLayout.class);
			layout = normalize(loaded != null ? loaded : new HudLayout(0, 0, 1.0D));
		} catch (IOException exception) {
			layout = new HudLayout(0, 0, 1.0D);
		}
	}

	public static void save(Minecraft minecraft) {
		Path path = configPath(minecraft);
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(layout, writer);
			}
		} catch (IOException ignored) {
		}
	}

	public static int offsetX() {
		return layout.offsetX;
	}

	public static int offsetY() {
		return layout.offsetY;
	}

	public static double scale() {
		return layout.scale;
	}

	public static void nudge(Minecraft minecraft, int dx, int dy) {
		layout = new HudLayout(layout.offsetX + dx, layout.offsetY + dy, layout.scale);
		save(minecraft);
	}

	public static void adjustScale(Minecraft minecraft, double delta) {
		layout = new HudLayout(layout.offsetX, layout.offsetY, clampScale(layout.scale + delta));
		save(minecraft);
	}

	public static void reset(Minecraft minecraft) {
		layout = new HudLayout(0, 0, 1.0D);
		save(minecraft);
	}

	private static Path configPath(Minecraft minecraft) {
		return minecraft.gameDirectory.toPath().resolve("config").resolve("immortality-client.json");
	}

	private static HudLayout normalize(HudLayout layout) {
		return new HudLayout(layout.offsetX, layout.offsetY, clampScale(layout.scale));
	}

	private static double clampScale(double scale) {
		return Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
	}

	private record HudLayout(int offsetX, int offsetY, double scale) {
	}
}
