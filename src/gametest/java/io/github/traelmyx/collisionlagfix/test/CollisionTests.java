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
			case "boat" -> net.minecraft.world.entity.EntityTypes.OAK_BOAT;
			case "minecart" -> net.minecraft.world.entity.EntityTypes.MINECART;
			case "shulker" -> net.minecraft.world.entity.EntityTypes.SHULKER;
			default -> net.minecraft.world.entity.EntityTypes.ZOMBIE;
		};
		*///?} else if >=1.21.11 {
		/*EntityType<?> type = switch (kind) {
			case "tnt" -> EntityType.TNT;
			case "item" -> EntityType.ITEM;
			case "falling" -> EntityType.FALLING_BLOCK;
			case "boat" -> EntityType.OAK_BOAT;
			case "minecart" -> EntityType.MINECART;
			case "shulker" -> EntityType.SHULKER;
			default -> EntityType.ZOMBIE;
		};
		*///?} else {
		EntityType<?> type = switch (kind) {
			case "tnt" -> EntityType.TNT;
			case "item" -> EntityType.ITEM;
			case "falling" -> EntityType.FALLING_BLOCK;
			case "boat" -> EntityType.BOAT;
			case "minecart" -> EntityType.MINECART;
			case "shulker" -> EntityType.SHULKER;
			default -> EntityType.ZOMBIE;
		};
		//?}
		//? if >=1.21.11 {
		/*return type.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
		*///?} else
		return type.create(level);
	}

	/** One mover: what it is and the state it starts each run in, so the vanilla and fixed runs start identical. */
	private record Mover(String kind, boolean onGround, boolean sneak, boolean boots) {
	}

	// a fresh entity every run: moving through powder snow leaves a slowdown that would carry over into the next move
	private static Entity prepare(ServerLevel level, Mover mover) {
		Entity entity = create(level, mover.kind());
		entity.setOnGround(mover.onGround());
		// sneaking is what scaffolding's collision shape looks at; leather boots let mobs walk on powder snow
		entity.setShiftKeyDown(mover.sneak());
		if (mover.boots() && entity instanceof net.minecraft.world.entity.LivingEntity living)
			living.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_BOOTS));
		return entity;
	}

	private static Vec3 moveOnce(ServerLevel level, Mover mover, Vec3 start, Vec3 motion, boolean fixed) {
		boolean was = CollisionLagFix.enabled;
		CollisionLagFix.enabled = fixed;
		try {
			Entity entity = prepare(level, mover);
			entity.setPos(start.x, start.y, start.z);
			entity.move(MoverType.SELF, motion);
			return entity.position();
		} finally {
			CollisionLagFix.enabled = was;
		}
	}

	private static Vec3 moveOnce(ServerLevel level, String kind, Vec3 start, Vec3 motion, boolean fixed) {
		return moveOnce(level, new Mover(kind, false, false, false), start, motion, fixed);
	}

	/**
	 * Thousands of random fast moves end exactly where vanilla ends them: through stone, top and bottom slabs, fences,
	 * scaffolding and powder snow, past boats, minecarts and shulkers, for TNT, items, falling blocks, zombies sneaking or
	 * not and with or without leather boots, on the ground (step-up) or in the air, and across the world border.
	 */
	public static void matchesVanilla(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(BlockPos.ZERO).offset(OFFSET, 0, 0);
		int size = 40, height = 24;
		RandomSource random = RandomSource.create(260743L);
		BlockState[] palette = {
			Blocks.STONE.defaultBlockState(),
			Blocks.STONE_SLAB.defaultBlockState(),
			Blocks.STONE_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.TOP),
			Blocks.OAK_FENCE.defaultBlockState(),
			Blocks.SCAFFOLDING.defaultBlockState(),
			Blocks.POWDER_SNOW.defaultBlockState(),
		};
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = 0; x < size; x++) for (int y = 0; y < height; y++) for (int z = 0; z < size; z++) {
			BlockState state = y == 0 ? Blocks.STONE.defaultBlockState()
				: random.nextFloat() < 0.05f ? palette[random.nextInt(palette.length)] : Blocks.AIR.defaultBlockState();
			level.setBlock(pos.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z), state, 2 | 16);
		}
		// load every chunk a move can reach first: the vanilla run would otherwise load some of them as a side effect
		// (block effects along its path) and the fixed run right after it would see blocks the vanilla run could not
		for (int cx = (origin.getX() - 48) >> 4; cx <= (origin.getX() + size + 48) >> 4; cx++)
			for (int cz = (origin.getZ() - 48) >> 4; cz <= (origin.getZ() + size + 48) >> 4; cz++)
				level.getChunk(cx, cz);
		List<Entity> obstacles = new ArrayList<>();
		String[] obstacleKinds = {"boat", "minecart", "shulker"};
		for (int i = 0; i < 60; i++) {
			Entity obstacle = create(level, obstacleKinds[i % obstacleKinds.length]);
			obstacle.setPos(origin.getX() + 2 + random.nextInt(size - 4) + 0.5, origin.getY() + 1 + random.nextInt(height - 2), origin.getZ() + 2 + random.nextInt(size - 4) + 0.5);
			obstacle.setNoGravity(true);
			if (obstacle instanceof net.minecraft.world.entity.Mob mob) mob.setNoAi(true);
			level.addFreshEntity(obstacle);
			obstacles.add(obstacle);
		}
		var border = level.getWorldBorder();
		double borderSize = border.getSize(), borderX = border.getCenterX(), borderZ = border.getCenterZ();
		List<String> mismatches = new ArrayList<>();
		int checked = 0;
		long perAxisBefore = CollisionLagFix.perAxisMoves;
		try {
			for (int round = 0; round < 2; round++) {
				if (round == 1) {
					// second round: the world border runs through the middle of the field
					border.setCenter(origin.getX() + size, origin.getZ() + size);
					border.setSize(size * 1.5);
				}
				for (int i = 0; i < 2500; i++) {
					Vec3 start = new Vec3(origin.getX() + 2 + random.nextDouble() * (size - 4), origin.getY() + 1 + random.nextDouble() * (height - 4), origin.getZ() + 2 + random.nextDouble() * (size - 4));
					double speed = 2 + random.nextDouble() * 30;
					Vec3 motion = new Vec3(random.nextGaussian(), random.nextGaussian() * 0.5, random.nextGaussian()).normalize().scale(speed);
					if (random.nextInt(4) == 0) motion = new Vec3(motion.x, 0, motion.z);
					if (!CollisionLagFix.isFast(motion)) continue;
					String[] kinds = {"tnt", "item", "falling", "zombie", "zombie"};
					Mover mover = new Mover(kinds[random.nextInt(kinds.length)], random.nextBoolean(), random.nextBoolean(), random.nextBoolean());
					Vec3 vanilla = moveOnce(level, mover, start, motion, false);
					Vec3 fixed = moveOnce(level, mover, start, motion, true);
					checked++;
					if (!vanilla.equals(fixed) && mismatches.size() < 5)
						mismatches.add(mover + (round == 1 ? " near the border" : "") + " from " + start + " by " + motion + ": vanilla " + vanilla + ", fixed " + fixed);
				}
			}
		} finally {
			border.setCenter(borderX, borderZ);
			border.setSize(borderSize);
			obstacles.forEach(Entity::discard);
		}
		long perAxis = CollisionLagFix.perAxisMoves - perAxisBefore;
		System.out.printf("[collision_lag_fix] compared %d fast moves, %d of them on the per-axis path%n", checked, perAxis);
		helper.assertTrue(checked > 2500, "only " + checked + " fast moves checked");
		// the border round and grounded mobs blocked sideways stay on the vanilla path by design
		helper.assertTrue(perAxis > 1000, "only " + perAxis + " of " + checked + " moves used the per-axis path");
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

	/** MC-263882 (1.21.1): TNT falling 150 blocks per tick no longer traces a ray along its whole fall, and lands the same. */
	public static void fallingTntSkipsTrace(GameTestHelper helper) {
		//? if <1.21.11 {
		ServerLevel level = helper.getLevel();
		Vec3 start = Vec3.atBottomCenterOf(helper.absolutePos(BlockPos.ZERO).offset(OFFSET, 200, 400));
		Vec3 motion = new Vec3(0, -150, 0);
		long vanilla = Long.MAX_VALUE, fixed = Long.MAX_VALUE;
		Vec3 endVanilla = null, endFixed = null;
		for (int trial = 0; trial < 5; trial++) {
			for (boolean fix : new boolean[] {false, true}) {
				boolean was = CollisionLagFix.skipTntFallTrace;
				CollisionLagFix.skipTntFallTrace = fix;
				Entity tnt = create(level, "tnt");
				tnt.setPos(start.x, start.y, start.z);
				tnt.fallDistance = 10;
				long t0 = System.nanoTime();
				tnt.move(MoverType.SELF, motion);
				long t = System.nanoTime() - t0;
				CollisionLagFix.skipTntFallTrace = was;
				if (fix) { fixed = Math.min(fixed, t); endFixed = tnt.position(); }
				else { vanilla = Math.min(vanilla, t); endVanilla = tnt.position(); }
			}
		}
		System.out.printf("[collision_lag_fix] TNT falling 150 blocks/tick: vanilla %.3f ms, fixed %.3f ms%n", vanilla / 1e6, fixed / 1e6);
		helper.assertTrue(endVanilla.equals(endFixed), "different end positions: " + endVanilla + " and " + endFixed);
		helper.assertTrue(fixed < vanilla, String.format("no faster: vanilla %.3f ms, fixed %.3f ms", vanilla / 1e6, fixed / 1e6));
		//?}
		helper.succeed();
	}
}
