package immortality.cultivation;

import java.util.List;
import net.minecraft.network.chat.Component;

public record ResearchDefinition(
	String id,
	String title,
	String description,
	String category,
	int column,
	int row,
	CultivationStage requiredStage,
	List<String> prerequisites,
	List<String> requiredInsights,
	int qiCost,
	String requiredItemId,
	String aspectStart,
	String aspectEnd,
	String studyBoardId,
	Integer studyBoardQiLimit,
	String studyVictoryMode,
	List<StudyBoardDefinition.BoardPoint> studyStarts,
	List<StudyBoardDefinition.BoardPoint> studyFinishes,
	List<StudyBoardDefinition.BoardPoint> studyRequiredNodes,
	ResearchRewardType rewardType,
	String rewardValue
) {
	public String titleKey() {
		return "research.immortality." + id + ".title";
	}

	public String descriptionKey() {
		return "research.immortality." + id + ".description";
	}

	public Component titleComponent() {
		return Component.translatable(titleKey());
	}

	public Component descriptionComponent() {
		return Component.translatable(descriptionKey());
	}
}
