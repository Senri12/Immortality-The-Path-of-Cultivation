package immortality.technique;

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

public final class TechniqueRegistry {
	public static final String NONE_ID = "none";
	private static final String TECHNIQUES_RESOURCE = "/data/immortality/techniques.json";
	private static final Gson GSON = new Gson();
	private static final Type TECHNIQUE_LIST_TYPE = new TypeToken<List<TechniqueEntry>>() { }.getType();
	private static final Map<String, TechniqueDefinition> DEFINITIONS = new LinkedHashMap<>();

	private TechniqueRegistry() {
	}

	public static void bootstrap() {
		if (!DEFINITIONS.isEmpty()) {
			return;
		}
		register(new TechniqueDefinition(NONE_ID, CultivationStage.MORTAL, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0, 0, 0, 0, "none"));
		loadFromJson();
	}

	public static TechniqueDefinition get(String id) {
		return DEFINITIONS.getOrDefault(id, DEFINITIONS.get(NONE_ID));
	}

	public static Collection<TechniqueDefinition> all() {
		return DEFINITIONS.values();
	}

	private static void register(TechniqueDefinition definition) {
		if (DEFINITIONS.containsKey(definition.id())) {
			throw new IllegalStateException("Duplicate technique id: " + definition.id());
		}
		DEFINITIONS.put(definition.id(), definition);
	}

	private static void loadFromJson() {
		try (InputStream stream = TechniqueRegistry.class.getResourceAsStream(TECHNIQUES_RESOURCE)) {
			if (stream == null) {
				throw new IllegalStateException("Missing technique resource: " + TECHNIQUES_RESOURCE);
			}
			try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				List<TechniqueEntry> entries = GSON.fromJson(reader, TECHNIQUE_LIST_TYPE);
				if (entries == null || entries.isEmpty()) {
					throw new IllegalStateException("Technique resource is empty: " + TECHNIQUES_RESOURCE);
				}
				for (TechniqueEntry entry : entries) {
					register(entry.toDefinition());
				}
			}
		} catch (IOException | JsonParseException exception) {
			throw new RuntimeException("Failed to load techniques from " + TECHNIQUES_RESOURCE, exception);
		}

		Immortality.LOGGER.info("Loaded {} techniques from JSON", DEFINITIONS.size() - 1);
	}

	private record TechniqueEntry(
		String id,
		String requiredStage,
		Double qiGainBonus,
		Double purityBonus,
		Double stabilityBonus,
		Double breakthroughBonus,
		Double deviationModifier,
		Integer activationQiCost,
		Integer activationCooldownTicks,
		Integer activationDurationTicks,
		Integer activationAmplifier,
		String activationEffect
	) {
		private TechniqueDefinition toDefinition() {
			if (id == null || id.isBlank()) {
				throw new IllegalStateException("Technique entry is missing id");
			}
			return new TechniqueDefinition(
				id,
				requiredStage != null && !requiredStage.isBlank() ? CultivationStage.valueOf(requiredStage) : CultivationStage.MORTAL,
				qiGainBonus != null ? qiGainBonus : 0.0D,
				purityBonus != null ? purityBonus : 0.0D,
				stabilityBonus != null ? stabilityBonus : 0.0D,
				breakthroughBonus != null ? breakthroughBonus : 0.0D,
				deviationModifier != null ? deviationModifier : 0.0D,
				activationQiCost != null ? activationQiCost : 0,
				activationCooldownTicks != null ? activationCooldownTicks : 0,
				activationDurationTicks != null ? activationDurationTicks : 0,
				activationAmplifier != null ? activationAmplifier : 0,
				activationEffect != null ? activationEffect : "none"
			);
		}
	}
}
