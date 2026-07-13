package immortality.dimension;

import immortality.Immortality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ImmortalPortalManager {
	public static final ResourceKey<Level> WORLD_OF_IMMORTALS = ResourceKey.create(Registries.DIMENSION, Immortality.id("world_of_immortals"));
	private static final int PORTAL_WIDTH = 2;
	private static final int PORTAL_HEIGHT = 3;
	private static final Map<UUID, BlockPos> OVERWORLD_PORTALS = new ConcurrentHashMap<>();
	private static final Map<UUID, BlockPos> IMMORTAL_PORTALS = new ConcurrentHashMap<>();

	private ImmortalPortalManager() {
	}

	public static boolean tryCreatePortal(Level level, BlockPos clickedPos, Direction clickedFace) {
		BlockPos interior = level.getBlockState(clickedPos).isAir() ? clickedPos : clickedPos.relative(clickedFace);
		if (!canReplaceInterior(level.getBlockState(interior))) {
			return false;
		}
		Frame frame = findFrame(level, interior);
		if (frame == null) {
			return false;
		}
		fillInterior(level, frame);
		return true;
	}

	public static void teleport(ServerPlayer player) {
		ServerLevel current = player.level();
		var server = current.getServer();
		ServerLevel target = current.dimension().equals(WORLD_OF_IMMORTALS)
			? server.overworld()
			: server.getLevel(WORLD_OF_IMMORTALS);
		if (target == null) {
			Immortality.LOGGER.warn("World of immortals dimension is missing; portal teleport skipped");
			return;
		}

		UUID playerId = player.getUUID();
		BlockPos sourcePortal = findNearbyPortalBlock(current, player.blockPosition());
		if (sourcePortal != null) {
			getPortalMap(current.dimension()).put(playerId, sourcePortal);
		}

		BlockPos linkedPortal = getPortalMap(target.dimension()).get(playerId);
		Arrival arrival = linkedPortal != null && findNearbyPortalBlock(target, linkedPortal) != null
			? attachToExistingPortal(target, linkedPortal)
			: prepareArrival(target, player.blockPosition());
		getPortalMap(target.dimension()).put(playerId, arrival.portal());

		BlockPos destination = arrival.arrival();
		player.teleportTo(target, destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D, Set.<Relative>of(), player.getYRot(), player.getXRot(), false);
		player.setPortalCooldown();
	}

	private static Arrival prepareArrival(ServerLevel target, BlockPos sourcePos) {
		int x = sourcePos.getX();
		int z = sourcePos.getZ();
		int y = target.dimension().equals(WORLD_OF_IMMORTALS)
			? 96
			: target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 1;
		BlockPos center = new BlockPos(x, y, z);
		buildPlatform(target, center);
		buildPortalFrame(target, center);
		return new Arrival(center, prepareSafeArrival(target, center));
	}

	private static void buildPlatform(ServerLevel target, BlockPos center) {
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				target.setBlockAndUpdate(center.offset(dx, -1, dz), Immortality.SPIRIT_STONE.defaultBlockState());
				for (int dy = 0; dy <= 4; dy++) {
					target.setBlockAndUpdate(center.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
				}
			}
		}
	}

	private static void buildPortalFrame(ServerLevel target, BlockPos center) {
		BlockPos base = center.offset(-1, 0, 0);
		for (int x = -1; x <= PORTAL_WIDTH; x++) {
			for (int y = -1; y <= PORTAL_HEIGHT; y++) {
				BlockPos pos = base.offset(x, y, 0);
				boolean border = x == -1 || x == PORTAL_WIDTH || y == -1 || y == PORTAL_HEIGHT;
				target.setBlockAndUpdate(pos, border ? Blocks.EMERALD_BLOCK.defaultBlockState() : Immortality.IMMORTAL_PORTAL.defaultBlockState());
			}
		}
	}

	private static BlockPos prepareSafeArrival(ServerLevel target, BlockPos center) {
		for (BlockPos candidate : new BlockPos[]{center.offset(0, 0, 2), center.offset(0, 0, -2), center.offset(2, 0, 0), center.offset(-2, 0, 0)}) {
			if (ensureSafeStand(target, candidate)) {
				return candidate;
			}
		}
		BlockPos fallback = center.offset(0, 0, 2);
		ensureSafeStand(target, fallback);
		return fallback;
	}

	private static Arrival attachToExistingPortal(ServerLevel target, BlockPos portalPos) {
		return new Arrival(portalPos, prepareSafeArrival(target, portalPos));
	}

	private static boolean ensureSafeStand(ServerLevel target, BlockPos pos) {
		BlockState below = target.getBlockState(pos.below());
		if (below.isAir() || below.is(Immortality.IMMORTAL_PORTAL)) {
			target.setBlockAndUpdate(pos.below(), target.dimension().equals(WORLD_OF_IMMORTALS) ? Immortality.SPIRIT_STONE.defaultBlockState() : Blocks.STONE.defaultBlockState());
		}
		target.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		target.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
		return !target.getBlockState(pos.below()).isAir();
	}

	private static Map<UUID, BlockPos> getPortalMap(ResourceKey<Level> dimension) {
		return dimension.equals(WORLD_OF_IMMORTALS) ? IMMORTAL_PORTALS : OVERWORLD_PORTALS;
	}

	private static BlockPos findNearbyPortalBlock(Level level, BlockPos origin) {
		for (int dy = -1; dy <= 1; dy++) {
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					BlockPos candidate = origin.offset(dx, dy, dz);
					if (level.getBlockState(candidate).is(Immortality.IMMORTAL_PORTAL)) {
						return candidate;
					}
				}
			}
		}
		return null;
	}

	private static Frame findFrame(Level level, BlockPos interior) {
		for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
			Direction positive = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
			for (int xOffset = 0; xOffset < PORTAL_WIDTH; xOffset++) {
				for (int yOffset = 0; yOffset < PORTAL_HEIGHT; yOffset++) {
					BlockPos base = interior.relative(positive.getOpposite(), xOffset).below(yOffset);
					if (isValidFrame(level, base, axis)) {
						return new Frame(base, axis);
					}
				}
			}
		}
		return null;
	}

	private static boolean isValidFrame(Level level, BlockPos base, Direction.Axis axis) {
		Direction widthDirection = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
		for (int width = -1; width <= PORTAL_WIDTH; width++) {
			for (int height = -1; height <= PORTAL_HEIGHT; height++) {
				BlockPos pos = base.relative(widthDirection, width).above(height);
				boolean border = width == -1 || width == PORTAL_WIDTH || height == -1 || height == PORTAL_HEIGHT;
				if (border) {
					if (!level.getBlockState(pos).is(Blocks.EMERALD_BLOCK)) {
						return false;
					}
					continue;
				}
				if (!canReplaceInterior(level.getBlockState(pos))) {
					return false;
				}
			}
		}
		return true;
	}

	private static void fillInterior(Level level, Frame frame) {
		Direction widthDirection = frame.axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
		for (int width = 0; width < PORTAL_WIDTH; width++) {
			for (int height = 0; height < PORTAL_HEIGHT; height++) {
				level.setBlock(frame.base.relative(widthDirection, width).above(height), Immortality.IMMORTAL_PORTAL.defaultBlockState(), 3);
			}
		}
	}

	private static boolean canReplaceInterior(BlockState state) {
		return state.isAir() || state.is(Blocks.FIRE) || state.is(Immortality.IMMORTAL_PORTAL);
	}

	private record Frame(BlockPos base, Direction.Axis axis) {
	}

	private record Arrival(BlockPos portal, BlockPos arrival) {
	}
}
