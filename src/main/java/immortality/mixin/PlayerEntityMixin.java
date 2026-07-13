package immortality.mixin;

import immortality.cultivation.CultivationData;
import immortality.cultivation.CultivatorAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerEntityMixin implements CultivatorAccess {
	@Unique
	private final CultivationData immortality$cultivationData = new CultivationData();

	@Override
	public CultivationData immortality$getCultivationData() {
		return this.immortality$cultivationData;
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void immortality$readCustomData(ValueInput input, CallbackInfo ci) {
		this.immortality$cultivationData.readFrom(input.childOrEmpty("ImmortalityCultivation"));
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void immortality$writeCustomData(ValueOutput output, CallbackInfo ci) {
		this.immortality$cultivationData.writeTo(output.child("ImmortalityCultivation"));
	}
}
