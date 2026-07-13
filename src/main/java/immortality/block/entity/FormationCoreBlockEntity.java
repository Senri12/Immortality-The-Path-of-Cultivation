package immortality.block.entity;

import immortality.Immortality;
import immortality.block.FormationFlagBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FormationCoreBlockEntity extends BlockEntity {
	private ItemStack rune = ItemStack.EMPTY;
	private final List<BlockPos> activeFlags = new ArrayList<>();
	private boolean active = false;
	private UUID ownerUuid = null;
	private int tickCounter = 0;

	public FormationCoreBlockEntity(BlockPos pos, BlockState state) {
		super(Immortality.FORMATION_CORE_ENTITY, pos, state);
	}

	public ItemStack getRune() {
		return this.rune;
	}

	public void setRune(ItemStack rune) {
		this.rune = rune;
		this.setChanged();
		if (this.level != null && !this.level.isClientSide()) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
		}
	}

	public boolean bindFlags(List<BlockPos> flags, Player player) {
		if (this.level == null) return false;
		
		for (BlockPos flagPos : flags) {
			BlockEntity be = this.level.getBlockEntity(flagPos);
			if (be instanceof FormationFlagBlockEntity flagBE) {
				double dist = Math.sqrt(this.worldPosition.distSqr(flagPos));
				if (dist > flagBE.getRange()) {
					player.displayClientMessage(Component.translatable("message.immortality.core.flag_too_far", flagPos.getX(), flagPos.getY(), flagPos.getZ(), flagBE.getRange()), false);
					return false;
				}
			} else {
				player.displayClientMessage(Component.translatable("message.immortality.core.invalid_flag_at", flagPos.getX(), flagPos.getY(), flagPos.getZ()), false);
				return false;
			}
		}

		this.activeFlags.clear();
		this.activeFlags.addAll(flags);
		this.ownerUuid = player.getUUID();
		this.active = true;
		this.setChanged();
		this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
		return true;
	}

	public void displayStatus(Player player) {
		if (this.active) {
			String runeName = this.rune.isEmpty() ? "None" : this.rune.getHoverName().getString();
			player.displayClientMessage(Component.translatable("message.immortality.core.status_active", this.activeFlags.size(), runeName), false);
		} else {
			player.displayClientMessage(Component.translatable("message.immortality.core.status_inactive"), false);
		}
	}

	public static void tick(Level level, BlockPos pos, BlockState state, FormationCoreBlockEntity core) {
		if (level.isClientSide()) {
			return;
		}

		core.tickCounter++;
		if (core.tickCounter >= 20) {
			core.tickCounter = 0;
			core.serverTick((ServerLevel) level);
		}
	}

	private void serverTick(ServerLevel level) {
		if (!this.active || this.activeFlags.isEmpty()) {
			return;
		}

		if (!checkFlagsExist()) {
			collapse(level);
			return;
		}

		if (!checkFlagsCharged()) {
			deactivate(level, "message.immortality.core.flags_depleted");
			return;
		}

		consumeQiFromFlags();

		if (!this.rune.isEmpty()) {
			spawnParticles(level);
			applyArrayEffects(level);
		}
	}

	private boolean checkFlagsExist() {
		for (BlockPos flagPos : this.activeFlags) {
			BlockState state = this.level.getBlockState(flagPos);
			if (!(state.getBlock() instanceof FormationFlagBlock)) {
				return false;
			}
		}
		return true;
	}

	private boolean checkFlagsCharged() {
		for (BlockPos flagPos : this.activeFlags) {
			BlockEntity be = this.level.getBlockEntity(flagPos);
			if (be instanceof FormationFlagBlockEntity flagBE) {
				if (flagBE.getCurrentQi() <= 0) {
					return false;
				}
			}
		}
		return true;
	}

	private void consumeQiFromFlags() {
		for (BlockPos flagPos : this.activeFlags) {
			BlockEntity be = this.level.getBlockEntity(flagPos);
			if (be instanceof FormationFlagBlockEntity flagBE) {
				flagBE.addQi(-2);
			}
		}
	}

	private void spawnParticles(ServerLevel level) {
		ParticleOptions particle = getArrayParticle();
		
		for (int i = 0; i < this.activeFlags.size(); i++) {
			BlockPos start = this.activeFlags.get(i);
			BlockPos end = this.activeFlags.get((i + 1) % this.activeFlags.size());
			spawnLineParticles(level, start, end, particle);
			spawnLineParticles(level, start, this.worldPosition, particle);
		}
	}

	private void spawnLineParticles(ServerLevel level, BlockPos start, BlockPos end, ParticleOptions particle) {
		double dx = end.getX() - start.getX();
		double dy = end.getY() - start.getY();
		double dz = end.getZ() - start.getZ();
		double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
		int steps = (int) (dist * 1.5);
		for (int i = 0; i <= steps; i++) {
			double ratio = (double) i / steps;
			double px = start.getX() + 0.5 + dx * ratio;
			double py = start.getY() + 0.5 + dy * ratio;
			double pz = start.getZ() + 0.5 + dz * ratio;
			level.sendParticles(particle, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	private ParticleOptions getArrayParticle() {
		if (this.rune.is(Immortality.SPIRIT_CONVERGENCE_RUNE)) {
			return ParticleTypes.HAPPY_VILLAGER;
		} else if (this.rune.is(Immortality.TAIJI_SHIELD_RUNE)) {
			return ParticleTypes.END_ROD;
		} else if (this.rune.is(Immortality.MIRAGE_CONCEALMENT_RUNE)) {
			return ParticleTypes.WITCH;
		} else if (this.rune.is(Immortality.SWORD_FOREST_RUNE)) {
			return ParticleTypes.CRIT;
		}
		return ParticleTypes.PORTAL;
	}

	private void applyArrayEffects(ServerLevel level) {
		AABB zone = getZoneAABB();

		if (this.rune.is(Immortality.SPIRIT_CONVERGENCE_RUNE)) {
			List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, zone);
			for (ServerPlayer player : players) {
				immortality.cultivation.CultivationData data = immortality.cultivation.CultivationManager.get(player);
				if (immortality.cultivation.CultivationManager.isMeditating(player)) {
					data.addQi(3);
					player.displayClientMessage(Component.translatable("message.immortality.array.spirit_gain", 3), true);
					immortality.cultivation.CultivationManager.sync(player);
				}
			}
		}
		else if (this.rune.is(Immortality.TAIJI_SHIELD_RUNE)) {
			List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, zone);
			for (Projectile proj : projectiles) {
				proj.setDeltaMovement(proj.getDeltaMovement().scale(-1.2D));
				proj.hurtMarked = true;
				level.playSound((Player) null, proj.blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.BLOCKS, 1.0F, 1.2F);
				level.sendParticles(ParticleTypes.POOF, proj.getX(), proj.getY(), proj.getZ(), 3, 0.0, 0.0, 0.0, 0.0);
			}

			List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, zone, entity -> entity instanceof Enemy);
			Vec3 center = new Vec3(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5);
			for (LivingEntity enemy : entities) {
				Vec3 diff = enemy.position().subtract(center).normalize().scale(0.8D);
				enemy.setDeltaMovement(diff.x, 0.2D, diff.z);
				enemy.hurtMarked = true;
			}
		}
		else if (this.rune.is(Immortality.MIRAGE_CONCEALMENT_RUNE)) {
			List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, zone);
			for (LivingEntity entity : entities) {
				if (entity instanceof Player player) {
					entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, true));
				} else if (entity instanceof Enemy) {
					entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
				}
			}
		}
		else if (this.rune.is(Immortality.SWORD_FOREST_RUNE)) {
			List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, zone, entity -> entity instanceof Enemy);
			for (LivingEntity enemy : enemies) {
				enemy.hurt(level.damageSources().magic(), 8.0F);
				level.playSound((Player) null, enemy.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.BLOCKS, 0.8F, 1.1F);
				level.sendParticles(ParticleTypes.SWEEP_ATTACK, enemy.getX(), enemy.getY() + 0.5, enemy.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
			}
		}
	}

	private AABB getZoneAABB() {
		if (this.activeFlags.isEmpty()) {
			return new AABB(this.worldPosition);
		}
		double minX = this.activeFlags.stream().mapToDouble(BlockPos::getX).min().orElse(0);
		double maxX = this.activeFlags.stream().mapToDouble(BlockPos::getX).max().orElse(0) + 1.0;
		double minY = this.activeFlags.stream().mapToDouble(BlockPos::getY).min().orElse(0);
		double maxY = this.activeFlags.stream().mapToDouble(BlockPos::getY).max().orElse(0) + 3.0;
		double minZ = this.activeFlags.stream().mapToDouble(BlockPos::getZ).min().orElse(0);
		double maxZ = this.activeFlags.stream().mapToDouble(BlockPos::getZ).max().orElse(0) + 1.0;
		return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
	}

	private void deactivate(ServerLevel level, String reasonKey) {
		this.active = false;
		this.activeFlags.clear();
		this.setChanged();
		level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
		
		if (this.ownerUuid != null) {
			ServerPlayer owner = level.getServer().getPlayerList().getPlayer(this.ownerUuid);
			if (owner != null) {
				owner.displayClientMessage(Component.translatable(reasonKey), false);
			}
		}
	}

	private void collapse(ServerLevel level) {
		this.active = false;
		this.activeFlags.clear();
		this.setChanged();
		level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);

		level.explode(null, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5, 3.0F, Level.ExplosionInteraction.TNT);

		AABB zone = getZoneAABB();
		List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, zone);
		for (ServerPlayer player : players) {
			immortality.cultivation.CultivationData data = immortality.cultivation.CultivationManager.get(player);
			data.setCurrentQi(0);
			data.addDeviation(0.50D);
			immortality.cultivation.CultivationManager.sync(player);
			
			player.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
			player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0));
			player.displayClientMessage(Component.translatable("message.immortality.core.backflash"), false);
		}
	}

	@Override
	protected void loadAdditional(ValueInput tag) {
		super.loadAdditional(tag);
		this.rune = tag.read("Rune", ItemStack.CODEC).orElse(ItemStack.EMPTY);
		this.active = tag.getIntOr("Active", 0) == 1;
		this.ownerUuid = tag.getString("Owner").map(UUID::fromString).orElse(null);
		
		this.activeFlags.clear();
		List<BlockPos> list = tag.read("Flags", BlockPos.CODEC.listOf()).orElse(new ArrayList<>());
		this.activeFlags.addAll(list);
	}

	@Override
	protected void saveAdditional(ValueOutput tag) {
		super.saveAdditional(tag);
		if (!this.rune.isEmpty()) {
			tag.store("Rune", ItemStack.CODEC, this.rune);
		}
		tag.putInt("Active", this.active ? 1 : 0);
		if (this.ownerUuid != null) {
			tag.putString("Owner", this.ownerUuid.toString());
		}
		if (!this.activeFlags.isEmpty()) {
			tag.store("Flags", BlockPos.CODEC.listOf(), this.activeFlags);
		}
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}
}
