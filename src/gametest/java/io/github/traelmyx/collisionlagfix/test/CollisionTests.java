package io.github.traelmyx.collisionlagfix.test;

import io.github.traelmyx.collisionlagfix.CollisionLagFix;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class CollisionTests {
	private CollisionTests() {
	}

	// away from the test structure; everything happens within one tick, so the chunks setBlock loads stay loaded
	private static final int OFFSET = 512;

	private static Entity create(ServerLevel level, String kind) {
		//? if >=26 {
		/*EntityType<?> type = switch (kind) {
			case "tnt" -> net.minecraft.world.entity.EntityTypes.TNT;
			case "item" -> net.minecraft.world.entity.EntityTypes.ITEM;
			case "falling" -> net.minecraft.world.entity.EntityTypes.FALLING_BLOCK;
			default -> net.minecraft.world.entity.EntityTypes.ZOMBIE;
		};
		*///?} else {
		EntityType<?> type = switch (kind) {
			case "tnt" -> EntityType.TNT;
			case "item" -> EntityType.ITEM;
			case "falling" -> EntityType.FALLING_BLOCK;
			default -> EntityType.ZOMBIE;
		};
		//?}
		//? if >=1.21.11 {
		/*return type.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
		*///?} else
		return type.create(level);
	}


	private static Vec3 moveOnce(ServerLevel level, String kind, Vec3 start, Vec3 motion, boolean fixed) {
		boolean was = CollisionLagFix.enabled;
		CollisionLagFix.enabled = fixed;
		try {
			Entity entity = create(level, kind);
			entity.setPos(start.x, start.y, start.z);
			entity.move(MoverType.SELF, motion);
			return entity.position();
		} finally {
			CollisionLagFix.enabled = was;
		}
	}

	/** Thousands of random moves through a random field of stone, slabs and fences end exactly where vanilla ends them. */
	public static void matchesVanilla(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(BlockPos.ZERO).offset(OFFSET, 0, 0);
		int size = 40, height = 24;
		RandomSource random = RandomSource.create(260743L);
		BlockState[] palette = {Blocks.STONE.defaultBlockState(), Blocks.STONE_SLAB.defaultBlockState(), Blocks.OAK_FENCE.defaultBlockState()};
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = 0; x < size; x++) for (int y = 0; y < height; y++) for (int z = 0; z < size; z++) {
			BlockState state = random.nextFloat() < 0.04f ? palette[random.nextInt(palette.length)] : Blocks.AIR.defaultBlockState();
			level.setBlock(pos.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z), state, 2 | 16);
		}
		String[] kinds = {"tnt", "item", "falling", "zombie"};
		List<String> mismatches = new ArrayList<>();
		int checked = 0;
		for (int i = 0; i < 3000; i++) {
			Vec3 start = new Vec3(origin.getX() + 2 + random.nextDouble() * (size - 4), origin.getY() + 2 + random.nextDouble() * (height - 4), origin.getZ() + 2 + random.nextDouble() * (size - 4));
			double speed = 2 + random.nextDouble() * 30;
			Vec3 motion = new Vec3(random.nextGaussian(), random.nextGaussian() * 0.5, random.nextGaussian()).normalize().scale(speed);
			if (random.nextInt(4) == 0) motion = new Vec3(motion.x, 0, motion.z);
			if (!CollisionLagFix.isFast(motion)) continue;
			String kind = kinds[i % kinds.length];
			Vec3 vanilla = moveOnce(level, kind, start, motion, false);
			Vec3 fixed = moveOnce(level, kind, start, motion, true);
			checked++;
			if (!vanilla.equals(fixed) && mismatches.size() < 5) mismatches.add(kind + " from " + start + " by " + motion + ": vanilla " + vanilla + ", fixed " + fixed);
		}
		helper.assertTrue(checked > 1500, "only " + checked + " fast moves checked");
		helper.assertTrue(mismatches.isEmpty(), "moves differ from vanilla: " + mismatches);
		helper.succeed();
	}

	private static long bestMoveNanos(ServerLevel level, Vec3 start, Vec3 motion, boolean fixed) {
		long best = Long.MAX_VALUE;
		for (int trial = 0; trial < 5; trial++) {
			long t0 = System.nanoTime();
			moveOnce(level, "tnt", start, motion, fixed);
			best = Math.min(best, System.nanoTime() - t0);
		}
		return best;
	}

	/** A TNT moving 80 blocks per tick diagonally into solid stone: vanilla scans the whole box, the fix only its path. */
	public static void fastTntIsCheap(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(BlockPos.ZERO).offset(OFFSET, 0, 200);
		int size = 96, height = 32;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = 0; x < size; x++) for (int y = 0; y < height; y++) for (int z = 0; z < size; z++)
			level.setBlock(pos.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z), Blocks.STONE.defaultBlockState(), 2 | 16);
		Vec3 start = new Vec3(origin.getX() + 1.5, origin.getY() + height + 1, origin.getZ() + 1.5);
		Vec3 motion = new Vec3(80, -20, 80);
		long vanilla = bestMoveNanos(level, start, motion, false);
		long fixed = bestMoveNanos(level, start, motion, true);
		Vec3 a = moveOnce(level, "tnt", start, motion, false), b = moveOnce(level, "tnt", start, motion, true);
		System.out.printf("[collision_lag_fix] TNT at 80 blocks/tick into stone: vanilla %.2f ms, fixed %.3f ms%n", vanilla / 1e6, fixed / 1e6);
		helper.assertTrue(a.equals(b), "different end positions: vanilla " + a + ", fixed " + b);
		helper.assertTrue(fixed * 10 < vanilla, String.format("fix not faster: vanilla %.2f ms, fixed %.3f ms", vanilla / 1e6, fixed / 1e6));
		helper.succeed();
	}
}
