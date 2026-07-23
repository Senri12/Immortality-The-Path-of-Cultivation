package immortality.mixin;

import immortality.Immortality;
import immortality.cultivation.CultivationData;
import immortality.cultivation.CultivationManager;
import immortality.item.SpiritualBlueprintComponent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
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
	private void immortality$applySpiritualModifiers(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		LivingEntity target = (LivingEntity) (Object) this;

		// 1. Unyielding & Electrum Armor Effects on target (scaled by target cultivation stage)
		float targetStageMult = 1.0F;
		if (target instanceof net.minecraft.server.level.ServerPlayer targetPlayer) {
			CultivationData data = CultivationManager.get(targetPlayer);
			targetStageMult = 1.0F + (data.stage().tier() * 0.35F);
		}

		for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
			ItemStack armor = target.getItemBySlot(slot);
			if (armor != null && !armor.isEmpty()) {
				SpiritualBlueprintComponent blueprint = armor.get(Immortality.SPIRITUAL_BLUEPRINT);
				if (blueprint != null) {
					int unyieldingLvl = blueprint.getModifierLevel("unyielding");
					if (unyieldingLvl > 0 && target.getHealth() - amount <= target.getMaxHealth() * 0.35F) {
						if (!target.hasEffect(MobEffects.ABSORPTION)) {
							int amp = (int) (unyieldingLvl * targetStageMult);
							target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, (int) (160 * targetStageMult), amp));
							target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, (int) (160 * targetStageMult), 1));
							target.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, (int) (160 * targetStageMult), 1));
							level.playSound(null, target.blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 1.5F, 0.6F);
							level.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY() + 1.0, target.getZ(), 25, 0.4, 0.4, 0.4, 0.1);
						}
					}
					int electrumLvl = blueprint.getModifierLevel("electrum");
					if (electrumLvl > 0 && source.getEntity() instanceof LivingEntity attacker) {
						attacker.hurtServer(level, target.damageSources().lightningBolt(), electrumLvl * 2.5F * targetStageMult);
						level.sendParticles(ParticleTypes.ELECTRIC_SPARK, attacker.getX(), attacker.getY() + 1.0, attacker.getZ(), 10, 0.2, 0.2, 0.2, 0.1);
					}
				}
			}
		}

		if (immortality$inBrokenAttack.get()) {
			return;
		}

		// 2. Attacker Weapon Modifiers (scaled by attacker cultivation stage)
		if (source.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
			CultivationData attackerData = CultivationManager.get(serverPlayer);
			float attackerStageMult = 1.0F + (attackerData.stage().tier() * 0.35F);

			ItemStack weapon = serverPlayer.getMainHandItem();
			if (weapon != null && !weapon.isEmpty()) {
				SpiritualBlueprintComponent blueprint = weapon.get(Immortality.SPIRITUAL_BLUEPRINT);
				if (blueprint != null) {
					boolean broken = weapon.isDamageableItem() && weapon.getDamageValue() >= weapon.getMaxDamage() - 1 && blueprint.hasFlag(SpiritualBlueprintComponent.TEMPERED);
					if (broken) {
						immortality$inBrokenAttack.set(true);
						try {
							boolean result = target.hurtServer(level, source, 1.0F); // Reduced broken damage
							cir.setReturnValue(result);
						} finally {
							immortality$inBrokenAttack.set(false);
						}
						return;
					}

					// Ignis Flame Modifier
					int ignisLvl = blueprint.getModifierLevel("ignis");
					if (ignisLvl > 0) {
						target.igniteForSeconds(ignisLvl * 4.0F * attackerStageMult);
						target.hurtServer(level, target.damageSources().onFire(), ignisLvl * 2.5F * attackerStageMult);
						level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 1.0, target.getZ(), 12, 0.3, 0.3, 0.3, 0.05);
					}

					// Electrum Lightning Modifier
					int electrumLvl = blueprint.getModifierLevel("electrum");
					if (electrumLvl > 0) {
						LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
						if (bolt != null) {
							bolt.setPos(target.getX(), target.getY(), target.getZ());
							level.addFreshEntity(bolt);
						}
						target.hurtServer(level, target.damageSources().lightningBolt(), electrumLvl * 3.0F * attackerStageMult);
						level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.0, target.getZ(), 20, 0.5, 0.5, 0.5, 0.15);
					}

					// Sharp True Damage Modifier
					int sharpLvl = blueprint.getModifierLevel("sharp");
					if (sharpLvl > 0) {
						target.hurtServer(level, target.damageSources().magic(), sharpLvl * 3.5F * attackerStageMult);
						level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(), 15, 0.3, 0.3, 0.3, 0.1);
					}
				}
			}
		}
	}
}
