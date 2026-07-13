package immortality.cultivation;

import immortality.manual.ManualDefinition;
import immortality.manual.ManualRegistry;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class CultivationData {
	private CultivationStage stage = CultivationStage.MORTAL;
	private int currentQi;
	private int maxQi = CultivationStage.MORTAL.qiCapacity();
	private double purity = 0.35D;
	private double stability = 0.55D;
	private double deviation;
	private String bodyId = BodyRegistry.NONE_ID;
	private String manualId = ManualRegistry.NONE_ID;
	private QiType dominantQi = QiType.SPIRIT;
	private int meditationTicks;
	private int breakthroughCooldown;
	private int techniqueCooldown;
	private final Set<String> knownResearches = new LinkedHashSet<>();
	private final Set<String> knownInsights = new LinkedHashSet<>();
	private final Set<String> knownTechniques = new LinkedHashSet<>();
	private final Set<QiFocus> qiFocuses = new LinkedHashSet<>();
	private String activeTechniqueId = "none";
	private String preparedResearchId = "";

	public CultivationStage stage() {
		return this.stage;
	}

	public void setStage(CultivationStage stage) {
		this.stage = stage;
		this.maxQi = stage.qiCapacity();
		this.currentQi = Math.min(this.currentQi, this.maxQi);
	}

	public int currentQi() {
		return this.currentQi;
	}

	public int addQi(int amount) {
		int before = this.currentQi;
		this.currentQi = Math.max(0, Math.min(this.maxQi, this.currentQi + amount));
		return this.currentQi - before;
	}

	public void setCurrentQi(int currentQi) {
		this.currentQi = Math.max(0, Math.min(this.maxQi, currentQi));
	}

	public int maxQi() {
		return this.maxQi;
	}

	public double purity() {
		return this.purity;
	}

	public void addPurity(double amount) {
		this.purity = clamp(this.purity + amount);
	}

	public double stability() {
		return this.stability;
	}

	public void addStability(double amount) {
		this.stability = clamp(this.stability + amount);
	}

	public double deviation() {
		return this.deviation;
	}

	public void addDeviation(double amount) {
		this.deviation = clamp(this.deviation + amount);
	}

	public String bodyId() {
		return this.bodyId;
	}

	public BodyDefinition body() {
		return BodyRegistry.get(this.bodyId);
	}

	public void setBody(String bodyId) {
		this.bodyId = bodyId != null && !bodyId.isBlank() ? bodyId : BodyRegistry.NONE_ID;
	}

	public String manualId() {
		return this.manualId;
	}

	public ManualDefinition manual() {
		return ManualRegistry.get(this.manualId);
	}

	public void setManual(String manualId) {
		this.manualId = manualId != null && !manualId.isBlank() ? manualId : ManualRegistry.NONE_ID;
	}

	public int meditationTicks() {
		return this.meditationTicks;
	}

	public void setMeditationTicks(int meditationTicks) {
		this.meditationTicks = meditationTicks;
	}

	public int breakthroughCooldown() {
		return this.breakthroughCooldown;
	}

	public void setBreakthroughCooldown(int breakthroughCooldown) {
		this.breakthroughCooldown = Math.max(0, breakthroughCooldown);
	}

	public int techniqueCooldown() {
		return this.techniqueCooldown;
	}

	public void setTechniqueCooldown(int techniqueCooldown) {
		this.techniqueCooldown = Math.max(0, techniqueCooldown);
	}

	public boolean knows(String researchId) {
		return this.knownResearches.contains(researchId);
	}

	public void learn(String researchId) {
		this.knownResearches.add(researchId);
	}

	public boolean knowsInsight(String insightId) {
		return this.knownInsights.contains(insightId);
	}

	public void learnInsight(String insightId) {
		this.knownInsights.add(insightId);
	}

	public Set<String> knownInsights() {
		return Set.copyOf(this.knownInsights);
	}

	public boolean knowsTechnique(String techniqueId) {
		return this.knownTechniques.contains(techniqueId);
	}

	public void learnTechnique(String techniqueId) {
		this.knownTechniques.add(techniqueId);
	}

	public Set<String> knownTechniques() {
		return Set.copyOf(this.knownTechniques);
	}

	public String activeTechniqueId() {
		return this.activeTechniqueId;
	}

	public void setActiveTechnique(String techniqueId) {
		if (techniqueId == null || techniqueId.isBlank() || "none".equals(techniqueId)) {
			this.activeTechniqueId = "none";
			return;
		}
		this.activeTechniqueId = this.knownTechniques.contains(techniqueId) ? techniqueId : "none";
	}

	public String preparedResearchId() {
		return this.preparedResearchId;
	}

	public void setPreparedResearchId(String researchId) {
		this.preparedResearchId = researchId == null ? "" : researchId;
	}

	public Set<QiFocus> qiFocuses() {
		return Set.copyOf(this.qiFocuses);
	}

	public boolean hasQiFocus(QiFocus qiFocus) {
		return qiFocus != null && qiFocus != QiFocus.NONE && this.qiFocuses.contains(qiFocus);
	}

	public void setQiFocuses(Set<QiFocus> focuses) {
		this.qiFocuses.clear();
		if (focuses != null) {
			for (QiFocus focus : focuses) {
				if (focus != null && focus != QiFocus.NONE) {
					this.qiFocuses.add(focus);
				}
			}
		}
	}

	public void clearQiFocuses() {
		this.qiFocuses.clear();
	}

	public void toggleQiFocus(QiFocus qiFocus) {
		if (qiFocus == null || qiFocus == QiFocus.NONE) {
			this.qiFocuses.clear();
			return;
		}
		if (!this.qiFocuses.add(qiFocus)) {
			this.qiFocuses.remove(qiFocus);
		}
	}

	public CompoundTag toNbt() {
		CompoundTag nbt = new CompoundTag();
		nbt.putString("Stage", this.stage.name());
		nbt.putInt("CurrentQi", this.currentQi);
		nbt.putDouble("Purity", this.purity);
		nbt.putDouble("Stability", this.stability);
		nbt.putDouble("Deviation", this.deviation);
		nbt.putString("Body", this.bodyId);
		nbt.putString("Manual", this.manualId);
		nbt.putString("DominantQi", this.dominantQi.name());
		nbt.putInt("MeditationTicks", this.meditationTicks);
		nbt.putInt("BreakthroughCooldown", this.breakthroughCooldown);
		nbt.putInt("TechniqueCooldown", this.techniqueCooldown);
		nbt.putString("ActiveTechnique", this.activeTechniqueId);
		nbt.putString("PreparedResearch", this.preparedResearchId);
		nbt.putString("QiFocus", this.qiFocuses.stream().map(QiFocus::name).reduce((left, right) -> left + "," + right).orElse(""));
		ListTag researches = new ListTag();
		for (String knownResearch : this.knownResearches) {
			researches.add(StringTag.valueOf(knownResearch));
		}
		nbt.put("KnownResearches", researches);
		ListTag insights = new ListTag();
		for (String knownInsight : this.knownInsights) {
			insights.add(StringTag.valueOf(knownInsight));
		}
		nbt.put("KnownInsights", insights);
		ListTag techniques = new ListTag();
		for (String knownTechnique : this.knownTechniques) {
			techniques.add(StringTag.valueOf(knownTechnique));
		}
		nbt.put("KnownTechniques", techniques);
		return nbt;
	}

	public void fromNbt(CompoundTag nbt) {
		this.stage = CultivationStage.valueOf(nbt.getString("Stage").orElse(CultivationStage.MORTAL.name()));
		this.maxQi = this.stage.qiCapacity();
		this.currentQi = Math.min(nbt.getInt("CurrentQi").orElse(0), this.maxQi);
		this.purity = nbt.getDouble("Purity").orElse(0.35D);
		this.stability = nbt.getDouble("Stability").orElse(0.55D);
		this.deviation = nbt.getDouble("Deviation").orElse(0.0D);
		this.bodyId = nbt.getString("Body").orElse(BodyRegistry.NONE_ID);
		this.manualId = nbt.getString("Manual").orElse(ManualRegistry.NONE_ID);
		this.dominantQi = QiType.valueOf(nbt.getString("DominantQi").orElse(QiType.SPIRIT.name()));
		this.meditationTicks = nbt.getInt("MeditationTicks").orElse(0);
		this.breakthroughCooldown = nbt.getInt("BreakthroughCooldown").orElse(0);
		this.techniqueCooldown = nbt.getInt("TechniqueCooldown").orElse(0);
		this.activeTechniqueId = nbt.getString("ActiveTechnique").orElse("none");
		this.preparedResearchId = nbt.getString("PreparedResearch").orElse("");
		this.qiFocuses.clear();
		readQiFocusCsv(nbt.getString("QiFocus").orElse(""));
		this.knownResearches.clear();
		ListTag list = nbt.getList("KnownResearches").orElseGet(ListTag::new);
		for (int i = 0; i < list.size(); i++) {
			this.knownResearches.add(list.getString(i).orElse(""));
		}
		this.knownInsights.clear();
		ListTag insights = nbt.getList("KnownInsights").orElseGet(ListTag::new);
		for (int i = 0; i < insights.size(); i++) {
			this.knownInsights.add(insights.getString(i).orElse(""));
		}
		this.knownTechniques.clear();
		ListTag techniques = nbt.getList("KnownTechniques").orElseGet(ListTag::new);
		for (int i = 0; i < techniques.size(); i++) {
			this.knownTechniques.add(techniques.getString(i).orElse(""));
		}
		if (!this.knownTechniques.contains(this.activeTechniqueId)) {
			this.activeTechniqueId = this.knownTechniques.stream().findFirst().orElse("none");
		}
	}

	public void copyFrom(CultivationData other) {
		fromNbt(other.toNbt());
	}

	public void writeTo(ValueOutput output) {
		output.putString("Stage", this.stage.name());
		output.putInt("CurrentQi", this.currentQi);
		output.putDouble("Purity", this.purity);
		output.putDouble("Stability", this.stability);
		output.putDouble("Deviation", this.deviation);
		output.putString("Body", this.bodyId);
		output.putString("Manual", this.manualId);
		output.putString("DominantQi", this.dominantQi.name());
		output.putInt("MeditationTicks", this.meditationTicks);
		output.putInt("BreakthroughCooldown", this.breakthroughCooldown);
		output.putInt("TechniqueCooldown", this.techniqueCooldown);
		output.putString("ActiveTechnique", this.activeTechniqueId);
		output.putString("PreparedResearch", this.preparedResearchId);
		output.putString("QiFocus", this.qiFocuses.stream().map(QiFocus::name).reduce((left, right) -> left + "," + right).orElse(""));
		output.putString("KnownResearches", String.join(",", this.knownResearches));
		output.putString("KnownInsights", String.join(",", this.knownInsights));
		output.putString("KnownTechniques", String.join(",", this.knownTechniques));
	}

	public void readFrom(ValueInput input) {
		this.stage = CultivationStage.valueOf(input.getStringOr("Stage", CultivationStage.MORTAL.name()));
		this.maxQi = this.stage.qiCapacity();
		this.currentQi = Math.min(input.getIntOr("CurrentQi", 0), this.maxQi);
		this.purity = input.getDoubleOr("Purity", 0.35D);
		this.stability = input.getDoubleOr("Stability", 0.55D);
		this.deviation = input.getDoubleOr("Deviation", 0.0D);
		this.bodyId = input.getStringOr("Body", BodyRegistry.NONE_ID);
		this.manualId = input.getStringOr("Manual", ManualRegistry.NONE_ID);
		this.dominantQi = QiType.valueOf(input.getStringOr("DominantQi", QiType.SPIRIT.name()));
		this.meditationTicks = input.getIntOr("MeditationTicks", 0);
		this.breakthroughCooldown = input.getIntOr("BreakthroughCooldown", 0);
		this.techniqueCooldown = input.getIntOr("TechniqueCooldown", 0);
		this.activeTechniqueId = input.getStringOr("ActiveTechnique", "none");
		this.preparedResearchId = input.getStringOr("PreparedResearch", "");
		this.qiFocuses.clear();
		readQiFocusCsv(input.getStringOr("QiFocus", ""));
		this.knownResearches.clear();
		String researchCsv = input.getStringOr("KnownResearches", "");
		if (!researchCsv.isBlank()) {
			for (String research : researchCsv.split(",")) {
				if (!research.isBlank()) {
					this.knownResearches.add(research);
				}
			}
		}
		this.knownInsights.clear();
		String insightCsv = input.getStringOr("KnownInsights", "");
		if (!insightCsv.isBlank()) {
			for (String insight : insightCsv.split(",")) {
				if (!insight.isBlank()) {
					this.knownInsights.add(insight);
				}
			}
		}
		this.knownTechniques.clear();
		String techniqueCsv = input.getStringOr("KnownTechniques", "");
		if (!techniqueCsv.isBlank()) {
			for (String technique : techniqueCsv.split(",")) {
				if (!technique.isBlank()) {
					this.knownTechniques.add(technique);
				}
			}
		}
		if (!this.knownTechniques.contains(this.activeTechniqueId)) {
			this.activeTechniqueId = this.knownTechniques.stream().findFirst().orElse("none");
		}
	}

	private static double clamp(double value) {
		return Math.max(0.0D, Math.min(1.0D, value));
	}

	private void readQiFocusCsv(String csv) {
		if (csv == null || csv.isBlank()) {
			return;
		}
		for (String token : csv.split(",")) {
			QiFocus focus = QiFocus.byId(token.trim());
			if (focus != QiFocus.NONE) {
				this.qiFocuses.add(focus);
			}
		}
	}
}
