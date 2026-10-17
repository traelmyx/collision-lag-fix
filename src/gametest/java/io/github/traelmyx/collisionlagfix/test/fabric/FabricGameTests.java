package io.github.traelmyx.collisionlagfix.test.fabric;

//? if fabric {
/*import io.github.traelmyx.collisionlagfix.test.CollisionTests;
import net.minecraft.gametest.framework.GameTestHelper;
//? if >=1.21.5 {
import net.fabricmc.fabric.api.gametest.v1.GameTest;
//?} else {
/^import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
^///?}

//? if >=1.21.5 {
public class FabricGameTests {
	private static final String EMPTY = "collision_lag_fix_gametest:empty";

	@GameTest(structure = EMPTY, maxTicks = 2400)
	public void matchesVanilla(GameTestHelper helper) {
		CollisionTests.matchesVanilla(helper);
	}

	@GameTest(structure = EMPTY, maxTicks = 2400)
	public void fastTntIsCheap(GameTestHelper helper) {
		CollisionTests.fastTntIsCheap(helper);
	}

	@GameTest(structure = EMPTY, maxTicks = 200)
	public void fallingTntSkipsTrace(GameTestHelper helper) {
		CollisionTests.fallingTntSkipsTrace(helper);
	}
}
//?} else {
/^public class FabricGameTests implements FabricGameTest {
	private static final String EMPTY = "collision_lag_fix_gametest:empty";

	@GameTest(template = EMPTY, timeoutTicks = 2400)
	public void matchesVanilla(GameTestHelper helper) {
		CollisionTests.matchesVanilla(helper);
	}

	@GameTest(template = EMPTY, timeoutTicks = 2400)
	public void fastTntIsCheap(GameTestHelper helper) {
		CollisionTests.fastTntIsCheap(helper);
	}

	@GameTest(template = EMPTY, timeoutTicks = 200)
	public void fallingTntSkipsTrace(GameTestHelper helper) {
		CollisionTests.fallingTntSkipsTrace(helper);
	}
}
^///?}
*///?}
