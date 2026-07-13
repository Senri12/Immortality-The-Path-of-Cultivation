package immortality.network;

import immortality.Immortality;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record QiFocusActionPayload(String focusId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<QiFocusActionPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Immortality.MOD_ID, "qi_focus_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, QiFocusActionPayload> CODEC = StreamCodec.of(
		(RegistryFriendlyByteBuf buffer, QiFocusActionPayload payload) -> buffer.writeUtf(payload.focusId(), 64),
		(RegistryFriendlyByteBuf buffer) -> new QiFocusActionPayload(buffer.readUtf(64))
	);

	public QiFocusActionPayload {
		Objects.requireNonNull(focusId, "focusId");
	}

	@Override
	public Type<QiFocusActionPayload> type() {
		return TYPE;
	}
}
