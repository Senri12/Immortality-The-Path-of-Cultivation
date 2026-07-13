package immortality.mixin;

import immortality.Immortality;
import immortality.item.SpiritualBlueprintComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@Unique
	private boolean immortality$isBroken() {
		ItemStack stack = (ItemStack) (Object) this;
		if (stack.isDamageableItem()) {
			SpiritualBlueprintComponent blueprint = stack.get(Immortality.SPIRITUAL_BLUEPRINT);
			if (blueprint != null && blueprint.hasFlag(SpiritualBlueprintComponent.TEMPERED)) {
				return stack.getDamageValue() >= stack.getMaxDamage() - 1;
			}
		}
		return false;
	}

	@Inject(method = "setDamageValue", at = @At("HEAD"), cancellable = true)
	private void immortality$preventBreak(int damage, CallbackInfo ci) {
		ItemStack stack = (ItemStack) (Object) this;
		if (stack.isDamageableItem()) {
			int maxDamage = stack.getMaxDamage();
			if (damage >= maxDamage) {
				SpiritualBlueprintComponent blueprint = stack.get(Immortality.SPIRITUAL_BLUEPRINT);
				if (blueprint != null && blueprint.hasFlag(SpiritualBlueprintComponent.TEMPERED)) {
					if (stack.getDamageValue() < maxDamage - 1) {
						stack.setDamageValue(maxDamage - 1);
					}
					ci.cancel();
				}
			}
		}
	}

	@Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
	private void immortality$getDestroySpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
		if (immortality$isBroken()) {
			cir.setReturnValue(1.0F); // Hand speed
		}
	}

	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void immortality$use(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		if (immortality$isBroken()) {
			cir.setReturnValue(InteractionResult.PASS);
		}
	}

	@Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
	private void immortality$useOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (immortality$isBroken()) {
			cir.setReturnValue(InteractionResult.PASS);
		}
	}
}
