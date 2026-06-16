package org.jade.hitcooldownsync.features;


import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
	private static Multimap<Attribute, AttributeModifier> lastItemToServer = HashMultimap.create();
	private static Multimap<Attribute, AttributeModifier> lastItem = HashMultimap.create();
	// Mining speed will be in here later on, speed may not be here because it will be possibly dubious
	private static final List<Attribute> AFFECTED_ATTRIBUTES = List.of(
		Attributes.ATTACK_SPEED
	);
	@Nullable
	public static Map.Entry<Attribute, UUID> anticipated = null;

	public static void handlePacket(ClientboundUpdateAttributesPacket packet) {
		if (anticipated == null) {
			return;
		}
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || packet.getEntityId() != player.getId()) {
			return;
		}
		int selected = player.getInventory().selected;
		Attribute attribute = anticipated.getKey();
		UUID uuid = anticipated.getValue();

		for (ClientboundUpdateAttributesPacket.AttributeSnapshot value : packet.getValues()) {
			if (!value.getAttribute().equals(attribute)) {
				continue;
			}
			for (AttributeModifier modifier : value.getModifiers()) {
				if (modifier.getId().equals(uuid)) {
					anticipated = null;
					lastItemToServer = player.getInventory().getItem(lastSlot)
						.getAttributeModifiers(EquipmentSlot.MAINHAND);
					return;
				}
			}
		}
		// Well well well
		replaceAttributes(player, lastItemToServer, selected, false);
	}

	public static void tick(LocalPlayer player) {
		int selected = player.getInventory().selected;
		if (lastSlot != selected) {
			replaceAttributes(player, lastItem, selected, true);
			lastSlot = selected;
			lastItem = player.getInventory().getItem(selected)
				.getAttributeModifiers(EquipmentSlot.MAINHAND);
		}
	}

	// I'm not even going to try fixing armor or offhand I'm gonna be honest
	public static void replaceAttributes(
		LocalPlayer player,
		Multimap<Attribute, AttributeModifier> oldModifiers,
		int newSlot,
		boolean updateAnticipation
	) {
		// Swap modifiers and pray to god there's no exception
		for (Attribute attribute : AFFECTED_ATTRIBUTES) {
			var instance = player.getAttributes().getInstance(attribute);
			if (instance == null) {
				return;
			}
			Inventory inv = player.getInventory();

			for (AttributeModifier modifier : oldModifiers.get(attribute)) {
				instance.removeModifier(modifier.getId());
			}

			Collection<AttributeModifier> newModifiers = inv.items.get(newSlot)
				.getAttributeModifiers(EquipmentSlot.MAINHAND)
				.get(attribute);
			for (AttributeModifier modifier : newModifiers) {
				if (updateAnticipation && anticipated == null) {
					anticipated = Map.entry(attribute, modifier.getId());
				}
				// It can sometimes be double applied and exception
				if (!instance.hasModifier(modifier)) {
					instance.addTransientModifier(modifier);
				}
			}
		}
	}
}

