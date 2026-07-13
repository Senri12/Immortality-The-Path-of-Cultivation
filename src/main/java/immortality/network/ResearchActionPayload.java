package immortality.network;

import immortality.Immortality;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ResearchActionPayload(String action, String researchId) implements CustomPacketPayload {
	public static final String ACTION_STUDY = "study";
	public static final String ACTION_PREPARE = "prepare";
	public static final String ACTION_BREAKTHROUGH = "breakthrough";
	public static final String ACTION_OPEN_BREAKTHROUGH = "open_breakthrough";
	public static final String ACTION_OPEN_RESEARCH = "open_research";
	public static final String ACTION_OPEN_TECHNIQUES = "open_techniques";
	public static final String ACTION_OPEN_EFFECTS = "open_effects";

	public static final CustomPacketPayload.Type<ResearchActionPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Immortality.MOD_ID, "research_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ResearchActionPayload> CODEC = StreamCodec.of(
		(RegistryFriendlyByteBuf buffer, ResearchActionPayload payload) -> {
			buffer.writeUtf(payload.action(), 32);
			buffer.writeUtf(payload.researchId(), 128);
		},
		(RegistryFriendlyByteBuf buffer) -> new ResearchActionPayload(buffer.readUtf(32), buffer.readUtf(128))
	);

	public ResearchActionPayload {
		Objects.requireNonNull(action, "action");
		Objects.requireNonNull(researchId, "researchId");
	}

	@Override
	public Type<ResearchActionPayload> type() {
		return TYPE;
	}
}
