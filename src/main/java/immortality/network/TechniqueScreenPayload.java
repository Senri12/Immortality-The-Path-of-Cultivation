package immortality.network;

import immortality.Immortality;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TechniqueScreenPayload(CompoundTag data) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<TechniqueScreenPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Immortality.MOD_ID, "technique_screen"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TechniqueScreenPayload> CODEC = StreamCodec.of(
		(RegistryFriendlyByteBuf buffer, TechniqueScreenPayload payload) -> buffer.writeNbt(payload.data()),
		(RegistryFriendlyByteBuf buffer) -> new TechniqueScreenPayload(Objects.requireNonNull(buffer.readNbt()))
	);

	public TechniqueScreenPayload {
		Objects.requireNonNull(data, "data");
	}

	@Override
	public Type<TechniqueScreenPayload> type() {
		return TYPE;
	}
}
