package org.jade.hitcooldownsync;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.jade.hitcooldownsync.features.AttackCooldownFix;

public class ModClient implements ClientModInitializer {

	/**
	 * Runs the mod initializer on the client environment.
	 */
	AttackCooldownFix attackCooldownFix;

	@Override
	public void onInitializeClient() {
		attackCooldownFix = new AttackCooldownFix();
		System.out.println("Fix :3");
		ClientTickEvents.END_CLIENT_TICK.register((client) ->{
			if(client.player != null) attackCooldownFix.tick(client.player);
		});
	}
}