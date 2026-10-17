package io.github.traelmyx.collisionlagfix.neoforge;

//? if neoforge {
import io.github.traelmyx.collisionlagfix.CollisionLagFix;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;

@Mod(CollisionLagFix.MOD_ID)
public class CollisionLagFixNeoForge {
	public CollisionLagFixNeoForge() {
		CollisionLagFix.loadConfig(FMLPaths.CONFIGDIR.get());
	}
}
//?}
