package immortality.manual;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import immortality.Immortality;
import immortality.cultivation.CultivationStage;
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

public final class ManualRegistry {
	public static final String NONE_ID = "none";
	private static final String MANUALS_RESOURCE = "/data/immortality/manuals.json";
	private static final Gson GSON = new Gson();
	private static final Type MANUAL_LIST_TYPE = new TypeToken<List<ManualEntry>>() { }.getType();
	private static final Map<String, ManualDefinition> DEFINITIONS = new LinkedHashMap<>();
	private static final Map<String, ManualDefinition> BY_ITEM_ID = new LinkedHashMap<>();

	private ManualRegistry() {
	}

	public static void bootstrap() {
		if (!DEFINITIONS.isEmpty()) {
			return;
		}
		register(new ManualDefinition(NONE_ID, "", CultivationStage.MORTAL, List.of(), List.of(), List.of(), 0.0D, 0.0D));
		loadFromJson();
	}

	public static ManualDefinition get(String id) {
		return DEFINITIONS.getOrDefault(id, DEFINITIONS.get(NONE_ID));
	}

	public static ManualDefinition byItemId(String itemId) {
		return BY_ITEM_ID.get(itemId);
	}

	public static Collection<ManualDefinition> all() {
		return DEFINITIONS.values();
	}

	private static void register(ManualDefinition definition) {
		if (DEFINITIONS.containsKey(definition.id())) {
			throw new IllegalStateException("Duplicate manual id: " + definition.id());
		}
		DEFINITIONS.put(definition.id(), definition);
		if (!definition.itemId().isBlank()) {
			BY_ITEM_ID.put(definition.itemId(), definition);
		}
	}

	private static void loadFromJson() {
		try (InputStream stream = ManualRegistry.class.getResourceAsStream(MANUALS_RESOURCE)) {
			if (stream == null) {
				throw new IllegalStateException("Missing manual resource: " + MANUALS_RESOURCE);
			}
			try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				List<ManualEntry> entries = GSON.fromJson(reader, MANUAL_LIST_TYPE);
				if (entries == null || entries.isEmpty()) {
					throw new IllegalStateException("Manual resource is empty: " + MANUALS_RESOURCE);
				}
				for (ManualEntry entry : entries) {
					register(entry.toDefinition());
				}
			}
		} catch (IOException | JsonParseException exception) {
			throw new RuntimeException("Failed to load manuals from " + MANUALS_RESOURCE, exception);
		}

		Immortality.LOGGER.info("Loaded {} manuals from JSON", DEFINITIONS.size() - 1);
	}

	private record ManualEntry(
		String id,
		String itemId,
		String maxStage,
		List<String> allowedResearchIds,
		List<String> grantedTechniques,
		List<String> insightPool,
		Double breakthroughBonus,
		Double deviationModifier
	) {
		private ManualDefinition toDefinition() {
			if (id == null || id.isBlank()) {
				throw new IllegalStateException("Manual entry is missing id");
			}
			if (itemId == null || itemId.isBlank()) {
				throw new IllegalStateException("Manual " + id + " is missing itemId");
			}
			return new ManualDefinition(
				id,
				itemId,
				maxStage != null && !maxStage.isBlank() ? CultivationStage.valueOf(maxStage) : CultivationStage.MORTAL,
				allowedResearchIds != null ? List.copyOf(allowedResearchIds) : Collections.emptyList(),
				grantedTechniques != null ? List.copyOf(grantedTechniques) : Collections.emptyList(),
				insightPool != null ? List.copyOf(insightPool) : Collections.emptyList(),
				breakthroughBonus != null ? breakthroughBonus : 0.0D,
				deviationModifier != null ? deviationModifier : 0.0D
			);
		}
	}
}
