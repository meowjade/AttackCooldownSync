package org.jade.hitcooldownsync.features;


import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

public class AttributeDesyncFix {
	private static int lastSlot = -1;
	private static int slotToServer = -1;
	// Mining speed will be in here later on, speed may not be here because it will be possibly dubious
	private static final List<Attribute> AFFECTED_ATTRIBUTES = List.of(
		Attributes.ATTACK_SPEED
	);
	@Nullable
	public static UUID anticipatedUUID = null;

	public static void handlePacket(ClientboundUpdateAttributesPacket packet) {
		if (anticipatedUUID == null) {
			return;
		}
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		int selected = player.getInventory().selected;
		for (ClientboundUpdateAttributesPacket.AttributeSnapshot value : packet.getValues()) {
			for (AttributeModifier modifier : value.getModifiers()) {
				if (modifier.getId().equals(anticipatedUUID)) {
					anticipatedUUID = null;
					slotToServer = selected;
					return;
				}
			}
		}
		// Well well well
		onSelect(player, slotToServer, selected);
	}

	public static void tick(LocalPlayer player) {
		int selected = player.getInventory().selected;
		if (lastSlot != selected) {
			onSelect(player, lastSlot, selected);
			lastSlot = selected;
		}
	}

	// I'm not even going to try fixing armor or offhand I'm gonna be honest
	public static void onSelect(LocalPlayer player, int lastSlot, int newSlot) {
		// Swap modifiers and pray to god there's no exception
		for (Attribute attribute : AFFECTED_ATTRIBUTES) {
			var instance = player.getAttributes().getInstance(attribute);
			if (instance == null) {
				return;
			}
			Inventory inv = player.getInventory();

			// Can be -1 because that's the init value
			if (Inventory.isHotbarSlot(lastSlot)) {
				Collection<AttributeModifier> oldModifiers = inv.getItem(lastSlot)
					.getAttributeModifiers(EquipmentSlot.MAINHAND)
					.get(attribute);
				for (AttributeModifier modifier : oldModifiers) {
					instance.removeModifier(modifier.getId());
				}
			}

			Collection<AttributeModifier> newModifiers = inv.items.get(newSlot)
				.getAttributeModifiers(EquipmentSlot.MAINHAND)
				.get(attribute);
			for (AttributeModifier modifier : newModifiers) {
				anticipatedUUID = modifier.getId();
				// It can sometimes be double applied and exception
				if (!instance.hasModifier(modifier)) {
					instance.addTransientModifier(modifier);
				}
			}
		}
	}
}

