package immortality.entity;

import immortality.Immortality;
import immortality.cultivation.CultivationData;
import immortality.cultivation.CultivationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

public class TribulationLordEntity extends Monster {

	private final ServerBossEvent bossEvent = new ServerBossEvent(
		Component.translatable("entity.immortality.tribulation_lord"),
		BossEvent.BossBarColor.BLUE,
		BossEvent.BossBarOverlay.PROGRESS
	);

	private int lightningTimer = 0;

	public TribulationLordEntity(EntityType<? extends Monster> entityType, Level level) {
		super(entityType, level);
		this.xpReward = 150;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 300.0D)
			.add(Attributes.MOVEMENT_SPEED, 0.28D)
			.add(Attributes.ATTACK_DAMAGE, 14.0D)
			.add(Attributes.ARMOR, 10.0D)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.8D)
			.add(Attributes.FOLLOW_RANGE, 64.0D);
	}

	public static boolean checkSpawnRules(
		EntityType<TribulationLordEntity> type,
		ServerLevelAccessor level,
		net.minecraft.world.entity.EntitySpawnReason spawnReason,
		BlockPos pos,
		net.minecraft.util.RandomSource random
	) {
		return level.getBlockState(pos.below()).isSolid();
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true));
		this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
		this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0F));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

		// Lightning strike skill every 6 seconds
		if (this.getTarget() != null && this.isAlive()) {
			this.lightningTimer++;
			if (this.lightningTimer >= 120) {
				this.lightningTimer = 0;
				LivingEntity target = this.getTarget();
				LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				if (bolt != null) {
					bolt.setPos(target.getX(), target.getY(), target.getZ());
					level.addFreshEntity(bolt);
				}
			}
		}
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		this.bossEvent.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		this.bossEvent.removePlayer(player);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (!isQiDamage(source)) {
			// Qi Barrier Shield Block sound & Particles
			level.playSound(null, this.blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.HOSTILE, 1.5F, 0.8F);
			level.playSound(null, this.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.HOSTILE, 0.8F, 1.8F);

			for (int i = 0; i < 20; i++) {
				Vec3 pos = this.position().add((this.random.nextDouble() - 0.5D) * 1.5D, this.random.nextDouble() * 2.0D, (this.random.nextDouble() - 0.5D) * 1.5D);
				level.sendParticles(ParticleTypes.ENCHANTED_HIT, pos.x, pos.y, pos.z, 2, 0.1D, 0.1D, 0.1D, 0.2D);
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 2, 0.1D, 0.1D, 0.1D, 0.2D);
			}

			if (source.getEntity() instanceof ServerPlayer player) {
				player.sendSystemMessage(Component.translatable("message.immortality.boss.qi_barrier_repel"));
				// Reflect 20% damage back
				player.hurtServer(level, this.damageSources().thorns(this), amount * 0.2F);
			}

			return false; // CANCEL ALL NON-QI DAMAGE!
		}

		return super.hurtServer(level, source, amount);
	}

	private boolean isQiDamage(DamageSource source) {
		if (source.is(DamageTypeTags.BYPASSES_ARMOR) || source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.IS_FIRE)) {
			return true;
		}
		if (source.getEntity() instanceof ServerPlayer player) {
			CultivationData data = CultivationManager.get(player);
			if (data.stage().tier() > 0 || data.currentQi() > 0) {
				return true;
			}
			ItemStack weapon = player.getMainHandItem();
			if (!weapon.isEmpty()) {
				if (weapon.has(Immortality.SPIRITUAL_BLUEPRINT)) {
					return true;
				}
				if (BuiltInRegistries.ITEM.getKey(weapon.getItem()).getNamespace().equals(Immortality.MOD_ID)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENDER_DRAGON_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource damageSource) {
		return SoundEvents.ENDER_DRAGON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENDER_DRAGON_DEATH;
	}
}
