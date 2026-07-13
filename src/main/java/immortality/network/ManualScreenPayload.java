package immortality.network;

import immortality.Immortality;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ManualScreenPayload(CompoundTag data) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ManualScreenPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Immortality.MOD_ID, "manual_screen"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ManualScreenPayload> CODEC = StreamCodec.of(
		(RegistryFriendlyByteBuf buffer, ManualScreenPayload payload) -> buffer.writeNbt(payload.data()),
		(RegistryFriendlyByteBuf buffer) -> new ManualScreenPayload(Objects.requireNonNull(buffer.readNbt()))
	);

	public ManualScreenPayload {
		Objects.requireNonNull(data, "data");
	}

	@Override
	public Type<ManualScreenPayload> type() {
		return TYPE;
	}
}
