package immortality.beast;

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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BeastCoreRegistry {
	private static final String CORES_RESOURCE = "/data/immortality/beast_cores.json";
	private static final Gson GSON = new Gson();
	private static final Type CORE_LIST_TYPE = new TypeToken<List<BeastCoreEntry>>() { }.getType();
	private static final Map<String, BeastCoreDefinition> DEFINITIONS = new LinkedHashMap<>();
	private static final Map<String, BeastCoreDefinition> BY_ITEM_ID = new LinkedHashMap<>();

	private BeastCoreRegistry() {
	}

	public static void bootstrap() {
		if (!DEFINITIONS.isEmpty()) {
			return;
		}
		loadFromJson();
	}

	public static BeastCoreDefinition byItemId(String itemId) {
		return BY_ITEM_ID.get(itemId);
	}

	private static void register(BeastCoreDefinition definition) {
		if (DEFINITIONS.containsKey(definition.id())) {
			throw new IllegalStateException("Duplicate beast core id: " + definition.id());
		}
		DEFINITIONS.put(definition.id(), definition);
		BY_ITEM_ID.put(definition.itemId(), definition);
	}

	private static void loadFromJson() {
		try (InputStream stream = BeastCoreRegistry.class.getResourceAsStream(CORES_RESOURCE)) {
			if (stream == null) {
				throw new IllegalStateException("Missing beast core resource: " + CORES_RESOURCE);
			}

			try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				List<BeastCoreEntry> entries = GSON.fromJson(reader, CORE_LIST_TYPE);
				if (entries == null || entries.isEmpty()) {
					throw new IllegalStateException("Beast core resource is empty: " + CORES_RESOURCE);
				}
				for (BeastCoreEntry entry : entries) {
					register(entry.toDefinition());
				}
			}
		} catch (IOException | JsonParseException exception) {
			throw new RuntimeException("Failed to load beast cores from " + CORES_RESOURCE, exception);
		}

		Immortality.LOGGER.info("Loaded {} beast core definitions from JSON", DEFINITIONS.size());
	}

	private record BeastCoreEntry(
		String id,
		String itemId,
		double breakthroughBonus,
		double stabilityBonus,
		double deviationPenalty,
		double purityBonus,
		List<String> compatibleBodies
	) {
		private BeastCoreDefinition toDefinition() {
			if (id == null || id.isBlank()) {
				throw new IllegalStateException("Beast core entry is missing id");
			}
			if (itemId == null || itemId.isBlank()) {
				throw new IllegalStateException("Beast core entry " + id + " is missing itemId");
			}
			return new BeastCoreDefinition(
				id,
				itemId,
				breakthroughBonus,
				stabilityBonus,
				deviationPenalty,
				purityBonus,
				compatibleBodies != null ? List.copyOf(compatibleBodies) : Collections.emptyList()
			);
		}
	}
}
