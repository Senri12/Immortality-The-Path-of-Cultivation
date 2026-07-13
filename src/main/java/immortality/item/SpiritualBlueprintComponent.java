package immortality.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import java.util.List;

public record SpiritualBlueprintComponent(List<ModifierInstance> modifiers, int flags, int currentQi, int maxQi) {
	public static final Codec<SpiritualBlueprintComponent> CODEC = RecordCodecBuilder.create(instance ->
		instance.group(
			ModifierInstance.CODEC.listOf().fieldOf("modifiers").forGetter(SpiritualBlueprintComponent::modifiers),
			Codec.INT.fieldOf("flags").forGetter(SpiritualBlueprintComponent::flags),
			Codec.INT.fieldOf("currentQi").forGetter(SpiritualBlueprintComponent::currentQi),
			Codec.INT.fieldOf("maxQi").forGetter(SpiritualBlueprintComponent::maxQi)
		).apply(instance, SpiritualBlueprintComponent::new)
	);

	public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SpiritualBlueprintComponent> STREAM_CODEC = StreamCodec.composite(
		ModifierInstance.STREAM_CODEC.apply(ByteBufCodecs.list()), SpiritualBlueprintComponent::modifiers,
		ByteBufCodecs.VAR_INT, SpiritualBlueprintComponent::flags,
		ByteBufCodecs.VAR_INT, SpiritualBlueprintComponent::currentQi,
		ByteBufCodecs.VAR_INT, SpiritualBlueprintComponent::maxQi,
		SpiritualBlueprintComponent::new
	);

	// Flag constants
	public static final int TEMPERED = 0x01;
	public static final int RESONANT = 0x02;
	public static final int STABILIZED = 0x04;
	public static final int SOULBOUND = 0x08;
	public static final int CORRUPTED = 0x10;
	public static final int MUTATED = 0x20;
	public static final int VOID_TOUCH = 0x40;

	public boolean hasFlag(int flag) {
		return (this.flags & flag) != 0;
	}

	public int getModifierLevel(String modifierId) {
		for (ModifierInstance modifier : modifiers) {
			if (modifier.id().equals(modifierId)) {
				return modifier.level();
			}
		}
		return 0;
	}
}
