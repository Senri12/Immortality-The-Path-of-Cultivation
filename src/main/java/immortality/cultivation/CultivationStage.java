package immortality.cultivation;

import java.util.Locale;
import net.minecraft.network.chat.Component;

public enum CultivationStage {
	MORTAL("Mortal", 30, 0),
	QI_GATHERING("Qi Gathering", 80, 1),
	FOUNDATION_ESTABLISHMENT("Foundation Establishment", 140, 2),
	CORE_FORMATION("Core Formation", 220, 3),
	NASCENT_SOUL("Nascent Soul", 320, 4),
	SPIRIT_SEVERING("Spirit Severing", 450, 5),
	ASCENDANT("Ascendant", 620, 6),
	ILLUSORY_YIN("Illusory Yin", 820, 7),
	CORPOREAL_YANG("Corporeal Yang", 1050, 8),
	NIRVANA_SCRYER("Nirvana Scryer", 1320, 9),
	NIRVANA_CLEANSER("Nirvana Cleanser", 1650, 10),
	VOID_TRIBULANT("Void Tribulant", 2050, 11);

	private final String displayName;
	private final int qiCapacity;
	private final int tier;

	CultivationStage(String displayName, int qiCapacity, int tier) {
		this.displayName = displayName;
		this.qiCapacity = qiCapacity;
		this.tier = tier;
	}

	public String displayName() {
		return this.displayName;
	}

	public String translationKey() {
		return "stage.immortality." + name().toLowerCase(Locale.ROOT);
	}

	public Component displayNameComponent() {
		return Component.translatable(translationKey());
	}

	public int qiCapacity() {
		return this.qiCapacity;
	}

	public int tier() {
		return this.tier;
	}

	public int strengthAmplifier() {
		return this == MORTAL ? -1 : this.tier - 1;
	}

	public int resistanceAmplifier() {
		return this == MORTAL ? -1 : this.tier - 1;
	}

	public int speedAmplifier() {
		return this.tier >= CORE_FORMATION.tier ? Math.max(0, this.tier - CORE_FORMATION.tier) : -1;
	}

	public int jumpAmplifier() {
		return this.tier >= NASCENT_SOUL.tier ? Math.max(0, this.tier - NASCENT_SOUL.tier) : -1;
	}

	public boolean grantsFlight() {
		return this.tier >= ASCENDANT.tier;
	}

	public CultivationStage next() {
		int nextOrdinal = ordinal() + 1;
		return nextOrdinal >= values().length ? this : values()[nextOrdinal];
	}
}
