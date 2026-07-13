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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class StudyBoardRegistry {
	private static final String RESOURCE = "/data/immortality/study_boards.json";
	private static final Gson GSON = new Gson();
	private static final Type LIST_TYPE = new TypeToken<List<BoardEntry>>() { }.getType();
	private static final Map<String, StudyBoardDefinition> DEFINITIONS = new LinkedHashMap<>();

	private StudyBoardRegistry() {
	}

	public static void bootstrap() {
		if (!DEFINITIONS.isEmpty()) {
			return;
		}
		try (InputStream stream = StudyBoardRegistry.class.getResourceAsStream(RESOURCE)) {
			if (stream == null) {
				throw new IllegalStateException("Missing study board resource: " + RESOURCE);
			}
			try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				List<BoardEntry> entries = GSON.fromJson(reader, LIST_TYPE);
				if (entries == null || entries.isEmpty()) {
					throw new IllegalStateException("Study board resource is empty: " + RESOURCE);
				}
				for (BoardEntry entry : entries) {
					StudyBoardDefinition definition = entry.toDefinition();
					if (DEFINITIONS.put(definition.id(), definition) != null) {
						throw new IllegalStateException("Duplicate study board id: " + definition.id());
					}
				}
			}
		} catch (IOException | JsonParseException exception) {
			throw new RuntimeException("Failed to load study boards from " + RESOURCE, exception);
		}
		Immortality.LOGGER.info("Loaded {} study boards from JSON", DEFINITIONS.size());
	}

	public static StudyBoardDefinition get(String id) {
		return DEFINITIONS.get(id);
	}

	public static Collection<StudyBoardDefinition> all() {
		return DEFINITIONS.values();
	}

	private record BoardEntry(
		String id,
		Integer width,
		Integer height,
		Integer qiLimit,
		Integer yinYangTolerance,
		String victoryMode,
		List<PointEntry> starts,
		List<PointEntry> finishes,
		List<PointEntry> requiredNodes,
		List<PointEntry> blocked,
		List<CellEffectEntry> effects
	) {
		private StudyBoardDefinition toDefinition() {
			if (id == null || id.isBlank()) {
				throw new IllegalStateException("Study board entry is missing id");
			}
			if (width == null || height == null || width <= 0 || height <= 0) {
				throw new IllegalStateException("Study board " + id + " has invalid size");
			}
			List<StudyBoardDefinition.BoardPoint> startPoints = points(starts);
			List<StudyBoardDefinition.BoardPoint> finishPoints = points(finishes);
			if (startPoints.isEmpty() || finishPoints.isEmpty()) {
				throw new IllegalStateException("Study board " + id + " needs at least one start and finish");
			}
			return new StudyBoardDefinition(
				id,
				width,
				height,
				qiLimit != null ? qiLimit : 0,
				yinYangTolerance != null ? yinYangTolerance : 99,
				victoryMode != null ? victoryMode : "single_path",
				startPoints,
				finishPoints,
				points(requiredNodes),
				new LinkedHashSet<>(points(blocked)),
				cellEffects(effects)
			);
		}

		private static List<StudyBoardDefinition.BoardPoint> points(List<PointEntry> entries) {
			if (entries == null) {
				return List.of();
			}
			return entries.stream().map(PointEntry::toPoint).toList();
		}

		private static Map<StudyBoardDefinition.BoardPoint, StudyBoardDefinition.CellEffect> cellEffects(List<CellEffectEntry> entries) {
			Map<StudyBoardDefinition.BoardPoint, StudyBoardDefinition.CellEffect> effects = new LinkedHashMap<>();
			if (entries == null) {
				return effects;
			}
			for (CellEffectEntry entry : entries) {
				StudyBoardDefinition.CellEffect effect = entry.toEffect();
				effects.put(effect.point(), effect);
			}
			return effects;
		}
	}

	private record PointEntry(Integer x, Integer y) {
		private StudyBoardDefinition.BoardPoint toPoint() {
			if (x == null || y == null) {
				throw new IllegalStateException("Study board point is missing coordinates");
			}
			return new StudyBoardDefinition.BoardPoint(x, y);
		}
	}

	private record CellEffectEntry(
		Integer x,
		Integer y,
		List<String> favoredAspects,
		List<String> hostileAspects,
		Integer favoredQiDelta,
		Integer hostileQiDelta,
		Integer neutralQiDelta,
		String transformTo,
		String requiredAdjacentAspect,
		String requiredPolarity
	) {
		private StudyBoardDefinition.CellEffect toEffect() {
			if (x == null || y == null) {
				throw new IllegalStateException("Study board effect point is missing coordinates");
			}
			return new StudyBoardDefinition.CellEffect(
				new StudyBoardDefinition.BoardPoint(x, y),
				favoredAspects != null ? List.copyOf(favoredAspects) : List.of(),
				hostileAspects != null ? List.copyOf(hostileAspects) : List.of(),
				favoredQiDelta != null ? favoredQiDelta : 0,
				hostileQiDelta != null ? hostileQiDelta : 0,
				neutralQiDelta != null ? neutralQiDelta : 0,
				transformTo,
				requiredAdjacentAspect,
				requiredPolarity != null ? requiredPolarity : "none"
			);
		}
	}
}
