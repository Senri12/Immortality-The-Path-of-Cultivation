package immortality.worldgen.feature;

import com.mojang.serialization.Codec;
import immortality.Immortality;
import immortality.entity.SpiritBeastEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Random;

public class SpiritVeinGrottoFeature extends Feature<NoneFeatureConfiguration> {

	public SpiritVeinGrottoFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		BlockPos origin = context.origin();
		Random random = new Random(context.random().nextLong());

		// Underground Y: -10 to 45
		int targetY = -10 + random.nextInt(55);
		BlockPos centerPos = new BlockPos(origin.getX(), targetY, origin.getZ());

		int cx = centerPos.getX();
		int cy = centerPos.getY();
		int cz = centerPos.getZ();

		// 1. Hollow out ellipsoid cavern (radius 7x5x7)
		int rx = 6;
		int ry = 4;
		int rz = 6;

		for (int x = -rx; x <= rx; x++) {
			for (int y = -ry; y <= ry; y++) {
				for (int z = -rz; z <= rz; z++) {
					double dist = (x * x) / (double)(rx * rx) + (y * y) / (double)(ry * ry) + (z * z) / (double)(rz * rz);
					if (dist <= 1.0) {
						BlockPos p = new BlockPos(cx + x, cy + y, cz + z);
						if (y == -ry) {
							// Cavern floor: Spirit Stone & Jade
							level.setBlock(p, (random.nextFloat() < 0.4F) ? Immortality.SPIRIT_STONE.defaultBlockState() : Immortality.JADE_BLOCK.defaultBlockState(), 2);
						} else if (y < -ry + 2) {
							// Water pool around center
							if (Math.abs(x) > 2 || Math.abs(z) > 2) {
								level.setBlock(p, Blocks.WATER.defaultBlockState(), 2);
							} else {
								// Center Island
								level.setBlock(p, Immortality.JADE_BLOCK.defaultBlockState(), 2);
							}
						} else {
							level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
						}
					}
				}
			}
		}

		// 2. Island Objects
		level.setBlock(new BlockPos(cx, cy - ry + 2, cz), Immortality.ENLIGHTENMENT_ALTAR.defaultBlockState(), 2);
		level.setBlock(new BlockPos(cx - 1, cy - ry + 2, cz), Immortality.MEDITATION_MAT.defaultBlockState(), 2);

		// 3. Wall Spirit Grass & Spirit Stones
		for (int i = 0; i < 8; i++) {
			int wx = cx + (random.nextInt(rx * 2) - rx);
			int wz = cz + (random.nextInt(rz * 2) - rz);
			BlockPos wallPos = new BlockPos(wx, cy - ry + 2, wz);
			if (level.getBlockState(wallPos).isAir() && level.getBlockState(wallPos.below()).isSolid()) {
				level.setBlock(wallPos, Blocks.SHORT_GRASS.defaultBlockState(), 2);
			}
		}

		// 4. Treasure Chest
		BlockPos chestPos = new BlockPos(cx + 1, cy - ry + 2, cz);
		level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
		if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
			ChestLootPopulator.populateChest(chest, random, 3);
		}

		// 5. Guardian Spirit Beast
		if (level instanceof ServerLevel serverLevel) {
			SpiritBeastEntity beast = Immortality.SPIRIT_BEAST.create(serverLevel, EntitySpawnReason.STRUCTURE);
			if (beast != null) {
				beast.setPos(cx + 0.5D, cy - ry + 2, cz + 0.5D);
				serverLevel.addFreshEntity(beast);
			}
		}

		return true;
	}
}
