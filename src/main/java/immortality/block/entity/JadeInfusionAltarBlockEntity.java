package immortality.block.entity;

import immortality.Immortality;
import immortality.item.SpiritualBlueprintComponent;
import immortality.cultivation.CultivationManager;
import immortality.cultivation.CultivationData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class JadeInfusionAltarBlockEntity extends BlockEntity {
	private ItemStack item = ItemStack.EMPTY;
	private int craftingProgress = 0;
	private UUID ritualPlayerUuid = null;
	private final List<BlockPos> activePedestals = new ArrayList<>();
	private InfusionRecipe activeRecipe = null;

	public JadeInfusionAltarBlockEntity(BlockPos pos, BlockState state) {
		super(Immortality.JADE_INFUSION_ALTAR_ENTITY, pos, state);
	}

	public ItemStack getItem() {
		return this.item;
	}

	public void setItem(ItemStack stack) {
		this.item = stack;
		this.setChanged();
		if (this.level != null && !this.level.isClientSide()) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
		}
	}

	public boolean isCrafting() {
		return this.craftingProgress > 0;
	}

	public void tryStartRitual(ServerPlayer player) {
		if (this.level == null || this.level.isClientSide() || this.craftingProgress > 0) {
			return;
		}

		if (this.item.isEmpty()) {
			player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.immortality.altar.empty_altar"));
			return;
		}

		// 1. Scan for pedestals and collect items
		List<BlockPos> pedestals = scanPedestals();
		List<ItemStack> pedestalItems = new ArrayList<>();
		for (BlockPos pos : pedestals) {
			BlockEntity be = this.level.getBlockEntity(pos);
			if (be instanceof JadePedestalBlockEntity pedestal) {
				if (!pedestal.getItem().isEmpty()) {
					pedestalItems.add(pedestal.getItem());
				}
			}
		}

		// 2. Find matching recipe
		InfusionRecipe matched = null;
		for (InfusionRecipe recipe : RECIPES) {
			if (recipe.matches(this.item, pedestalItems)) {
				matched = recipe;
				break;
			}
		}

		if (matched == null) {
			player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.immortality.altar.invalid_recipe"));
			return;
		}

		// 3. Check Stage & Qi cost
		CultivationData data = CultivationManager.get(player);
		if (data.stage().tier() < matched.minStage.tier()) {
			player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.immortality.altar.insufficient_stage", matched.minStage.displayNameComponent()));
			return;
		}
		if (data.currentQi() < matched.qiCost) {
			player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.immortality.altar.insufficient_qi", matched.qiCost, data.currentQi()));
			return;
		}

		// 4. Start ritual
		this.activeRecipe = matched;
		this.ritualPlayerUuid = player.getUUID();
		this.activePedestals.clear();
		for (BlockPos pos : pedestals) {
			BlockEntity be = this.level.getBlockEntity(pos);
			if (be instanceof JadePedestalBlockEntity pedestal) {
				if (!pedestal.getItem().isEmpty()) {
					this.activePedestals.add(pos);
				}
			}
		}
		this.craftingProgress = 1;
		this.setChanged();
		this.level.playSound(null, this.worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
	}

	private List<BlockPos> scanPedestals() {
		List<BlockPos> list = new ArrayList<>();
		if (this.level == null) return list;
		
		int r = 4;
		BlockPos center = this.worldPosition;
		for (int x = -r; x <= r; x++) {
			for (int z = -r; z <= z + 1 && z <= r; z++) {
				for (int y = -1; y <= 1; y++) {
					BlockPos target = center.offset(x, y, z);
					if (target.equals(center)) continue;
					if (this.level.getBlockState(target).is(Immortality.JADE_PEDESTAL)) {
						list.add(target);
					}
				}
			}
		}
		return list;
	}

	private boolean verifyRecipeItems(ServerLevel level) {
		if (this.activeRecipe == null || this.item.isEmpty()) {
			return false;
		}
		
		List<ItemStack> pedestalItems = new ArrayList<>();
		for (BlockPos pos : this.activePedestals) {
			BlockEntity be = level.getBlockEntity(pos);
			if (!(be instanceof JadePedestalBlockEntity pedestal) || pedestal.getItem().isEmpty()) {
				return false;
			}
			pedestalItems.add(pedestal.getItem());
		}

		return this.activeRecipe.matches(this.item, pedestalItems);
	}

	private void cancelRitual(ServerLevel level) {
		this.craftingProgress = 0;
		this.activeRecipe = null;
		this.ritualPlayerUuid = null;
		this.activePedestals.clear();
		this.setChanged();
		level.playSound(null, this.worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 0.8F);
	}

	private void spawnBeamParticles(ServerLevel level, BlockPos pedestalPos) {
		double startX = pedestalPos.getX() + 0.5D;
		double startY = pedestalPos.getY() + 1.2D;
		double startZ = pedestalPos.getZ() + 0.5D;

		double endX = this.worldPosition.getX() + 0.5D;
		double endY = this.worldPosition.getY() + 1.2D;
		double endZ = this.worldPosition.getZ() + 0.5D;

		double dx = endX - startX;
		double dy = endY - startY;
		double dz = endZ - startZ;

		int steps = 8;
		for (int i = 0; i <= steps; i++) {
			double ratio = (double) i / steps;
			double px = startX + dx * ratio;
			double py = startY + dy * ratio;
			double pz = startZ + dz * ratio;

			level.sendParticles(
				ParticleTypes.ENCHANT,
				px, py, pz,
				1, 0, 0, 0, 0
			);
		}
	}

	private void completeRitual(ServerLevel level) {
		ServerPlayer player = level.getServer().getPlayerList().getPlayer(this.ritualPlayerUuid);
		if (player == null || player.level() != level || player.distanceToSqr(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D, this.worldPosition.getZ() + 0.5D) > 225.0D) {
			cancelRitual(level);
			return;
		}

		CultivationData data = CultivationManager.get(player);
		if (data.currentQi() < this.activeRecipe.qiCost) {
			cancelRitual(level);
			return;
		}

		// 1. Deduct Qi
		data.setCurrentQi(data.currentQi() - this.activeRecipe.qiCost);
		CultivationManager.sync(player);

		// 2. Consume pedestal items
		for (BlockPos pos : this.activePedestals) {
			BlockEntity be = level.getBlockEntity(pos);
			if (be instanceof JadePedestalBlockEntity pedestal) {
				pedestal.setItem(ItemStack.EMPTY);
			}
		}

		// 3. Transform central item
		ItemStack result = this.activeRecipe.resultFactory.apply(this.item, level);
		setItem(result);

		// 4. Finish
		this.craftingProgress = 0;
		this.activeRecipe = null;
		this.ritualPlayerUuid = null;
		this.activePedestals.clear();
		this.setChanged();

		level.playSound(null, this.worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 2.0F, 1.0F);
		level.playSound(null, this.worldPosition, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.BLOCKS, 1.0F, 0.8F);
		
		if (level instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(
				ParticleTypes.EXPLOSION,
				this.worldPosition.getX() + 0.5D,
				this.worldPosition.getY() + 1.2D,
				this.worldPosition.getZ() + 0.5D,
				10, 0.1D, 0.1D, 0.1D, 0.05D
			);
		}
	}

	public static void tick(Level level, BlockPos pos, BlockState state, JadeInfusionAltarBlockEntity altar) {
		if (level.isClientSide()) {
			return;
		}
		altar.tickServer((ServerLevel) level);
	}

	public void tickServer(ServerLevel level) {
		if (this.craftingProgress > 0) {
			this.craftingProgress++;
			
			if (!verifyRecipeItems(level)) {
				cancelRitual(level);
				return;
			}

			for (BlockPos pedestalPos : this.activePedestals) {
				spawnBeamParticles(level, pedestalPos);
			}

			if (this.craftingProgress >= 60) { // 3 seconds
				completeRitual(level);
			}
		}
	}

	@Override
	protected void loadAdditional(net.minecraft.world.level.storage.ValueInput tag) {
		super.loadAdditional(tag);
		this.item = tag.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
		this.craftingProgress = tag.getIntOr("Progress", 0);
		this.ritualPlayerUuid = tag.getString("RitualPlayer").map(UUID::fromString).orElse(null);
	}

	@Override
	protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput tag) {
		super.saveAdditional(tag);
		if (!this.item.isEmpty()) {
			tag.store("Item", ItemStack.CODEC, this.item);
		}
		tag.putInt("Progress", this.craftingProgress);
		if (this.ritualPlayerUuid != null) {
			tag.putString("RitualPlayer", this.ritualPlayerUuid.toString());
		}
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (this.level != null && !this.level.isClientSide()) {
			if (!this.item.isEmpty()) {
				net.minecraft.world.Containers.dropItemStack(this.level, pos.getX(), pos.getY(), pos.getZ(), this.item);
			}
		}
		super.preRemoveSideEffects(pos, state);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}

	// Recipe Class
	public static class InfusionRecipe {
		public final net.minecraft.world.item.Item centralItem;
		public final List<net.minecraft.world.item.Item> pedestalItems;
		public final BiFunction<ItemStack, Level, ItemStack> resultFactory;
		public final int qiCost;
		public final immortality.cultivation.CultivationStage minStage;
		public final Predicate<ItemStack> centralPredicate;

		public InfusionRecipe(net.minecraft.world.item.Item centralItem, List<net.minecraft.world.item.Item> pedestalItems, immortality.cultivation.CultivationStage minStage, int qiCost, Predicate<ItemStack> centralPredicate, BiFunction<ItemStack, Level, ItemStack> resultFactory) {
			this.centralItem = centralItem;
			this.pedestalItems = pedestalItems;
			this.minStage = minStage;
			this.qiCost = qiCost;
			this.centralPredicate = centralPredicate;
			this.resultFactory = resultFactory;
		}

		public boolean matches(ItemStack central, List<ItemStack> pedestals) {
			if (this.centralItem != null && !central.is(this.centralItem)) {
				return false;
			}
			if (this.centralPredicate != null && !this.centralPredicate.test(central)) {
				return false;
			}

			List<net.minecraft.world.item.Item> remaining = new ArrayList<>(this.pedestalItems);
			for (ItemStack stack : pedestals) {
				if (stack.isEmpty()) continue;
				boolean matched = false;
				for (int i = 0; i < remaining.size(); i++) {
					if (stack.is(remaining.get(i))) {
						remaining.remove(i);
						matched = true;
						break;
					}
				}
				if (!matched) {
					return false;
				}
			}
			return remaining.isEmpty();
		}
	}

	private static final List<InfusionRecipe> RECIPES = new ArrayList<>();

	static {
		Predicate<ItemStack> isEquipment = stack -> !stack.isEmpty() && (stack.isDamageableItem() || stack.getMaxStackSize() == 1);

		// Recipe 1: Universal Tempered Equipment
		RECIPES.add(new InfusionRecipe(
			null,
			Arrays.asList(
				net.minecraft.world.item.Item.byBlock(Immortality.SPIRIT_STONE),
				net.minecraft.world.item.Item.byBlock(Immortality.SPIRIT_STONE),
				net.minecraft.world.item.Item.byBlock(Immortality.SPIRIT_STONE),
				net.minecraft.world.item.Item.byBlock(Immortality.SPIRIT_STONE)
			),
			immortality.cultivation.CultivationStage.QI_GATHERING,
			100,
			isEquipment,
			(central, level) -> {
				ItemStack result = central.copy();
				SpiritualBlueprintComponent current = result.get(Immortality.SPIRITUAL_BLUEPRINT);
				List<immortality.item.ModifierInstance> mods = current != null ? new ArrayList<>(current.modifiers()) : new ArrayList<>();
				result.set(Immortality.SPIRITUAL_BLUEPRINT, new SpiritualBlueprintComponent(
					mods,
					SpiritualBlueprintComponent.TEMPERED,
					100, 100
				));
				return result;
			}
		));

		// Recipe 2: Ignis Flame Infusion (Any weapon/equipment)
		RECIPES.add(new InfusionRecipe(
			null,
			Arrays.asList(
				Immortality.FLAME_BEAST_CORE,
				Immortality.IMMORTALS_JADE
			),
			immortality.cultivation.CultivationStage.FOUNDATION_ESTABLISHMENT,
			200,
			isEquipment,
			(central, level) -> {
				ItemStack result = central.copy();
				SpiritualBlueprintComponent current = result.get(Immortality.SPIRITUAL_BLUEPRINT);
				List<immortality.item.ModifierInstance> mods = current != null ? new ArrayList<>(current.modifiers()) : new ArrayList<>();
				mods.removeIf(m -> m.id().equals("ignis"));
				mods.add(new immortality.item.ModifierInstance("ignis", 2));
				int flags = current != null ? current.flags() : SpiritualBlueprintComponent.TEMPERED;
				result.set(Immortality.SPIRITUAL_BLUEPRINT, new SpiritualBlueprintComponent(
					mods, flags, 100, 100
				));
				return result;
			}
		));

		// Recipe 3: Vigor Life Infusion (Any armor/equipment)
		RECIPES.add(new InfusionRecipe(
			null,
			Arrays.asList(
				Immortality.EARTH_BEAST_CORE,
				Immortality.IMMORTALS_JADE,
				Immortality.IMMORTALS_JADE
			),
			immortality.cultivation.CultivationStage.FOUNDATION_ESTABLISHMENT,
			300,
			isEquipment,
			(central, level) -> {
				ItemStack result = central.copy();
				SpiritualBlueprintComponent current = result.get(Immortality.SPIRITUAL_BLUEPRINT);
				List<immortality.item.ModifierInstance> mods = current != null ? new ArrayList<>(current.modifiers()) : new ArrayList<>();
				mods.removeIf(m -> m.id().equals("vigor"));
				mods.add(new immortality.item.ModifierInstance("vigor", 3));
				int flags = current != null ? current.flags() : SpiritualBlueprintComponent.TEMPERED;
				result.set(Immortality.SPIRITUAL_BLUEPRINT, new SpiritualBlueprintComponent(
					mods, flags, 100, 100
				));
				return result;
			}
		));

		// Recipe 4: Formation Compass
		RECIPES.add(new InfusionRecipe(
			net.minecraft.world.item.Items.COMPASS,
			Arrays.asList(
				Immortality.IMMORTALS_JADE,
				Immortality.IMMORTALS_JADE,
				Immortality.IMMORTALS_JADE,
				Immortality.IMMORTALS_JADE
			),
			immortality.cultivation.CultivationStage.QI_GATHERING,
			150,
			stack -> true,
			(central, level) -> new ItemStack(Immortality.FORMATION_COMPASS)
		));

		// Recipe 5: Bamboo Flag
		RECIPES.add(new InfusionRecipe(
			net.minecraft.world.item.Items.STICK,
			Arrays.asList(
				net.minecraft.world.item.Items.PAPER,
				net.minecraft.world.item.Items.PAPER,
				net.minecraft.world.item.Item.byBlock(Immortality.SPIRIT_STONE)
			),
			immortality.cultivation.CultivationStage.MORTAL,
			50,
			stack -> true,
			(central, level) -> new ItemStack(Immortality.BAMBOO_FLAG)
		));

		// Recipe 6: Jade Flag
		RECIPES.add(new InfusionRecipe(
			net.minecraft.world.item.Item.byBlock(Immortality.BAMBOO_FLAG),
			Arrays.asList(
				Immortality.IMMORTALS_JADE,
				Immortality.IMMORTALS_JADE,
				net.minecraft.world.item.Item.byBlock(Immortality.SPIRIT_STONE)
			),
			immortality.cultivation.CultivationStage.QI_GATHERING,
			250,
			stack -> true,
			(central, level) -> new ItemStack(Immortality.JADE_FLAG)
		));

		// Recipe 7: Formation Core
		RECIPES.add(new InfusionRecipe(
			net.minecraft.world.item.Item.byBlock(Immortality.JADE_BLOCK),
			Arrays.asList(
				Immortality.IMMORTALS_JADE,
				Immortality.IMMORTALS_JADE,
				Immortality.IMMORTALS_JADE,
				Immortality.IMMORTALS_JADE,
				net.minecraft.world.item.Items.DIAMOND
			),
			immortality.cultivation.CultivationStage.FOUNDATION_ESTABLISHMENT,
			400,
			stack -> true,
			(central, level) -> new ItemStack(Immortality.FORMATION_CORE)
		));

		// Recipe 8: Spirit Convergence Rune
		RECIPES.add(new InfusionRecipe(
			net.minecraft.world.item.Items.STONE,
			Arrays.asList(
				Immortality.SPIRIT_BEAST_CORE
			),
			immortality.cultivation.CultivationStage.QI_GATHERING,
			100,
			stack -> true,
			(central, level) -> new ItemStack(Immortality.SPIRIT_CONVERGENCE_RUNE)
		));

		// Recipe 9: Taiji Shield Rune
		RECIPES.add(new InfusionRecipe(
			net.minecraft.world.item.Items.STONE,
			Arrays.asList(
				Immortality.EARTH_BEAST_CORE,
				net.minecraft.world.item.Items.SHIELD
			),
			immortality.cultivation.CultivationStage.QI_GATHERING,
			150,
			stack -> true,
			(central, level) -> new ItemStack(Immortality.TAIJI_SHIELD_RUNE)
		));

		// Recipe 10: Mirage Concealment Rune
		RECIPES.add(new InfusionRecipe(
			net.minecraft.world.item.Items.STONE,
			Arrays.asList(
				Immortality.SPIRIT_BEAST_CORE,
				net.minecraft.world.item.Items.FERMENTED_SPIDER_EYE
			),
			immortality.cultivation.CultivationStage.QI_GATHERING,
			150,
			stack -> true,
			(central, level) -> new ItemStack(Immortality.MIRAGE_CONCEALMENT_RUNE)
		));

		// Recipe 11: Sword Forest Rune
		RECIPES.add(new InfusionRecipe(
			net.minecraft.world.item.Items.STONE,
			Arrays.asList(
				Immortality.FLAME_BEAST_CORE,
				net.minecraft.world.item.Items.IRON_SWORD
			),
			immortality.cultivation.CultivationStage.FOUNDATION_ESTABLISHMENT,
			200,
			stack -> true,
			(central, level) -> new ItemStack(Immortality.SWORD_FOREST_RUNE)
		));

		// Recipe 12: Heavenly Lightning Rune
		RECIPES.add(new InfusionRecipe(
			net.minecraft.world.item.Items.STONE,
			Arrays.asList(
				Immortality.LIGHTNING_BEAST_CORE,
				net.minecraft.world.item.Item.byBlock(Immortality.SPIRIT_STONE)
			),
			immortality.cultivation.CultivationStage.FOUNDATION_ESTABLISHMENT,
			250,
			stack -> true,
			(central, level) -> new ItemStack(Immortality.HEAVENLY_LIGHTNING_RUNE)
		));

		// Recipe 13: Frost Domain Rune
		RECIPES.add(new InfusionRecipe(
			net.minecraft.world.item.Items.STONE,
			Arrays.asList(
				Immortality.FROST_BEAST_CORE,
				net.minecraft.world.item.Items.ICE
			),
			immortality.cultivation.CultivationStage.FOUNDATION_ESTABLISHMENT,
			250,
			stack -> true,
			(central, level) -> new ItemStack(Immortality.FROST_DOMAIN_RUNE)
		));

		// Recipe 14: Life Spring Rune
		RECIPES.add(new InfusionRecipe(
			net.minecraft.world.item.Items.STONE,
			Arrays.asList(
				Immortality.SPIRIT_GRASS,
				Immortality.SPIRIT_BEAST_CORE
			),
			immortality.cultivation.CultivationStage.QI_GATHERING,
			120,
			stack -> true,
			(central, level) -> new ItemStack(Immortality.LIFE_SPRING_RUNE)
		));

		// Recipe 15: Gravity Suppression Rune
		RECIPES.add(new InfusionRecipe(
			net.minecraft.world.item.Items.STONE,
			Arrays.asList(
				Immortality.EARTH_BEAST_CORE,
				Immortality.DRAGON_VEIN_STONE
			),
			immortality.cultivation.CultivationStage.CORE_FORMATION,
			350,
			stack -> true,
			(central, level) -> new ItemStack(Immortality.GRAVITY_SUPPRESSION_RUNE)
		));

		// Recipe 16: Flame Lotus Rune
		RECIPES.add(new InfusionRecipe(
			net.minecraft.world.item.Items.STONE,
			Arrays.asList(
				Immortality.FLAME_BEAST_CORE,
				Immortality.PHOENIX_FEATHER
			),
			immortality.cultivation.CultivationStage.CORE_FORMATION,
			350,
			stack -> true,
			(central, level) -> new ItemStack(Immortality.FLAME_LOTUS_RUNE)
		));

		// Recipe 17: Qi Sealing Rune
		RECIPES.add(new InfusionRecipe(
			net.minecraft.world.item.Items.STONE,
			Arrays.asList(
				Immortality.IMMORTALS_JADE,
				Immortality.SPIRIT_BEAST_CORE,
				Immortality.DRAGON_VEIN_STONE
			),
			immortality.cultivation.CultivationStage.CORE_FORMATION,
			400,
			stack -> true,
			(central, level) -> new ItemStack(Immortality.QI_SEALING_RUNE)
		));

		// Recipe 18: Electrum Lightning Infusion (Any weapon/armor)
		RECIPES.add(new InfusionRecipe(
			null,
			Arrays.asList(
				Immortality.LIGHTNING_BEAST_CORE,
				Immortality.HEAVENLY_IRON
			),
			immortality.cultivation.CultivationStage.CORE_FORMATION,
			300,
			isEquipment,
			(central, level) -> {
				ItemStack result = central.copy();
				SpiritualBlueprintComponent current = result.get(Immortality.SPIRITUAL_BLUEPRINT);
				List<immortality.item.ModifierInstance> mods = current != null ? new ArrayList<>(current.modifiers()) : new ArrayList<>();
				mods.removeIf(m -> m.id().equals("electrum"));
				mods.add(new immortality.item.ModifierInstance("electrum", 3));
				int flags = current != null ? current.flags() : SpiritualBlueprintComponent.TEMPERED;
				result.set(Immortality.SPIRITUAL_BLUEPRINT, new SpiritualBlueprintComponent(
					mods, flags, 120, 120
				));
				return result;
			}
		));

		// Recipe 19: Unyielding Fortitude Infusion (Any armor/shield)
		RECIPES.add(new InfusionRecipe(
			null,
			Arrays.asList(
				Immortality.DRAGON_VEIN_STONE,
				Immortality.HEAVENLY_IRON,
				Immortality.IMMORTALS_JADE
			),
			immortality.cultivation.CultivationStage.NASCENT_SOUL,
			450,
			isEquipment,
			(central, level) -> {
				ItemStack result = central.copy();
				SpiritualBlueprintComponent current = result.get(Immortality.SPIRITUAL_BLUEPRINT);
				List<immortality.item.ModifierInstance> mods = current != null ? new ArrayList<>(current.modifiers()) : new ArrayList<>();
				mods.removeIf(m -> m.id().equals("unyielding"));
				mods.add(new immortality.item.ModifierInstance("unyielding", 3));
				int flags = current != null ? current.flags() : SpiritualBlueprintComponent.TEMPERED;
				result.set(Immortality.SPIRITUAL_BLUEPRINT, new SpiritualBlueprintComponent(
					mods, flags, 150, 150
				));
				return result;
			}
		));

		// Recipe 20: Lightning Talisman Infusion
		RECIPES.add(new InfusionRecipe(
			net.minecraft.world.item.Items.PAPER,
			Arrays.asList(
				Immortality.LIGHTNING_BEAST_CORE,
				net.minecraft.world.item.Item.byBlock(Immortality.SPIRIT_STONE),
				net.minecraft.world.item.Item.byBlock(Immortality.SPIRIT_STONE)
			),
			immortality.cultivation.CultivationStage.QI_GATHERING,
			100,
			stack -> true,
			(central, level) -> new ItemStack(Immortality.LIGHTNING_TALISMAN)
		));

		// Recipe 21: Golden Core Pill Infusion
		RECIPES.add(new InfusionRecipe(
			Immortality.FOUNDATION_PILL,
			Arrays.asList(
				Immortality.SPIRIT_GRASS,
				Immortality.FLAME_BEAST_CORE,
				Immortality.IMMORTALS_JADE
			),
			immortality.cultivation.CultivationStage.FOUNDATION_ESTABLISHMENT,
			250,
			stack -> true,
			(central, level) -> new ItemStack(Immortality.GOLDEN_CORE_PILL)
		));
	}
}
