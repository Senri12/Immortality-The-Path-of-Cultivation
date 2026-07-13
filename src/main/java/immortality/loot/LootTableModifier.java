package immortality.loot;

import immortality.Immortality;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class LootTableModifier {
	public static void init() {
		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			if (source.isBuiltin()) {
				// 1. Dungeons, Mineshafts, Pyramids, Temple (Standard magic elements)
				if (key.equals(BuiltInLootTables.SIMPLE_DUNGEON) || 
					key.equals(BuiltInLootTables.ABANDONED_MINESHAFT) || 
					key.equals(BuiltInLootTables.DESERT_PYRAMID) || 
					key.equals(BuiltInLootTables.JUNGLE_TEMPLE)) {
					
					LootPool.Builder pool = LootPool.lootPool()
						.setRolls(UniformGenerator.between(0.0F, 2.0F))
						.add(LootItem.lootTableItem(Immortality.SPIRIT_BEAST_CORE).setWeight(10))
						.add(LootItem.lootTableItem(Immortality.FLAME_BEAST_CORE).setWeight(5))
						.add(LootItem.lootTableItem(Immortality.EARTH_BEAST_CORE).setWeight(5))
						.add(LootItem.lootTableItem(Immortality.IMMORTALS_JADE).setWeight(15))
						.add(LootItem.lootTableItem(Immortality.STONE_BODY_MANUAL).setWeight(8))
						.add(LootItem.lootTableItem(Immortality.WANDERING_CLOUD_MANUAL).setWeight(5))
						.add(LootItem.lootTableItem(Immortality.CRIMSON_FLAME_MANUAL).setWeight(4))
						.add(LootItem.lootTableItem(Immortality.SPIRIT_STONE).setWeight(12));
					
					tableBuilder.pool(pool.build());
				}

				// 2. Strongholds and End Cities (High tier cultivation and array items)
				if (key.equals(BuiltInLootTables.STRONGHOLD_LIBRARY) || 
					key.equals(BuiltInLootTables.STRONGHOLD_CORRIDOR) || 
					key.equals(BuiltInLootTables.END_CITY_TREASURE)) {
					
					LootPool.Builder pool = LootPool.lootPool()
						.setRolls(UniformGenerator.between(0.0F, 1.0F))
						.add(LootItem.lootTableItem(Immortality.OMNISCIENCE_MANUAL).setWeight(2))
						.add(LootItem.lootTableItem(Immortality.FORMATION_COMPASS).setWeight(5))
						.add(LootItem.lootTableItem(Immortality.SPIRIT_CONVERGENCE_RUNE).setWeight(6))
						.add(LootItem.lootTableItem(Immortality.TAIJI_SHIELD_RUNE).setWeight(6))
						.add(LootItem.lootTableItem(Immortality.MIRAGE_CONCEALMENT_RUNE).setWeight(4))
						.add(LootItem.lootTableItem(Immortality.SWORD_FOREST_RUNE).setWeight(4))
						.add(LootItem.lootTableItem(Immortality.JADE_PEDESTAL).setWeight(8))
						.add(LootItem.lootTableItem(Immortality.JADE_INFUSION_ALTAR).setWeight(4))
						.add(LootItem.lootTableItem(Immortality.FORMATION_CORE).setWeight(4))
						.add(LootItem.lootTableItem(Immortality.BAMBOO_FLAG).setWeight(10))
						.add(LootItem.lootTableItem(Immortality.JADE_FLAG).setWeight(8));
					
					tableBuilder.pool(pool.build());
				}
				
				// 3. Ruined Portals (Jade/Spirit items)
				if (key.equals(BuiltInLootTables.RUINED_PORTAL)) {
					LootPool.Builder pool = LootPool.lootPool()
						.setRolls(UniformGenerator.between(0.0F, 1.0F))
						.add(LootItem.lootTableItem(Immortality.IMMORTALS_JADE).setWeight(15))
						.add(LootItem.lootTableItem(Immortality.SPIRIT_STONE).setWeight(10))
						.add(LootItem.lootTableItem(Immortality.MEDITATION_MAT).setWeight(5))
						.add(LootItem.lootTableItem(Immortality.ENLIGHTENMENT_ALTAR).setWeight(2))
						.add(LootItem.lootTableItem(Immortality.RESEARCH_STUDY).setWeight(2));
					
					tableBuilder.pool(pool.build());
				}
			}
		});
	}
}
