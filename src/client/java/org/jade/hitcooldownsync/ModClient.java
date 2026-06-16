package org.jade.hitcooldownsync;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.jade.hitcooldownsync.features.AttributeDesyncFix;

public class ModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		System.out.println("Attack Cooldown Fix :3");
		ClientTickEvents.END_CLIENT_TICK.register((client) -> {
			if (client.player != null) {
				AttributeDesyncFix.tick(client.player);
			}
		});
	}
}