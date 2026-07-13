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

public final class ResearchRegistry {
	private static final String RESEARCHES_RESOURCE = "/data/immortality/researches.json";
	private static final Gson GSON = new Gson();
	private static final Type RESEARCH_LIST_TYPE = new TypeToken<List<ResearchEntry>>() { }.getType();
	private static final Map<String, ResearchDefinition> DEFINITIONS = new LinkedHashMap<>();

	private ResearchRegistry() {
	}

	public static void bootstrap() {
		if (!DEFINITIONS.isEmpty()) {
			return;
		}
		loadFromJson();
	}

	private static void register(ResearchDefinition definition) {
		if (DEFINITIONS.containsKey(definition.id())) {
			throw new IllegalStateException("Duplicate research id: " + definition.id());
		}
		DEFINITIONS.put(definition.id(), definition);
	}

	public static ResearchDefinition get(String id) {
		return DEFINITIONS.get(id);
	}

	public static Collection<ResearchDefinition> all() {
		return DEFINITIONS.values();
	}

	private static void loadFromJson() {
		try (InputStream stream = ResearchRegistry.class.getResourceAsStream(RESEARCHES_RESOURCE)) {
			if (stream == null) {
				throw new IllegalStateException("Missing research resource: " + RESEARCHES_RESOURCE);
			}

			try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				List<ResearchEntry> entries = GSON.fromJson(reader, RESEARCH_LIST_TYPE);
				if (entries == null || entries.isEmpty()) {
					throw new IllegalStateException("Research resource is empty: " + RESEARCHES_RESOURCE);
				}

				for (ResearchEntry entry : entries) {
					register(entry.toDefinition());
				}
			}
		} catch (IOException | JsonParseException exception) {
			throw new RuntimeException("Failed to load researches from " + RESEARCHES_RESOURCE, exception);
		}

		Immortality.LOGGER.info("Loaded {} researches from JSON", DEFINITIONS.size());
	}

	private record ResearchEntry(
		String id,
		String title,
		String description,
		String category,
		Integer column,
		Integer row,
		String requiredStage,
		List<String> prerequisites,
		List<String> requiredInsights,
		int qiCost,
		String requiredItemId,
		String aspectStart,
		String aspectEnd,
		String studyBoardId,
		Integer studyBoardQiLimit,
		String studyVictoryMode,
		List<PointEntry> studyStarts,
		List<PointEntry> studyFinishes,
		List<PointEntry> studyRequiredNodes,
		String rewardType,
		String rewardValue
	) {
		private ResearchDefinition toDefinition() {
			if (id == null || id.isBlank()) {
				throw new IllegalStateException("Research entry is missing id");
			}
			if (requiredStage == null || requiredStage.isBlank()) {
				throw new IllegalStateException("Research " + id + " is missing requiredStage");
			}
			if (rewardType == null || rewardType.isBlank()) {
				throw new IllegalStateException("Research " + id + " is missing rewardType");
			}

			return new ResearchDefinition(
				id,
				title != null ? title : id,
				description != null ? description : "",
				category != null ? category : "general",
				column != null ? column : 0,
				row != null ? row : 0,
				CultivationStage.valueOf(requiredStage),
				prerequisites != null ? List.copyOf(prerequisites) : Collections.emptyList(),
				requiredInsights != null ? List.copyOf(requiredInsights) : Collections.emptyList(),
				qiCost,
				requiredItemId,
				aspectStart != null && !aspectStart.isBlank() ? aspectStart : defaultAspectStart(id, category),
				aspectEnd != null && !aspectEnd.isBlank() ? aspectEnd : defaultAspectEnd(id, category),
				studyBoardId != null && !studyBoardId.isBlank() ? studyBoardId : defaultStudyBoard(requiredStage),
				studyBoardQiLimit,
				studyVictoryMode,
				points(studyStarts),
				points(studyFinishes),
				points(studyRequiredNodes),
				ResearchRewardType.valueOf(rewardType),
				rewardValue
			);
		}

		private static List<StudyBoardDefinition.BoardPoint> points(List<PointEntry> entries) {
			if (entries == null) {
				return List.of();
			}
			return entries.stream().map(PointEntry::toPoint).toList();
		}

		private static String defaultStudyBoard(String requiredStage) {
			CultivationStage stage = CultivationStage.valueOf(requiredStage);
			return stage.tier() >= CultivationStage.NASCENT_SOUL.tier() ? "great_lattice" : "small_ring";
		}

		private static String defaultAspectStart(String id, String category) {
			if (id.contains("water")) return "spirit";
			if (id.contains("wood")) return "water";
			if (id.contains("earth")) return "body";
			if (id.contains("metal")) return "earth";
			if (id.contains("fire")) return "fire";
			if (id.contains("yin")) return "water";
			if (id.contains("yang")) return "fire";
			if (id.contains("void")) return "void";
			return switch (category != null ? category : "general") {
				case "body" -> "body";
				case "soul" -> "spirit";
				case "dao" -> "yin";
				case "breakthrough" -> "yin";
				case "beast" -> "spirit";
				case "spirit" -> "wood";
				case "trial" -> "fire";
				case "element" -> "spirit";
				case "void" -> "void";
				default -> "spirit";
			};
		}

		private static String defaultAspectEnd(String id, String category) {
			if (id.contains("water")) return "water";
			if (id.contains("wood")) return "wood";
			if (id.contains("earth")) return "earth";
			if (id.contains("metal")) return "metal";
			if (id.contains("fire")) return "yang";
			if (id.contains("yin")) return "yin";
			if (id.contains("yang")) return "yang";
			if (id.contains("void")) return "void";
			return switch (category != null ? category : "general") {
				case "body" -> "metal";
				case "soul" -> "soul";
				case "dao" -> "yang";
				case "breakthrough" -> "yang";
				case "beast" -> "earth";
				case "spirit" -> "spirit";
				case "trial" -> "yang";
				case "element" -> "water";
				case "void" -> "yin";
				default -> "water";
			};
		}
	}

	private record PointEntry(Integer x, Integer y) {
		private StudyBoardDefinition.BoardPoint toPoint() {
			if (x == null || y == null) {
				throw new IllegalStateException("Research board point is missing coordinates");
			}
			return new StudyBoardDefinition.BoardPoint(x, y);
		}
	}
}
