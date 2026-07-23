package immortality.worldgen.feature;

import immortality.Immortality;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class ChestLootPopulator {

	public static void populateChest(ChestBlockEntity chest, Random random, int tier) {
		if (chest == null) return;
		chest.clearContent();

		int itemCount = 3 + random.nextInt(4); // 3 to 6 items
		Set<Integer> usedSlots = new HashSet<>();

		for (int i = 0; i < itemCount; i++) {
			int slot;
			do {
				slot = random.nextInt(27);
			} while (usedSlots.contains(slot));
			usedSlots.add(slot);

			ItemStack stack = getRandomLoot(random, tier);
			if (!stack.isEmpty()) {
				chest.setItem(slot, stack);
			}
		}
	}

	private static ItemStack getRandomLoot(Random random, int tier) {
		float roll = random.nextFloat();

		if (tier == 1) {
			if (roll < 0.35F) {
				// Mod items (35%)
				int modRoll = random.nextInt(4);
				switch (modRoll) {
					case 0: return new ItemStack(Immortality.SPIRIT_GRASS, 1 + random.nextInt(3));
					case 1: return new ItemStack(Immortality.SPIRIT_STONE, 2 + random.nextInt(5));
					case 2: return new ItemStack(Immortality.LIGHTNING_TALISMAN, 1 + random.nextInt(2));
					case 3: return new ItemStack(Immortality.WANDERING_CLOUD_MANUAL, 1);
				}
			} else {
				// Vanilla junk/gear (65%)
				int junkRoll = random.nextInt(9);
				switch (junkRoll) {
					case 0: return new ItemStack(Items.COAL, 2 + random.nextInt(5));
					case 1: return new ItemStack(Items.PAPER, 1 + random.nextInt(4));
					case 2: return new ItemStack(Items.BONE, 1 + random.nextInt(3));
					case 3: return new ItemStack(Items.BREAD, 1 + random.nextInt(3));
					case 4: return new ItemStack(Items.TORCH, 3 + random.nextInt(6));
					case 5: return new ItemStack(Items.STRING, 1 + random.nextInt(4));
					case 6: return new ItemStack(Items.LEATHER_CHESTPLATE, 1);
					case 7: return new ItemStack(Items.IRON_NUGGET, 2 + random.nextInt(6));
					case 8: return new ItemStack(Items.ROTTEN_FLESH, 2 + random.nextInt(4));
				}
			}
		} else if (tier == 2) {
			if (roll < 0.40F) {
				// Mod items (40%)
				int modRoll = random.nextInt(6);
				switch (modRoll) {
					case 0: return new ItemStack(Immortality.IMMORTALS_JADE, 1 + random.nextInt(2));
					case 1: return new ItemStack(Immortality.STONE_BODY_MANUAL, 1);
					case 2: return new ItemStack(Immortality.CRIMSON_FLAME_MANUAL, 1);
					case 3: return new ItemStack(Immortality.SPIRIT_CONVERGENCE_RUNE, 1);
					case 4: return new ItemStack(Immortality.TAIJI_SHIELD_RUNE, 1);
					case 5: return new ItemStack(Immortality.HEAVENLY_IRON, 1 + random.nextInt(2));
				}
			} else {
				// Vanilla items/gear (60%)
				int junkRoll = random.nextInt(8);
				switch (junkRoll) {
					case 0: return new ItemStack(Items.IRON_INGOT, 1 + random.nextInt(3));
					case 1: return new ItemStack(Items.GOLD_INGOT, 1 + random.nextInt(2));
					case 2: return new ItemStack(Items.BOOK, 1 + random.nextInt(3));
					case 3: return new ItemStack(Items.IRON_SWORD, 1);
					case 4: return new ItemStack(Items.IRON_HELMET, 1);
					case 5: return new ItemStack(Items.ARROW, 4 + random.nextInt(8));
					case 6: return new ItemStack(Items.LAPIS_LAZULI, 3 + random.nextInt(5));
					case 7: return new ItemStack(Items.EXPERIENCE_BOTTLE, 1 + random.nextInt(2));
				}
			}
		} else { // Tier 3
			if (roll < 0.45F) {
				// Mod High-tier items (45%)
				int modRoll = random.nextInt(7);
				switch (modRoll) {
					case 0: return new ItemStack(Immortality.NINE_DRAGONS_MANUAL, 1);
					case 1: return new ItemStack(Immortality.FOUNDATION_PILL, 1);
					case 2: return new ItemStack(Immortality.GOLDEN_CORE_PILL, 1);
					case 3: return new ItemStack(Immortality.HEAVENLY_LIGHTNING_RUNE, 1);
					case 4: return new ItemStack(Immortality.DRAGON_VEIN_STONE, 1);
					case 5: return new ItemStack(Immortality.PHOENIX_FEATHER, 1);
					case 6: return new ItemStack(Immortality.SPIRIT_BEAST_CORE, 1 + random.nextInt(2));
				}
			} else {
				// Vanilla high-tier gear/loot (55%)
				int junkRoll = random.nextInt(7);
				switch (junkRoll) {
					case 0: return new ItemStack(Items.DIAMOND, 1 + random.nextInt(2));
					case 1: return new ItemStack(Items.GOLDEN_CARROT, 2 + random.nextInt(4));
					case 2: return new ItemStack(Items.DIAMOND_SWORD, 1);
					case 3: return new ItemStack(Items.IRON_LEGGINGS, 1);
					case 4: return new ItemStack(Items.EMERALD, 2 + random.nextInt(4));
					case 5: return new ItemStack(Items.AMETHYST_SHARD, 2 + random.nextInt(5));
					case 6: return new ItemStack(Items.ENDER_PEARL, 1 + random.nextInt(2));
				}
			}
		}

		return ItemStack.EMPTY;
	}
}
