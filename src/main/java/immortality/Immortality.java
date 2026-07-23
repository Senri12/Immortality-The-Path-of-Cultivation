package immortality;

import immortality.beast.BeastCoreDropService;
import immortality.beast.BeastCoreRegistry;
import immortality.block.EnlightenmentAltarBlock;
import immortality.block.ImmortalPortalBlock;
import immortality.block.MeditationMatBlock;
import immortality.block.ResearchStudyBlock;
import immortality.block.SimpleStairBlock;
import immortality.block.JadePedestalBlock;
import immortality.block.JadeInfusionAltarBlock;
import immortality.cultivation.BodyRegistry;
import immortality.command.CultivationCommands;
import immortality.cultivation.CultivationManager;
import immortality.cultivation.QiAspectRegistry;
import immortality.cultivation.ResearchRegistry;
import immortality.cultivation.StudyBoardRegistry;
import immortality.dimension.ImmortalPortalManager;
import immortality.manual.ManualItem;
import immortality.manual.ManualRegistry;
import immortality.technique.TechniqueRegistry;
import immortality.network.AltarResearchScreenPayload;
import immortality.network.BreakthroughScreenPayload;
import immortality.network.CultivationSyncPayload;
import immortality.network.ManualActionPayload;
import immortality.network.ManualScreenPayload;
import immortality.network.QiFocusActionPayload;
import immortality.network.QiFocusScreenPayload;
import immortality.network.ResearchActionPayload;
import immortality.network.ResearchLinkPayload;
import immortality.network.ResearchStudyScreenPayload;
import immortality.network.TechniqueActionPayload;
import immortality.network.TechniqueScreenPayload;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Immortality implements ModInitializer {
	public static final String MOD_ID = "immortality";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final net.minecraft.core.component.DataComponentType<immortality.item.SpiritualBlueprintComponent> SPIRITUAL_BLUEPRINT = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		id("spiritual_blueprint"),
		net.minecraft.core.component.DataComponentType.<immortality.item.SpiritualBlueprintComponent>builder()
			.persistent(immortality.item.SpiritualBlueprintComponent.CODEC)
			.networkSynchronized(immortality.item.SpiritualBlueprintComponent.STREAM_CODEC)
			.build()
	);
 
 	public static final net.minecraft.core.component.DataComponentType<immortality.item.FormationCompassComponent> FORMATION_FLAGS = Registry.register(
 		BuiltInRegistries.DATA_COMPONENT_TYPE,
 		id("formation_flags"),
 		net.minecraft.core.component.DataComponentType.<immortality.item.FormationCompassComponent>builder()
 			.persistent(immortality.item.FormationCompassComponent.CODEC)
 			.networkSynchronized(immortality.item.FormationCompassComponent.STREAM_CODEC)
 			.build()
 	);

	public static final Item SPIRIT_BEAST_CORE = registerItem("spirit_beast_core", new Item(itemProperties("spirit_beast_core")));
	public static final Item FLAME_BEAST_CORE = registerItem("flame_beast_core", new Item(itemProperties("flame_beast_core")));
	public static final Item EARTH_BEAST_CORE = registerItem("earth_beast_core", new Item(itemProperties("earth_beast_core")));
	public static final Item LIGHTNING_BEAST_CORE = registerItem("lightning_beast_core", new Item(itemProperties("lightning_beast_core")));
	public static final Item FROST_BEAST_CORE = registerItem("frost_beast_core", new Item(itemProperties("frost_beast_core")));
	public static final Item SPIRIT_GRASS = registerItem("spirit_grass", new Item(itemProperties("spirit_grass")));
	public static final Item DRAGON_VEIN_STONE = registerItem("dragon_vein_stone", new Item(itemProperties("dragon_vein_stone")));
	public static final Item HEAVENLY_IRON = registerItem("heavenly_iron", new Item(itemProperties("heavenly_iron")));
	public static final Item FOUNDATION_PILL = registerItem("foundation_pill", new immortality.item.SpiritPillItem(50, 0.10D, 0.10D, itemProperties("foundation_pill")));
	public static final Item GOLDEN_CORE_PILL = registerItem("golden_core_pill", new immortality.item.SpiritPillItem(120, 0.15D, 0.20D, itemProperties("golden_core_pill")));
	public static final Item PHOENIX_FEATHER = registerItem("phoenix_feather", new Item(itemProperties("phoenix_feather")));
	public static final Item LIGHTNING_TALISMAN = registerItem("lightning_talisman", new Item(itemProperties("lightning_talisman")));
	public static final Item WANDERING_CLOUD_MANUAL = registerItem("wandering_cloud_manual", new ManualItem("wandering_cloud_manual", itemProperties("wandering_cloud_manual")));
	public static final Item CRIMSON_FLAME_MANUAL = registerItem("crimson_flame_manual", new ManualItem("crimson_flame_manual", itemProperties("crimson_flame_manual")));
	public static final Item STONE_BODY_MANUAL = registerItem("stone_body_manual", new ManualItem("stone_body_manual", itemProperties("stone_body_manual")));
	public static final Item OMNISCIENCE_MANUAL = registerItem("omniscience_manual", new ManualItem("omniscience_manual", itemProperties("omniscience_manual")));
	public static final Item NINE_DRAGONS_MANUAL = registerItem("nine_dragons_manual", new ManualItem("nine_dragons_manual", itemProperties("nine_dragons_manual")));
	public static final Item IMMORTALS_JADE = registerItem("immortals_jade", new Item(itemProperties("immortals_jade")));
	
	public static final Item FORMATION_COMPASS = registerItem("formation_compass", new immortality.item.FormationCompassItem(itemProperties("formation_compass").stacksTo(1)));
	public static final Item SPIRIT_CONVERGENCE_RUNE = registerItem("spirit_convergence_rune", new Item(itemProperties("spirit_convergence_rune")));
	public static final Item TAIJI_SHIELD_RUNE = registerItem("taiji_shield_rune", new Item(itemProperties("taiji_shield_rune")));
	public static final Item MIRAGE_CONCEALMENT_RUNE = registerItem("mirage_concealment_rune", new Item(itemProperties("mirage_concealment_rune")));
	public static final Item SWORD_FOREST_RUNE = registerItem("sword_forest_rune", new Item(itemProperties("sword_forest_rune")));
	public static final Item HEAVENLY_LIGHTNING_RUNE = registerItem("heavenly_lightning_rune", new Item(itemProperties("heavenly_lightning_rune")));
	public static final Item FROST_DOMAIN_RUNE = registerItem("frost_domain_rune", new Item(itemProperties("frost_domain_rune")));
	public static final Item LIFE_SPRING_RUNE = registerItem("life_spring_rune", new Item(itemProperties("life_spring_rune")));
	public static final Item GRAVITY_SUPPRESSION_RUNE = registerItem("gravity_suppression_rune", new Item(itemProperties("gravity_suppression_rune")));
	public static final Item FLAME_LOTUS_RUNE = registerItem("flame_lotus_rune", new Item(itemProperties("flame_lotus_rune")));
	public static final Item QI_SEALING_RUNE = registerItem("qi_sealing_rune", new Item(itemProperties("qi_sealing_rune")));

	public static final EntityType<immortality.entity.SpiritBeastEntity> SPIRIT_BEAST = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		id("spirit_beast"),
		EntityType.Builder.of(immortality.entity.SpiritBeastEntity::new, net.minecraft.world.entity.MobCategory.MONSTER)
			.sized(0.9F, 1.2F)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, id("spirit_beast")))
	);

	public static final EntityType<immortality.entity.TribulationLordEntity> TRIBULATION_LORD = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		id("tribulation_lord"),
		EntityType.Builder.of(immortality.entity.TribulationLordEntity::new, net.minecraft.world.entity.MobCategory.MONSTER)
			.sized(1.4F, 2.2F)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, id("tribulation_lord")))
	);

	public static final EntityType<immortality.entity.FlameSalamanderEntity> FLAME_SALAMANDER = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		id("flame_salamander"),
		EntityType.Builder.of(immortality.entity.FlameSalamanderEntity::new, net.minecraft.world.entity.MobCategory.MONSTER)
			.sized(0.8F, 0.7F)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, id("flame_salamander")))
	);

	public static final EntityType<immortality.entity.FrostFoxEntity> FROST_FOX = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		id("frost_fox"),
		EntityType.Builder.of(immortality.entity.FrostFoxEntity::new, net.minecraft.world.entity.MobCategory.MONSTER)
			.sized(0.7F, 0.7F)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, id("frost_fox")))
	);

	public static final Item SPIRIT_BEAST_SPAWN_EGG = registerItem(
		"spirit_beast_spawn_egg",
		new immortality.item.SpiritBeastSpawnEggItem(itemProperties("spirit_beast_spawn_egg"))
	);

	public static final Item TRIBULATION_LORD_SPAWN_EGG = registerItem(
		"tribulation_lord_spawn_egg",
		new immortality.item.ModSpawnEggItem(() -> TRIBULATION_LORD, itemProperties("tribulation_lord_spawn_egg"))
	);

	public static final Item FLAME_SALAMANDER_SPAWN_EGG = registerItem(
		"flame_salamander_spawn_egg",
		new immortality.item.ModSpawnEggItem(() -> FLAME_SALAMANDER, itemProperties("flame_salamander_spawn_egg"))
	);

	public static final Item FROST_FOX_SPAWN_EGG = registerItem(
		"frost_fox_spawn_egg",
		new immortality.item.ModSpawnEggItem(() -> FROST_FOX, itemProperties("frost_fox_spawn_egg"))
	);

	public static final Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration> ANCIENT_PAGODA_FEATURE = Registry.register(
		BuiltInRegistries.FEATURE,
		id("ancient_pagoda"),
		new immortality.worldgen.feature.AncientPagodaFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC)
	);

	public static final Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration> RUINED_DAO_SHRINE_FEATURE = Registry.register(
		BuiltInRegistries.FEATURE,
		id("ruined_dao_shrine"),
		new immortality.worldgen.feature.RuinedDaoShrineFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC)
	);

	public static final Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration> SPIRIT_VEIN_GROTTO_FEATURE = Registry.register(
		BuiltInRegistries.FEATURE,
		id("spirit_vein_grotto"),
		new immortality.worldgen.feature.SpiritVeinGrottoFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC)
	);

	public static final Block JADE_PEDESTAL = registerBlock(
		"jade_pedestal",
		new JadePedestalBlock(blockProperties("jade_pedestal", BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).strength(2.5F)))
	);

	public static final Block JADE_INFUSION_ALTAR = registerBlock(
		"jade_infusion_altar",
		new JadeInfusionAltarBlock(blockProperties("jade_infusion_altar", BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).strength(3.0F)))
	);

	public static final net.minecraft.world.level.block.entity.BlockEntityType<immortality.block.entity.JadeInfusionAltarBlockEntity> JADE_INFUSION_ALTAR_ENTITY = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		id("jade_infusion_altar"),
		net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
			immortality.block.entity.JadeInfusionAltarBlockEntity::new,
			JADE_INFUSION_ALTAR
		).build()
	);

	public static final net.minecraft.world.level.block.entity.BlockEntityType<immortality.block.entity.JadePedestalBlockEntity> JADE_PEDESTAL_ENTITY = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		id("jade_pedestal"),
		net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
			immortality.block.entity.JadePedestalBlockEntity::new,
			JADE_PEDESTAL
		).build()
	);

	public static final Block SPIRIT_STONE = registerBlock(
		"spirit_stone",
		new Block(blockProperties("spirit_stone", BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE).strength(2.2F, 6.0F)))
	);
	public static final Block JADE_BLOCK = registerBlock(
		"jade_block",
		new Block(blockProperties("jade_block", BlockBehaviour.Properties.ofFullCopy(Blocks.EMERALD_BLOCK).strength(3.0F, 6.0F)))
	);
	public static final Block JADE_PLANKS = registerBlock(
		"jade_planks",
		new Block(blockProperties("jade_planks", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(2.0F, 3.0F)))
	);
	public static final Block JADE_STAIRS = registerBlock(
		"jade_stairs",
		new SimpleStairBlock(JADE_PLANKS.defaultBlockState(), blockProperties("jade_stairs", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_STAIRS).strength(2.0F, 3.0F)))
	);
	public static final Block JADE_SLAB = registerBlock(
		"jade_slab",
		new SlabBlock(blockProperties("jade_slab", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB).strength(2.0F, 3.0F)))
	);
	public static final Block JADE_FENCE = registerBlock(
		"jade_fence",
		new FenceBlock(blockProperties("jade_fence", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE).strength(2.0F, 3.0F)))
	);
	public static final Block IMMORTAL_PORTAL = registerBlock(
		"immortal_portal",
		new ImmortalPortalBlock(
			blockProperties("immortal_portal", BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_PORTAL)
				.noLootTable()
				.noCollision()
				.strength(-1.0F)
				.lightLevel(state -> 11))
		)
	);
	public static final Block MEDITATION_MAT = registerBlock(
		"meditation_mat",
		new MeditationMatBlock(blockProperties("meditation_mat", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(0.8F)))
	);
	public static final Block ENLIGHTENMENT_ALTAR = registerBlock(
		"enlightenment_altar",
		new EnlightenmentAltarBlock(blockProperties("enlightenment_altar", BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).strength(2.5F).requiresCorrectToolForDrops()))
	);
	public static final Block RESEARCH_STUDY = registerBlock(
		"research_study",
		new ResearchStudyBlock(blockProperties("research_study", BlockBehaviour.Properties.ofFullCopy(Blocks.BOOKSHELF).strength(2.0F)))
	);

	public static final Block BAMBOO_FLAG = registerBlock(
		"bamboo_flag",
		new immortality.block.FormationFlagBlock(blockProperties("bamboo_flag", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(1.0F).noOcclusion()))
	);
	public static final Block JADE_FLAG = registerBlock(
		"jade_flag",
		new immortality.block.FormationFlagBlock(blockProperties("jade_flag", BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).strength(2.5F).noOcclusion()))
	);
	public static final Block FORMATION_CORE = registerBlock(
		"formation_core",
		new immortality.block.FormationCoreBlock(blockProperties("formation_core", BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).strength(3.0F)))
	);

	public static final net.minecraft.world.level.levelgen.feature.Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration> PEDESTAL_SHRINE_FEATURE = Registry.register(
		BuiltInRegistries.FEATURE,
		id("pedestal_shrine"),
		new immortality.worldgen.feature.PedestalShrineFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC)
	);

	public static final net.minecraft.world.level.block.entity.BlockEntityType<immortality.block.entity.FormationFlagBlockEntity> FORMATION_FLAG_ENTITY = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		id("formation_flag"),
		net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
			immortality.block.entity.FormationFlagBlockEntity::new,
			BAMBOO_FLAG, JADE_FLAG
		).build()
	);

	public static final net.minecraft.world.level.block.entity.BlockEntityType<immortality.block.entity.FormationCoreBlockEntity> FORMATION_CORE_ENTITY = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		id("formation_core"),
		net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
			immortality.block.entity.FormationCoreBlockEntity::new,
			FORMATION_CORE
		).build()
	);

	public static final CreativeModeTab IMMORTALITY_TAB = Registry.register(
		BuiltInRegistries.CREATIVE_MODE_TAB,
		id("immortality"),
		FabricItemGroup.builder()
			.title(Component.translatable("itemGroup.immortality"))
			.icon(() -> new ItemStack(SPIRIT_BEAST_CORE))
			.displayItems((parameters, output) -> {
				output.accept(SPIRIT_BEAST_CORE);
				output.accept(FLAME_BEAST_CORE);
				output.accept(EARTH_BEAST_CORE);
				output.accept(LIGHTNING_BEAST_CORE);
				output.accept(FROST_BEAST_CORE);
				output.accept(SPIRIT_GRASS);
				output.accept(DRAGON_VEIN_STONE);
				output.accept(HEAVENLY_IRON);
				output.accept(FOUNDATION_PILL);
				output.accept(GOLDEN_CORE_PILL);
				output.accept(PHOENIX_FEATHER);
				output.accept(LIGHTNING_TALISMAN);
				output.accept(IMMORTALS_JADE);
				output.accept(SPIRIT_STONE);
				output.accept(JADE_BLOCK);
				output.accept(JADE_PLANKS);
				output.accept(JADE_STAIRS);
				output.accept(JADE_SLAB);
				output.accept(JADE_FENCE);
				output.accept(JADE_PEDESTAL);
				output.accept(JADE_INFUSION_ALTAR);
				output.accept(WANDERING_CLOUD_MANUAL);
				output.accept(CRIMSON_FLAME_MANUAL);
				output.accept(STONE_BODY_MANUAL);
				output.accept(OMNISCIENCE_MANUAL);
				output.accept(NINE_DRAGONS_MANUAL);
				output.accept(MEDITATION_MAT);
				output.accept(ENLIGHTENMENT_ALTAR);
				output.accept(RESEARCH_STUDY);
				
				output.accept(FORMATION_COMPASS);
				output.accept(BAMBOO_FLAG);
				output.accept(JADE_FLAG);
				output.accept(FORMATION_CORE);
				output.accept(SPIRIT_CONVERGENCE_RUNE);
				output.accept(TAIJI_SHIELD_RUNE);
				output.accept(MIRAGE_CONCEALMENT_RUNE);
				output.accept(SWORD_FOREST_RUNE);
				output.accept(HEAVENLY_LIGHTNING_RUNE);
				output.accept(FROST_DOMAIN_RUNE);
				output.accept(LIFE_SPRING_RUNE);
				output.accept(GRAVITY_SUPPRESSION_RUNE);
				output.accept(FLAME_LOTUS_RUNE);
				output.accept(QI_SEALING_RUNE);
				output.accept(SPIRIT_BEAST_SPAWN_EGG);
				output.accept(TRIBULATION_LORD_SPAWN_EGG);
				output.accept(FLAME_SALAMANDER_SPAWN_EGG);
				output.accept(FROST_FOX_SPAWN_EGG);
			})
			.build()
	);

	@Override
	public void onInitialize() {
		net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(SPIRIT_BEAST, immortality.entity.SpiritBeastEntity.createAttributes());
		net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(TRIBULATION_LORD, immortality.entity.TribulationLordEntity.createAttributes());
		net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(FLAME_SALAMANDER, immortality.entity.FlameSalamanderEntity.createAttributes());
		net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(FROST_FOX, immortality.entity.FrostFoxEntity.createAttributes());
		
		net.minecraft.world.entity.SpawnPlacements.register(
			SPIRIT_BEAST,
			net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
			net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			immortality.entity.SpiritBeastEntity::checkSpiritBeastSpawnRules
		);

		net.minecraft.world.entity.SpawnPlacements.register(
			TRIBULATION_LORD,
			net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
			net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			immortality.entity.TribulationLordEntity::checkSpawnRules
		);

		net.minecraft.world.entity.SpawnPlacements.register(
			FLAME_SALAMANDER,
			net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
			net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			immortality.entity.FlameSalamanderEntity::checkSpawnRules
		);

		net.minecraft.world.entity.SpawnPlacements.register(
			FROST_FOX,
			net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
			net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			immortality.entity.FrostFoxEntity::checkSpawnRules
		);

		net.fabricmc.fabric.api.biome.v1.BiomeModifications.addSpawn(
			net.fabricmc.fabric.api.biome.v1.BiomeSelectors.foundInOverworld(),
			net.minecraft.world.entity.MobCategory.MONSTER,
			SPIRIT_BEAST,
			6,
			1, 2
		);
		net.fabricmc.fabric.api.biome.v1.BiomeModifications.addSpawn(
			net.fabricmc.fabric.api.biome.v1.BiomeSelectors.foundInOverworld(),
			net.minecraft.world.entity.MobCategory.MONSTER,
			EntityType.ZOMBIE,
			95, 2, 4
		);
		net.fabricmc.fabric.api.biome.v1.BiomeModifications.addSpawn(
			net.fabricmc.fabric.api.biome.v1.BiomeSelectors.foundInOverworld(),
			net.minecraft.world.entity.MobCategory.MONSTER,
			EntityType.SKELETON,
			80, 2, 4
		);
		net.fabricmc.fabric.api.biome.v1.BiomeModifications.addSpawn(
			net.fabricmc.fabric.api.biome.v1.BiomeSelectors.foundInOverworld(),
			net.minecraft.world.entity.MobCategory.MONSTER,
			EntityType.SPIDER,
			80, 2, 4
		);

		ResourceKey<net.minecraft.world.level.levelgen.placement.PlacedFeature> pagodaKey = ResourceKey.create(
			Registries.PLACED_FEATURE, id("ancient_pagoda")
		);
		ResourceKey<net.minecraft.world.level.levelgen.placement.PlacedFeature> shrineKey = ResourceKey.create(
			Registries.PLACED_FEATURE, id("ruined_dao_shrine")
		);
		ResourceKey<net.minecraft.world.level.levelgen.placement.PlacedFeature> grottoKey = ResourceKey.create(
			Registries.PLACED_FEATURE, id("spirit_vein_grotto")
		);

		net.fabricmc.fabric.api.biome.v1.BiomeModifications.addFeature(
			net.fabricmc.fabric.api.biome.v1.BiomeSelectors.foundInOverworld(),
			net.minecraft.world.level.levelgen.GenerationStep.Decoration.SURFACE_STRUCTURES,
			pagodaKey
		);
		net.fabricmc.fabric.api.biome.v1.BiomeModifications.addFeature(
			net.fabricmc.fabric.api.biome.v1.BiomeSelectors.foundInOverworld(),
			net.minecraft.world.level.levelgen.GenerationStep.Decoration.SURFACE_STRUCTURES,
			shrineKey
		);
		net.fabricmc.fabric.api.biome.v1.BiomeModifications.addFeature(
			net.fabricmc.fabric.api.biome.v1.BiomeSelectors.foundInOverworld(),
			net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_STRUCTURES,
			grottoKey
		);

		PayloadTypeRegistry.playS2C().register(CultivationSyncPayload.TYPE, CultivationSyncPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(AltarResearchScreenPayload.TYPE, AltarResearchScreenPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(BreakthroughScreenPayload.TYPE, BreakthroughScreenPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ManualScreenPayload.TYPE, ManualScreenPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ResearchStudyScreenPayload.TYPE, ResearchStudyScreenPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(TechniqueScreenPayload.TYPE, TechniqueScreenPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(QiFocusScreenPayload.TYPE, QiFocusScreenPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ResearchActionPayload.TYPE, ResearchActionPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ResearchLinkPayload.TYPE, ResearchLinkPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ManualActionPayload.TYPE, ManualActionPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(TechniqueActionPayload.TYPE, TechniqueActionPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(QiFocusActionPayload.TYPE, QiFocusActionPayload.CODEC);
		BodyRegistry.bootstrap();
		BeastCoreRegistry.bootstrap();
		TechniqueRegistry.bootstrap();
		ManualRegistry.bootstrap();
		ResearchRegistry.bootstrap();
		QiAspectRegistry.bootstrap();
		StudyBoardRegistry.bootstrap();
		immortality.loot.LootTableModifier.init();
		ServerPlayNetworking.registerGlobalReceiver(ResearchActionPayload.TYPE, (payload, context) ->
			context.server().execute(() -> CultivationManager.handleResearchAction(context.player(), payload))
		);
		ServerPlayNetworking.registerGlobalReceiver(ResearchLinkPayload.TYPE, (payload, context) ->
			context.server().execute(() -> CultivationManager.handleResearchLink(context.player(), payload))
		);
		ServerPlayNetworking.registerGlobalReceiver(ManualActionPayload.TYPE, (payload, context) ->
			context.server().execute(() -> CultivationManager.handleManualAction(context.player(), payload))
		);
		ServerPlayNetworking.registerGlobalReceiver(TechniqueActionPayload.TYPE, (payload, context) ->
			context.server().execute(() -> CultivationManager.handleTechniqueAction(context.player(), payload))
		);
		ServerPlayNetworking.registerGlobalReceiver(QiFocusActionPayload.TYPE, (payload, context) ->
			context.server().execute(() -> CultivationManager.handleQiFocusAction(context.player(), payload))
		);
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (!player.getItemInHand(hand).is(Items.FLINT_AND_STEEL)) {
				return InteractionResult.PASS;
			}
			if (world.isClientSide()) {
				return InteractionResult.PASS;
			}
			if (ImmortalPortalManager.tryCreatePortal(world, hitResult.getBlockPos(), hitResult.getDirection())) {
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});

		CommandRegistrationCallback.EVENT.register(CultivationCommands::register);
		ServerLivingEntityEvents.AFTER_DEATH.register(BeastCoreDropService::handleDeath);
		ServerTickEvents.END_SERVER_TICK.register(server ->
			server.getPlayerList().getPlayers().forEach(CultivationManager::tickPlayer)
		);
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
			CultivationManager.sync(newPlayer)
		);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
			CultivationManager.sync(handler.player)
		);
		ServerLifecycleEvents.SERVER_STARTED.register(server -> LOGGER.info("Immortality cultivation systems initialized"));
	}

	private static Item registerItem(String path, Item item) {
		return Registry.register(BuiltInRegistries.ITEM, id(path), item);
	}

	private static Block registerBlock(String path, Block block) {
		Identifier id = id(path);
		Registry.register(BuiltInRegistries.BLOCK, id, block);
		Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, blockItemProperties(path)));
		return block;
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	private static Item.Properties itemProperties(String path) {
		return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(path)));
	}

	private static BlockBehaviour.Properties blockProperties(String path, BlockBehaviour.Properties properties) {
		return properties.setId(ResourceKey.create(Registries.BLOCK, id(path)));
	}

	private static Item.Properties blockItemProperties(String path) {
		return itemProperties(path).useBlockDescriptionPrefix();
	}
}
