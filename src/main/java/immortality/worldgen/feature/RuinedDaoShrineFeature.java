package immortality.worldgen.feature;

import com.mojang.serialization.Codec;
import immortality.Immortality;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Random;

public class RuinedDaoShrineFeature extends Feature<NoneFeatureConfiguration> {

	public RuinedDaoShrineFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		BlockPos origin = context.origin();
		Random random = new Random(context.random().nextLong());

		BlockPos surfacePos = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, origin);
		if (surfacePos.getY() < 60 || surfacePos.getY() > 200) {
			return false;
		}

		int cx = surfacePos.getX();
		int cy = surfacePos.getY();
		int cz = surfacePos.getZ();

		// 1. Base 9x9 Platform
		for (int x = -4; x <= 4; x++) {
			for (int z = -4; z <= 4; z++) {
				double dist = Math.sqrt(x * x + z * z);
				if (dist <= 4.5) {
					// Foundation below
					for (int y = -2; y <= 0; y++) {
						BlockState mat = (random.nextFloat() < 0.3F) ? Immortality.JADE_BLOCK.defaultBlockState() : Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
						level.setBlock(new BlockPos(cx + x, cy + y, cz + z), mat, 2);
					}
					// Clear air above
					for (int y = 1; y <= 6; y++) {
						level.setBlock(new BlockPos(cx + x, cy + y, cz + z), Blocks.AIR.defaultBlockState(), 2);
					}
				}
			}
		}

		// 2. Center Formation Core & Pedestals
		level.setBlock(new BlockPos(cx, cy + 1, cz), Immortality.FORMATION_CORE.defaultBlockState(), 2);
		level.setBlock(new BlockPos(cx - 2, cy + 1, cz), Immortality.JADE_PEDESTAL.defaultBlockState(), 2);
		level.setBlock(new BlockPos(cx + 2, cy + 1, cz), Immortality.JADE_PEDESTAL.defaultBlockState(), 2);
		level.setBlock(new BlockPos(cx, cy + 1, cz - 2), Immortality.ENLIGHTENMENT_ALTAR.defaultBlockState(), 2);
		level.setBlock(new BlockPos(cx, cy + 1, cz + 2), Immortality.MEDITATION_MAT.defaultBlockState(), 2);

		// 3. 4 Corner Pillars & Spiritual Flags
		int[][] corners = {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}};
		for (int i = 0; i < corners.length; i++) {
			int px = cx + corners[i][0];
			int pz = cz + corners[i][1];
			// Pillar Y+1..Y+3
			level.setBlock(new BlockPos(px, cy + 1, pz), Immortality.JADE_BLOCK.defaultBlockState(), 2);
			level.setBlock(new BlockPos(px, cy + 2, pz), Immortality.JADE_BLOCK.defaultBlockState(), 2);
			level.setBlock(new BlockPos(px, cy + 3, pz), Immortality.JADE_FENCE.defaultBlockState(), 2);
			
			// Flag at top of pillar
			BlockState flag = (i % 2 == 0) ? Immortality.BAMBOO_FLAG.defaultBlockState() : Immortality.JADE_FLAG.defaultBlockState();
			level.setBlock(new BlockPos(px, cy + 4, pz), flag, 2);
		}

		// 4. Treasure Chest
		BlockPos chestPos = new BlockPos(cx + 1, cy + 1, cz + 1);
		level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
		if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
			ChestLootPopulator.populateChest(chest, random, 2);
		}

		return true;
	}
}
