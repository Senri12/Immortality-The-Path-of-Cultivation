package immortality.client;

import immortality.cultivation.CultivationData;
import net.minecraft.nbt.CompoundTag;

public final class ClientCultivationState {
	private static final CultivationData DATA = new CultivationData();

	private ClientCultivationState() {
	}

	public static CultivationData get() {
		return DATA;
	}

	public static void apply(CompoundTag tag) {
		DATA.fromNbt(tag);
	}

	public static void reset() {
		DATA.fromNbt(new CompoundTag());
	}
}
