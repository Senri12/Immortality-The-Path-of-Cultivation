package immortality.network;

import immortality.Immortality;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ResearchLinkPayload(String researchId, List<Placement> placements) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ResearchLinkPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Immortality.MOD_ID, "research_link"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ResearchLinkPayload> CODEC = StreamCodec.of(
		(RegistryFriendlyByteBuf buffer, ResearchLinkPayload payload) -> {
			buffer.writeUtf(payload.researchId(), 128);
			buffer.writeInt(payload.placements().size());
			for (Placement placement : payload.placements()) {
				buffer.writeInt(placement.x());
				buffer.writeInt(placement.y());
				buffer.writeUtf(placement.aspectId(), 64);
			}
		},
		(RegistryFriendlyByteBuf buffer) -> {
			String researchId = buffer.readUtf(128);
			int size = buffer.readInt();
			List<Placement> placements = new java.util.ArrayList<>();
			for (int i = 0; i < size; i++) {
				placements.add(new Placement(buffer.readInt(), buffer.readInt(), buffer.readUtf(64)));
			}
			return new ResearchLinkPayload(researchId, placements);
		}
	);

	public ResearchLinkPayload {
		Objects.requireNonNull(researchId, "researchId");
		Objects.requireNonNull(placements, "placements");
	}

	@Override
	public Type<ResearchLinkPayload> type() {
		return TYPE;
	}

	public record Placement(int x, int y, String aspectId) {
		public Placement {
			Objects.requireNonNull(aspectId, "aspectId");
		}
	}
}
