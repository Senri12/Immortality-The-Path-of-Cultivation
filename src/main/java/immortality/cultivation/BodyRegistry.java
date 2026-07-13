package immortality.cultivation;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import immortality.Immortality;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BodyRegistry {
	public static final String NONE_ID = "none";

	private static final String BODIES_RESOURCE = "/data/immortality/bodies.json";
	private static final Gson GSON = new Gson();
	private static final Type BODY_LIST_TYPE = new TypeToken<List<BodyEntry>>() { }.getType();
	private static final Map<String, BodyDefinition> DEFINITIONS = new LinkedHashMap<>();

	private BodyRegistry() {
	}

	public static void bootstrap() {
		if (!DEFINITIONS.isEmpty()) {
			return;
		}
		loadFromJson();
	}

	public static BodyDefinition get(String id) {
		BodyDefinition definition = DEFINITIONS.get(id);
		return definition != null ? definition : DEFINITIONS.get(NONE_ID);
	}

	private static void register(BodyDefinition definition) {
		if (DEFINITIONS.containsKey(definition.id())) {
			throw new IllegalStateException("Duplicate body id: " + definition.id());
		}
		DEFINITIONS.put(definition.id(), definition);
	}

	private static void loadFromJson() {
		try (InputStream stream = BodyRegistry.class.getResourceAsStream(BODIES_RESOURCE)) {
			if (stream == null) {
				throw new IllegalStateException("Missing body resource: " + BODIES_RESOURCE);
			}

			try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				List<BodyEntry> entries = GSON.fromJson(reader, BODY_LIST_TYPE);
				if (entries == null || entries.isEmpty()) {
					throw new IllegalStateException("Body resource is empty: " + BODIES_RESOURCE);
				}
				for (BodyEntry entry : entries) {
					register(entry.toDefinition());
				}
			}
		} catch (IOException | JsonParseException exception) {
			throw new RuntimeException("Failed to load bodies from " + BODIES_RESOURCE, exception);
		}

		if (!DEFINITIONS.containsKey(NONE_ID)) {
			throw new IllegalStateException("Body registry must contain body id: " + NONE_ID);
		}

		Immortality.LOGGER.info("Loaded {} body definitions from JSON", DEFINITIONS.size());
	}

	private record BodyEntry(String id, double breakthroughBonus, double stabilityBonus) {
		private BodyDefinition toDefinition() {
			if (id == null || id.isBlank()) {
				throw new IllegalStateException("Body entry is missing id");
			}
			return new BodyDefinition(id, breakthroughBonus, stabilityBonus);
		}
	}
}
