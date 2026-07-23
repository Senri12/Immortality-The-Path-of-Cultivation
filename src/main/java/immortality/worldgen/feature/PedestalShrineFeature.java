package immortality.worldgen.feature;

import com.mojang.serialization.Codec;
import immortality.Immortality;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Random;

public class PedestalShrineFeature extends Feature<NoneFeatureConfiguration> {

	public PedestalShrineFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		BlockPos origin = context.origin();
		Random random = new Random(context.random().nextLong());

		BlockPos surfacePos = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, origin);
		if (surfacePos.getY() < 50 || surfacePos.getY() > 200) {
			return false;
		}

		BlockState baseBlock = level.getBlockState(surfacePos.below());
		if (!baseBlock.isSolid()) {
			return false;
		}

		int startX = surfacePos.getX();
		int startY = surfacePos.getY();
		int startZ = surfacePos.getZ();

		BlockState jadeBlock = Immortality.JADE_BLOCK.defaultBlockState();
		BlockState jadePlanks = Immortality.JADE_PLANKS.defaultBlockState();
		BlockState jadeSlab = Immortality.JADE_SLAB.defaultBlockState();
		BlockState jadeStairs = Immortality.JADE_STAIRS.defaultBlockState();
		BlockState altarBlock = Immortality.JADE_INFUSION_ALTAR.defaultBlockState();
		BlockState pedestalBlock = Immortality.JADE_PEDESTAL.defaultBlockState();
		BlockState flagBlock = Immortality.JADE_FLAG.defaultBlockState();

		// 1. Base platform (9x9)
		for (int x = -4; x <= 4; x++) {
			for (int z = -4; z <= 4; z++) {
				for (int y = -2; y <= 0; y++) {
					level.setBlock(new BlockPos(startX + x, startY + y, startZ + z), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 2);
				}
				level.setBlock(new BlockPos(startX + x, startY + 1, startZ + z), jadePlanks, 2);
			}
		}

		// Outer border
		for (int i = -4; i <= 4; i++) {
			level.setBlock(new BlockPos(startX + i, startY + 1, startZ - 4), jadeBlock, 2);
			level.setBlock(new BlockPos(startX + i, startY + 1, startZ + 4), jadeBlock, 2);
			level.setBlock(new BlockPos(startX - 4, startY + 1, startZ + i), jadeBlock, 2);
			level.setBlock(new BlockPos(startX + 4, startY + 1, startZ + i), jadeBlock, 2);
		}

		// 2. Central Altar and 4 Pedestals
		level.setBlock(new BlockPos(startX, startY + 2, startZ), altarBlock, 2);

		level.setBlock(new BlockPos(startX, startY + 2, startZ - 3), pedestalBlock, 2);
		level.setBlock(new BlockPos(startX, startY + 2, startZ + 3), pedestalBlock, 2);
		level.setBlock(new BlockPos(startX - 3, startY + 2, startZ), pedestalBlock, 2);
		level.setBlock(new BlockPos(startX + 3, startY + 2, startZ), pedestalBlock, 2);

		// 3. Flags at 4 corners
		level.setBlock(new BlockPos(startX - 3, startY + 2, startZ - 3), flagBlock, 2);
		level.setBlock(new BlockPos(startX + 3, startY + 2, startZ - 3), flagBlock, 2);
		level.setBlock(new BlockPos(startX - 3, startY + 2, startZ + 3), flagBlock, 2);
		level.setBlock(new BlockPos(startX + 3, startY + 2, startZ + 3), flagBlock, 2);

		// 4. Pillars & Pagoda Canopy
		int[] px = {-4, 4, -4, 4};
		int[] pz = {-4, -4, 4, 4};
		for (int i = 0; i < 4; i++) {
			for (int h = 2; h <= 5; h++) {
				level.setBlock(new BlockPos(startX + px[i], startY + h, startZ + pz[i]), jadeBlock, 2);
			}
		}

		// Roof at Y+6
		for (int x = -5; x <= 5; x++) {
			for (int z = -5; z <= 5; z++) {
				if (Math.abs(x) == 5 || Math.abs(z) == 5) {
					level.setBlock(new BlockPos(startX + x, startY + 6, startZ + z), jadeSlab, 2);
				} else {
					level.setBlock(new BlockPos(startX + x, startY + 6, startZ + z), jadePlanks, 2);
				}
			}
		}
		for (int x = -3; x <= 3; x++) {
			for (int z = -3; z <= 3; z++) {
				level.setBlock(new BlockPos(startX + x, startY + 7, startZ + z), jadeBlock, 2);
			}
		}

		// 5. Chest with random loot
		BlockPos chestPos = new BlockPos(startX + 2, startY + 2, startZ + 2);
		level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
		if (level.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
			ChestLootPopulator.populateChest(chest, random, 4);
		}

		return true;
	}
}
