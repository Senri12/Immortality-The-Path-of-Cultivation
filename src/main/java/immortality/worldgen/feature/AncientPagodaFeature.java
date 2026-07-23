package immortality.worldgen.feature;

import com.mojang.serialization.Codec;
import immortality.Immortality;
import immortality.entity.SpiritBeastEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Random;

public class AncientPagodaFeature extends Feature<NoneFeatureConfiguration> {

	public AncientPagodaFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		BlockPos origin = context.origin();
		Random random = new Random(context.random().nextLong());

		// Find surface Y
		BlockPos surfacePos = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, origin);
		if (surfacePos.getY() < 60 || surfacePos.getY() > 180) {
			return false;
		}

		BlockState baseBlock = level.getBlockState(surfacePos.below());
		if (!baseBlock.isSolid()) {
			return false;
		}

		int startX = surfacePos.getX();
		int startY = surfacePos.getY();
		int startZ = surfacePos.getZ();

		// 1. Clear 9x9 area and build deepslate foundation
		for (int x = -4; x <= 4; x++) {
			for (int z = -4; z <= 4; z++) {
				for (int y = -3; y <= 0; y++) {
					level.setBlock(new BlockPos(startX + x, startY + y, startZ + z), Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
				}
			}
		}

		// 2. FLOOR 1 (Y+1 to Y+5): Entrance Hall
		buildFloorWalls(level, startX, startY + 1, startZ, 4, 5, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), Immortality.JADE_BLOCK.defaultBlockState());
		
		// Doorway (South)
		level.setBlock(new BlockPos(startX, startY + 1, startZ + 4), Blocks.AIR.defaultBlockState(), 2);
		level.setBlock(new BlockPos(startX, startY + 2, startZ + 4), Blocks.AIR.defaultBlockState(), 2);

		// Floor 1 Objects
		level.setBlock(new BlockPos(startX, startY + 1, startZ), Immortality.ENLIGHTENMENT_ALTAR.defaultBlockState(), 2);
		level.setBlock(new BlockPos(startX - 1, startY + 1, startZ), Immortality.MEDITATION_MAT.defaultBlockState(), 2);

		// Floor 1 Chest
		BlockPos chestPos1 = new BlockPos(startX + 2, startY + 1, startZ - 2);
		level.setBlock(chestPos1, Blocks.CHEST.defaultBlockState(), 2);
		if (level.getBlockEntity(chestPos1) instanceof ChestBlockEntity chest) {
			ChestLootPopulator.populateChest(chest, random, 1);
		}

		// Eaves 1 (Y+5)
		buildPagodaEaves(level, startX, startY + 5, startZ, 5);

		// 3. FLOOR 2 (Y+6 to Y+10): Library & Artifact Chamber
		buildFloorWalls(level, startX, startY + 6, startZ, 3, 5, Blocks.DEEPSLATE_BRICKS.defaultBlockState(), Immortality.JADE_BLOCK.defaultBlockState());

		// Floor 2 Objects
		level.setBlock(new BlockPos(startX - 1, startY + 6, startZ - 1), Blocks.BOOKSHELF.defaultBlockState(), 2);
		level.setBlock(new BlockPos(startX + 1, startY + 6, startZ - 1), Blocks.BOOKSHELF.defaultBlockState(), 2);
		level.setBlock(new BlockPos(startX, startY + 6, startZ - 1), Immortality.JADE_PEDESTAL.defaultBlockState(), 2);

		// Floor 2 Chest
		BlockPos chestPos2 = new BlockPos(startX - 2, startY + 6, startZ + 1);
		level.setBlock(chestPos2, Blocks.CHEST.defaultBlockState(), 2);
		if (level.getBlockEntity(chestPos2) instanceof ChestBlockEntity chest) {
			ChestLootPopulator.populateChest(chest, random, 2);
		}

		// Eaves 2 (Y+10)
		buildPagodaEaves(level, startX, startY + 10, startZ, 4);

		// 4. FLOOR 3 (Y+11 to Y+15): Grand Altar & Formation Core
		buildFloorWalls(level, startX, startY + 11, startZ, 2, 5, Immortality.JADE_BLOCK.defaultBlockState(), Blocks.CHISELED_DEEPSLATE.defaultBlockState());

		// Floor 3 Objects
		level.setBlock(new BlockPos(startX, startY + 11, startZ), Immortality.JADE_INFUSION_ALTAR.defaultBlockState(), 2);
		level.setBlock(new BlockPos(startX - 1, startY + 11, startZ), Immortality.FORMATION_CORE.defaultBlockState(), 2);
		level.setBlock(new BlockPos(startX + 1, startY + 11, startZ), Immortality.JADE_PEDESTAL.defaultBlockState(), 2);

		// Floor 3 Chest
		BlockPos chestPos3 = new BlockPos(startX, startY + 11, startZ + 1);
		level.setBlock(chestPos3, Blocks.CHEST.defaultBlockState(), 2);
		if (level.getBlockEntity(chestPos3) instanceof ChestBlockEntity chest) {
			ChestLootPopulator.populateChest(chest, random, 3);
		}

		// Roof Eaves & Spire (Y+15 to Y+18)
		buildPagodaEaves(level, startX, startY + 15, startZ, 3);
		level.setBlock(new BlockPos(startX, startY + 16, startZ), Immortality.JADE_BLOCK.defaultBlockState(), 2);
		level.setBlock(new BlockPos(startX, startY + 17, startZ), Immortality.JADE_FENCE.defaultBlockState(), 2);
		level.setBlock(new BlockPos(startX, startY + 18, startZ), Blocks.LIGHTNING_ROD.defaultBlockState(), 2);

		// 5. Spawn Spirit Beast Guardian on balcony/roof
		if (level instanceof ServerLevel serverLevel) {
			SpiritBeastEntity beast = Immortality.SPIRIT_BEAST.create(serverLevel, EntitySpawnReason.STRUCTURE);
			if (beast != null) {
				beast.setPos(startX + 0.5D, startY + 16, startZ + 0.5D);
				serverLevel.addFreshEntity(beast);
			}
		}

		return true;
	}

	private void buildFloorWalls(WorldGenLevel level, int cx, int cy, int cz, int radius, int height, BlockState wallState, BlockState cornerState) {
		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				boolean isCorner = Math.abs(x) == radius && Math.abs(z) == radius;
				boolean isEdge = Math.abs(x) == radius || Math.abs(z) == radius;
				for (int h = 0; h < height; h++) {
					BlockPos pos = new BlockPos(cx + x, cy + h, cz + z);
					if (isCorner) {
						level.setBlock(pos, cornerState, 2);
					} else if (isEdge) {
						level.setBlock(pos, wallState, 2);
					} else if (h == 0) {
						level.setBlock(pos, Blocks.SMOOTH_BASALT.defaultBlockState(), 2);
					} else {
						level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
					}
				}
			}
		}
	}

	private void buildPagodaEaves(WorldGenLevel level, int cx, int cy, int cz, int radius) {
		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				if (Math.abs(x) == radius || Math.abs(z) == radius) {
					BlockPos pos = new BlockPos(cx + x, cy, cz + z);
					level.setBlock(pos, Immortality.JADE_SLAB.defaultBlockState(), 2);
				}
			}
		}
	}
}
