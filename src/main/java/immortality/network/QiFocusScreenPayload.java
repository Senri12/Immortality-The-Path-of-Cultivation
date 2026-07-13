package immortality.network;

import immortality.Immortality;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record QiFocusScreenPayload(CompoundTag data) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<QiFocusScreenPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Immortality.MOD_ID, "qi_focus_screen"));
	public static final StreamCodec<RegistryFriendlyByteBuf, QiFocusScreenPayload> CODEC = StreamCodec.of(
		(RegistryFriendlyByteBuf buffer, QiFocusScreenPayload payload) -> buffer.writeNbt(payload.data()),
		(RegistryFriendlyByteBuf buffer) -> new QiFocusScreenPayload(Objects.requireNonNull(buffer.readNbt()))
	);

	public QiFocusScreenPayload {
		Objects.requireNonNull(data, "data");
	}

	@Override
	public Type<QiFocusScreenPayload> type() {
		return TYPE;
	}
}
