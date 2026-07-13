package immortality.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ModifierInstance(String id, int level) {
	public static final Codec<ModifierInstance> CODEC = RecordCodecBuilder.create(instance ->
		instance.group(
			Codec.STRING.fieldOf("id").forGetter(ModifierInstance::id),
			Codec.INT.fieldOf("level").forGetter(ModifierInstance::level)
		).apply(instance, ModifierInstance::new)
	);

	public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, ModifierInstance> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.stringUtf8(256), ModifierInstance::id,
		ByteBufCodecs.VAR_INT, ModifierInstance::level,
		ModifierInstance::new
	);
}
