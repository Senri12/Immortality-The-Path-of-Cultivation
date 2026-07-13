package immortality.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import java.util.List;

public record FormationCompassComponent(List<BlockPos> flags) {
	public static final Codec<FormationCompassComponent> CODEC = RecordCodecBuilder.create(instance ->
		instance.group(
			BlockPos.CODEC.listOf().fieldOf("flags").forGetter(FormationCompassComponent::flags)
		).apply(instance, FormationCompassComponent::new)
	);

	public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, FormationCompassComponent> STREAM_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), FormationCompassComponent::flags,
		FormationCompassComponent::new
	);
}
