package org.jade.hitcooldownsync.features;


import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AttributeDesyncFix {
	private static int lastSlot = -1;
	@NotNull
	private static ItemAttributeModifiers lastItemToServer = ItemAttributeModifiers.EMPTY;
	@NotNull
	private static ItemAttributeModifiers lastItem = ItemAttributeModifiers.EMPTY;
	// Mining speed will be in here later on, speed may not be here because it will be possibly dubious
	private static final List<Holder<Attribute>> AFFECTED_ATTRIBUTES = List.of(
		Attributes.ATTACK_SPEED
	);
	public static Map.Entry<Holder<Attribute>, Identifier> anticipated = null;

	public static void handlePacket(ClientboundUpdateAttributesPacket packet) {
		if (anticipated == null) {
			return;
		}
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || packet.getEntityId() != player.getId()) {
			return;
		}
		int selected = player.getInventory().getSelectedSlot();
		Holder<Attribute> attribute = anticipated.getKey();
		Identifier uuid = anticipated.getValue();

		for (ClientboundUpdateAttributesPacket.AttributeSnapshot value : packet.getValues()) {
			if (!value.attribute().equals(attribute)) {
				continue;
			}
			for (AttributeModifier modifier : value.modifiers()) {
				if (modifier.id().equals(uuid)) {
					anticipated = null;
					lastItemToServer = orElse(player.getInventory()
						.getItem(lastSlot)
						.get(DataComponents.ATTRIBUTE_MODIFIERS));
					return;
				}
			}
		}
		// Well well well
		replaceAttributes(player, lastItemToServer, selected, false);
	}

	public static void tick(LocalPlayer player) {
		int selected = player.getInventory().getSelectedSlot();
		if (lastSlot != selected) {
			replaceAttributes(player, lastItem, selected, true);
			lastSlot = selected;
			lastItem = orElse(player.getInventory()
				.getItem(selected)
				.get(DataComponents.ATTRIBUTE_MODIFIERS));
		}
	}

	private static @NotNull ItemAttributeModifiers orElse(@Nullable ItemAttributeModifiers modifiers) {
		return modifiers == null ? ItemAttributeModifiers.EMPTY : modifiers;
	}

	// I'm not even going to try fixing armor or offhand I'm gonna be honest
	public static void replaceAttributes(
		LocalPlayer player,
		@NotNull ItemAttributeModifiers oldModifiers,
		int newSlot,
		boolean updateAnticipation
	) {
		// Swap modifiers and pray to god there's no exception
		for (Holder<Attribute> attribute : AFFECTED_ATTRIBUTES) {
			var instance = player.getAttributes().getInstance(attribute);
			if (instance == null) {
				return;
			}
			Inventory inv = player.getInventory();

			oldModifiers.forEach(EquipmentSlot.MAINHAND, (attributeHolder, attributeModifier) -> {
				if (attributeHolder != attribute) {
					return;
				}
				instance.removeModifier(attributeModifier);
			});

			ItemAttributeModifiers newModifiers = inv.getNonEquipmentItems().get(newSlot)
				.get(DataComponents.ATTRIBUTE_MODIFIERS);
			if (newModifiers == null) {
				continue;
			}
			newModifiers.forEach(EquipmentSlot.MAINHAND, (attributeHolder, attributeModifier) -> {
				if (attributeHolder != attribute) {
					return;
				}
				if (updateAnticipation && anticipated == null) {
					anticipated = Map.entry(attribute, attributeModifier.id());
				}
				// It can sometimes be double applied and exception
				if (!instance.hasModifier(attributeModifier.id())) {
					instance.addTransientModifier(attributeModifier);
				}
			});
		}
	}
}

