package immortality.network;

import immortality.Immortality;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record AltarResearchScreenPayload(CompoundTag data) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<AltarResearchScreenPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Immortality.MOD_ID, "altar_research_screen"));
	public static final StreamCodec<RegistryFriendlyByteBuf, AltarResearchScreenPayload> CODEC = StreamCodec.of(
		(RegistryFriendlyByteBuf buffer, AltarResearchScreenPayload payload) -> buffer.writeNbt(payload.data()),
		(RegistryFriendlyByteBuf buffer) -> new AltarResearchScreenPayload(Objects.requireNonNull(buffer.readNbt()))
	);

	public AltarResearchScreenPayload {
		Objects.requireNonNull(data, "data");
	}

	@Override
	public Type<AltarResearchScreenPayload> type() {
		return TYPE;
	}
}
