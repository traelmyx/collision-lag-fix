package io.github.traelmyx.collisionlagfix;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;

/**
 * Vanilla collects the block shapes of the whole box an entity's movement spans, then moves it along Y, then along the
 * longer and the shorter horizontal axis. For an entity moving tens of blocks per tick on two or three axes that box holds
 * hundreds of thousands of blocks, almost none of which it can touch (MC-260743). Here each axis only collects the slab it
 * moves through. The shapes that can stop the entity are the same, in the same order, so the result is the same.
 */
public final class CollisionLagFix {
	public static final String MOD_ID = "collision_lag_fix";
	public static final Logger LOGGER = LoggerFactory.getLogger("Collision Lag Fix");

	public static boolean enabled = true;
	/** Movements below this many blocks on every axis but one keep the vanilla path. */
	public static double minSpeed = 2.0;

	private CollisionLagFix() {
	}

	/** True when vanilla's single box would be much bigger than the three slabs. */
	public static boolean isFast(Vec3 movement) {
		int fastAxes = (Math.abs(movement.x) >= minSpeed ? 1 : 0) + (Math.abs(movement.y) >= minSpeed ? 1 : 0) + (Math.abs(movement.z) >= minSpeed ? 1 : 0);
		return enabled && fastAxes >= 2;
	}

	/** Same steps as Entity.collideWithShapes, with the colliders collected per axis. */
	public static Vec3 collide(Vec3 movement, AABB box, Function<AABB, List<VoxelShape>> colliders) {
		double x = movement.x;
		double y = movement.y;
		double z = movement.z;
		if (y != 0.0) {
			y = Shapes.collide(Direction.Axis.Y, box, colliders.apply(box.expandTowards(0.0, y, 0.0)), y);
			if (y != 0.0) box = box.move(0.0, y, 0.0);
		}
		boolean zFirst = Math.abs(x) < Math.abs(z);
		if (zFirst && z != 0.0) {
			z = Shapes.collide(Direction.Axis.Z, box, colliders.apply(box.expandTowards(0.0, 0.0, z)), z);
			if (z != 0.0) box = box.move(0.0, 0.0, z);
		}
		if (x != 0.0) {
			x = Shapes.collide(Direction.Axis.X, box, colliders.apply(box.expandTowards(x, 0.0, 0.0)), x);
			if (!zFirst && x != 0.0) box = box.move(x, 0.0, 0.0);
		}
		if (!zFirst && z != 0.0) {
			z = Shapes.collide(Direction.Axis.Z, box, colliders.apply(box.expandTowards(0.0, 0.0, z)), z);
		}
		return new Vec3(x, y, z);
	}

	public static void loadConfig(Path configDir) {
		Path file = configDir.resolve(MOD_ID + ".properties");
		Properties props = new Properties();
		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				props.load(reader);
			} catch (IOException e) {
				LOGGER.warn("Could not read {}, using defaults", file, e);
			}
		}
		enabled = Boolean.parseBoolean(props.getProperty("enabled", "true"));
		try {
			minSpeed = Math.max(0.0, Double.parseDouble(props.getProperty("minSpeed", "2.0")));
		} catch (NumberFormatException e) {
			minSpeed = 2.0;
		}
		props.setProperty("enabled", Boolean.toString(enabled));
		props.setProperty("minSpeed", Double.toString(minSpeed));
		try {
			Files.createDirectories(configDir);
			try (Writer writer = Files.newBufferedWriter(file)) {
				props.store(writer, "Collision Lag Fix. enabled: use the per-axis collision check. minSpeed: blocks per tick, on at least two axes, before it is used.");
			}
		} catch (IOException e) {
			LOGGER.warn("Could not write {}", file, e);
		}
	}
}
