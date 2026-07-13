package immortality.cultivation;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record StudyBoardDefinition(
	String id,
	int width,
	int height,
	int qiLimit,
	int yinYangTolerance,
	String victoryMode,
	List<BoardPoint> starts,
	List<BoardPoint> finishes,
	List<BoardPoint> requiredNodes,
	Set<BoardPoint> blocked,
	Map<BoardPoint, CellEffect> effects
) {
	public boolean contains(BoardPoint point) {
		return point.x() >= 0 && point.x() < this.width && point.y() >= 0 && point.y() < this.height;
	}

	public boolean isBlocked(BoardPoint point) {
		return this.blocked.contains(point);
	}

	public CellEffect effectAt(BoardPoint point) {
		return this.effects.get(point);
	}

	public record BoardPoint(int x, int y) {
	}

	public record CellEffect(
		BoardPoint point,
		List<String> favoredAspects,
		List<String> hostileAspects,
		int favoredQiDelta,
		int hostileQiDelta,
		int neutralQiDelta,
		String transformTo,
		String requiredAdjacentAspect,
		String requiredPolarity
	) {
		public int qiCost(String aspectId) {
			if (this.favoredAspects.contains(aspectId)) {
				return Math.max(0, 1 + this.favoredQiDelta);
			}
			if (this.hostileAspects.contains(aspectId)) {
				return Math.max(0, 1 + this.hostileQiDelta);
			}
			return Math.max(0, 1 + this.neutralQiDelta);
		}

		public boolean favors(String aspectId) {
			return this.favoredAspects.contains(aspectId);
		}

		public boolean hostileTo(String aspectId) {
			return this.hostileAspects.contains(aspectId);
		}

		public String transform(String aspectId) {
			return this.transformTo != null && !this.transformTo.isBlank() ? this.transformTo : aspectId;
		}
	}
}
