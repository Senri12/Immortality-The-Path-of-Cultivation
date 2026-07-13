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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class QiAspectRegistry {
	private static final String RESOURCE = "/data/immortality/qi_aspects.json";
	private static final Gson GSON = new Gson();
	private static final Type LIST_TYPE = new TypeToken<List<AspectEntry>>() { }.getType();
	private static final Map<String, QiAspectDefinition> DEFINITIONS = new LinkedHashMap<>();

	private QiAspectRegistry() {
	}

	public static void bootstrap() {
		if (!DEFINITIONS.isEmpty()) {
			return;
		}
		try (InputStream stream = QiAspectRegistry.class.getResourceAsStream(RESOURCE)) {
			if (stream == null) {
				throw new IllegalStateException("Missing qi aspect resource: " + RESOURCE);
			}
			try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				List<AspectEntry> entries = GSON.fromJson(reader, LIST_TYPE);
				if (entries == null || entries.isEmpty()) {
					throw new IllegalStateException("Qi aspect resource is empty: " + RESOURCE);
				}
				for (AspectEntry entry : entries) {
					QiAspectDefinition definition = entry.toDefinition();
					DEFINITIONS.put(definition.id(), definition);
				}
			}
		} catch (IOException | JsonParseException exception) {
			throw new RuntimeException("Failed to load qi aspects from " + RESOURCE, exception);
		}
		Immortality.LOGGER.info("Loaded {} qi aspects from JSON", DEFINITIONS.size());
	}

	public static QiAspectDefinition get(String id) {
		return DEFINITIONS.get(id);
	}

	public static Collection<QiAspectDefinition> all() {
		return DEFINITIONS.values();
	}

	private record AspectEntry(String id, List<String> connections) {
		private QiAspectDefinition toDefinition() {
			if (id == null || id.isBlank()) {
				throw new IllegalStateException("Qi aspect entry is missing id");
			}
			return new QiAspectDefinition(id, connections != null ? List.copyOf(connections) : Collections.emptyList());
		}
	}
}
