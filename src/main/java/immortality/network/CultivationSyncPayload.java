package immortality.network;

import immortality.Immortality;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CultivationSyncPayload(CompoundTag data) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<CultivationSyncPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Immortality.MOD_ID, "cultivation_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CultivationSyncPayload> CODEC = StreamCodec.of(
		(RegistryFriendlyByteBuf buffer, CultivationSyncPayload payload) -> buffer.writeNbt(payload.data()),
		(RegistryFriendlyByteBuf buffer) -> new CultivationSyncPayload(Objects.requireNonNull(buffer.readNbt()))
	);

	public CultivationSyncPayload {
		Objects.requireNonNull(data, "data");
	}

	@Override
	public Type<CultivationSyncPayload> type() {
		return TYPE;
	}
}
