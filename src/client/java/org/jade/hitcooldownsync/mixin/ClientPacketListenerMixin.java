package org.jade.hitcooldownsync.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import org.jade.hitcooldownsync.features.AttributeDesyncFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin extends ClientCommonPacketListenerImpl {
	protected ClientPacketListenerMixin(Minecraft minecraft, Connection connection, CommonListenerCookie commonListenerCookie) {
		super(minecraft, connection, commonListenerCookie);
	}

	@Inject(method = "handleUpdateAttributes", at = @At("TAIL"))
	public void dontHandleAttributePacketTestingDont(ClientboundUpdateAttributesPacket packet, CallbackInfo ci) {
		AttributeDesyncFix.handlePacket(packet);
	}

}
