package io.github.traelmyx.collisionlagfix.mixin;

import io.github.traelmyx.collisionlagfix.CollisionLagFix;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Entity.class)
public abstract class EntityMixin {
	@Shadow
	public abstract AABB getBoundingBox();

	@Shadow
	public abstract Level level();

	@Shadow
	public abstract float maxUpStep();

	@Shadow
	public abstract boolean onGround();

	//? if >=26 {
	/*@Shadow
	private static List<VoxelShape> collectCollidersIgnoringWorldBorder(Entity entity, Level level, List<VoxelShape> entityShapes, AABB box) {
		throw new AssertionError();
	}
	*///?} else {
	@Shadow
	private static List<VoxelShape> collectColliders(Entity entity, Level level, List<VoxelShape> entityShapes, AABB box) {
		throw new AssertionError();
	}
	//?}

	// HEAD of collide rather than collideBoundingBox: Lithium redirects the call inside collide to its own sweep
	@Inject(method = "collide", at = @At("HEAD"), cancellable = true)
	private void collisionlagfix$collidePerAxis(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
		if (!CollisionLagFix.isFast(movement)) return;
		Entity self = (Entity) (Object) this;
		Level level = this.level();
		AABB box = this.getBoundingBox();
		// vanilla only counts the world border when it is closer than twice the movement box; near it, keep vanilla
		AABB swept = box.expandTowards(movement);
		if (level.getWorldBorder().getDistanceToBorder(self) < 2.0 * Math.max(Math.max(swept.getXsize(), swept.getZsize()), 1.0) + 1.0) return;
		List<VoxelShape> entityShapes = level.getEntityCollisions(self, swept);
		//? if >=26 {
		/*Vec3 result = CollisionLagFix.collide(movement, box, slab -> collectCollidersIgnoringWorldBorder(self, level, entityShapes, slab));
		*///?} else
		Vec3 result = CollisionLagFix.collide(movement, box, slab -> collectColliders(self, level, entityShapes, slab));
		boolean blockedHorizontally = result.x != movement.x || result.z != movement.z;
		boolean grounded = this.onGround() || result.y != movement.y && movement.y < 0.0;
		// stepping up onto a block: leave it to vanilla, which this rarely reaches at these speeds
		if (this.maxUpStep() > 0.0F && grounded && blockedHorizontally) return;
		CollisionLagFix.perAxisMoves++;
		cir.setReturnValue(result);
	}

	//? if <1.21.11 {
	/**
	 * MC-263882: before 1.21.11, an entity with any fall distance traces a ray along its whole movement every tick to see
	 * whether it passes water or cobwebs, which for cannon TNT means a ray of a hundred blocks or more per tick. TNT can't
	 * take fall damage, so for TNT without passengers the trace changes nothing; skip it. Newer versions cap it at 8 blocks.
	 */
	@com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;"))
	private net.minecraft.world.phys.BlockHitResult collisionlagfix$skipTntFallTrace(Level level, net.minecraft.world.level.ClipContext context,
		com.llamalad7.mixinextras.injector.wrapoperation.Operation<net.minecraft.world.phys.BlockHitResult> original) {
		Entity self = (Entity) (Object) this;
		if (CollisionLagFix.enabled && CollisionLagFix.skipTntFallTrace && self instanceof net.minecraft.world.entity.item.PrimedTnt && !self.isVehicle())
			return net.minecraft.world.phys.BlockHitResult.miss(self.position(), net.minecraft.core.Direction.DOWN, self.blockPosition());
		return original.call(level, context);
	}
	//?}
}
