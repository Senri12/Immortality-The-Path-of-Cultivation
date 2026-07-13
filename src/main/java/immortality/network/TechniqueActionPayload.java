package immortality.network;

import immortality.Immortality;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TechniqueActionPayload(String action, String techniqueId) implements CustomPacketPayload {
	public static final String ACTION_CYCLE_NEXT = "cycle_next";
	public static final String ACTION_CLEAR = "clear";
	public static final String ACTION_SET = "set";
	public static final String ACTION_INVOKE = "invoke";

	public static final CustomPacketPayload.Type<TechniqueActionPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Immortality.MOD_ID, "technique_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TechniqueActionPayload> CODEC = StreamCodec.of(
		(RegistryFriendlyByteBuf buffer, TechniqueActionPayload payload) -> {
			buffer.writeUtf(payload.action(), 32);
			buffer.writeUtf(payload.techniqueId(), 128);
		},
		(RegistryFriendlyByteBuf buffer) -> new TechniqueActionPayload(buffer.readUtf(32), buffer.readUtf(128))
	);

	public TechniqueActionPayload {
		Objects.requireNonNull(action, "action");
		Objects.requireNonNull(techniqueId, "techniqueId");
	}

	@Override
	public Type<TechniqueActionPayload> type() {
		return TYPE;
	}
}
