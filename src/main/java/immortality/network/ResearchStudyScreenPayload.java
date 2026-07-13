package immortality.network;

import immortality.Immortality;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ResearchStudyScreenPayload(CompoundTag data) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ResearchStudyScreenPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Immortality.MOD_ID, "research_study_screen"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ResearchStudyScreenPayload> CODEC = StreamCodec.of(
		(RegistryFriendlyByteBuf buffer, ResearchStudyScreenPayload payload) -> buffer.writeNbt(payload.data()),
		(RegistryFriendlyByteBuf buffer) -> new ResearchStudyScreenPayload(Objects.requireNonNull(buffer.readNbt()))
	);

	public ResearchStudyScreenPayload {
		Objects.requireNonNull(data, "data");
	}

	@Override
	public Type<ResearchStudyScreenPayload> type() {
		return TYPE;
	}
}
