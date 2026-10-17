package io.github.traelmyx.collisionlagfix.test.fabric;

//? if fabric {
/*import io.github.traelmyx.collisionlagfix.test.ClientSmoke;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class FabricClientSmoke implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		if (ClientSmoke.start()) ClientTickEvents.END_CLIENT_TICK.register(ClientSmoke::tick);
	}
}
*///?}
