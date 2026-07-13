package immortality.network;

import immortality.Immortality;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ManualActionPayload(String action, String manualId) implements CustomPacketPayload {
	public static final String ACTION_COMPREHEND = "comprehend";

	public static final CustomPacketPayload.Type<ManualActionPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Immortality.MOD_ID, "manual_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ManualActionPayload> CODEC = StreamCodec.of(
		(RegistryFriendlyByteBuf buffer, ManualActionPayload payload) -> {
			buffer.writeUtf(payload.action(), 32);
			buffer.writeUtf(payload.manualId(), 128);
		},
		(RegistryFriendlyByteBuf buffer) -> new ManualActionPayload(buffer.readUtf(32), buffer.readUtf(128))
	);

	public ManualActionPayload {
		Objects.requireNonNull(action, "action");
		Objects.requireNonNull(manualId, "manualId");
	}

	@Override
	public Type<ManualActionPayload> type() {
		return TYPE;
	}
}
