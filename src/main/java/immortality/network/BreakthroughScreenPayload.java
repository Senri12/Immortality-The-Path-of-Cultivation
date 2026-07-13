package immortality.network;

import immortality.Immortality;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BreakthroughScreenPayload(CompoundTag data) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<BreakthroughScreenPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Immortality.MOD_ID, "breakthrough_screen"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BreakthroughScreenPayload> CODEC = StreamCodec.of(
		(RegistryFriendlyByteBuf buffer, BreakthroughScreenPayload payload) -> buffer.writeNbt(payload.data()),
		(RegistryFriendlyByteBuf buffer) -> new BreakthroughScreenPayload(Objects.requireNonNull(buffer.readNbt()))
	);

	public BreakthroughScreenPayload {
		Objects.requireNonNull(data, "data");
	}

	@Override
	public Type<BreakthroughScreenPayload> type() {
		return TYPE;
	}
}
