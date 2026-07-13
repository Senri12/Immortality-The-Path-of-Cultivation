package immortality.mixin;

import immortality.Immortality;
import immortality.item.SpiritualBlueprintComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Unique
	private static final ThreadLocal<Boolean> immortality$inBrokenAttack = ThreadLocal.withInitial(() -> false);

	@Inject(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
	private void immortality$applyIgnisAndBrokenModifier(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if (immortality$inBrokenAttack.get()) {
			return;
		}

		if (source.getEntity() instanceof Player player) {
			ItemStack stack = player.getMainHandItem();
			if (stack != null && !stack.isEmpty()) {
				SpiritualBlueprintComponent blueprint = stack.get(Immortality.SPIRITUAL_BLUEPRINT);
				if (blueprint != null) {
					boolean broken = stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() - 1 && blueprint.hasFlag(SpiritualBlueprintComponent.TEMPERED);
					if (broken) {
						immortality$inBrokenAttack.set(true);
						try {
							LivingEntity target = (LivingEntity) (Object) this;
							boolean result = target.hurtServer(level, source, 1.0F); // Override damage to fist damage (1.0F)
							cir.setReturnValue(result);
						} finally {
							immortality$inBrokenAttack.set(false);
						}
					} else {
						int ignisLvl = blueprint.getModifierLevel("ignis");
						if (ignisLvl > 0) {
							LivingEntity target = (LivingEntity) (Object) this;
							target.igniteForSeconds(ignisLvl * 3);
							target.hurtServer(level, target.damageSources().onFire(), ignisLvl * 2.0F);
						}
					}
				}
			}
		}
	}
}
