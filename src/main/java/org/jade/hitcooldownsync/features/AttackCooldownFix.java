package org.jade.hitcooldownsync.features;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.jetbrains.annotations.NotNull;

import java.util.*;


public class AttackCooldownFix {
	private static final EquipmentSlot[] SLOTS = {
				EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
	};
	int last_selected = -1;

	public static Multimap<Holder<Attribute>, AttributeModifier> getAttackSpeed(@NotNull Player player) {
		Inventory inv = player.getInventory();
		Multimap<Holder<Attribute>, AttributeModifier>  attack_speed = HashMultimap.create();
		List<ItemStack> items = new ArrayList<>(inv.armor);
		items.add(inv.getSelected());
		items.add(inv.offhand.get(0));
		double base = 0.0;
		double modifier = 0.0;
		double modifier_final = 0.0;

		for (int i = 0; i < 6; ++i) {
			final ItemAttributeModifiers itemAttributeModifiers = items.get(i).getComponents().get(DataComponents.ATTRIBUTE_MODIFIERS);
			if (itemAttributeModifiers == null) continue;
			
			final List<ItemAttributeModifiers.Entry> modifiers = itemAttributeModifiers.modifiers();
			for (ItemAttributeModifiers.Entry a : modifiers) {
				if (a.attribute().equals(Attributes.ATTACK_SPEED) && a.modifier().operation().equals(AttributeModifier.Operation.ADD_VALUE)) base += a.modifier().amount();
			}
		}
		attack_speed.put(
					Attributes.ATTACK_SPEED,
					new AttributeModifier(nonsenseResourceLocation(), base, AttributeModifier.Operation.ADD_VALUE)
		);

		for (int i = 0; i < 6; ++i) {
			final ItemAttributeModifiers itemAttributeModifiers = items.get(i).getComponents().get(DataComponents.ATTRIBUTE_MODIFIERS);
			if (itemAttributeModifiers == null) continue;

			final List<ItemAttributeModifiers.Entry> modifiers = itemAttributeModifiers.modifiers();
			for (ItemAttributeModifiers.Entry a : modifiers) {
				if (a.attribute().equals(Attributes.ATTACK_SPEED) && a.modifier().operation().equals(AttributeModifier.Operation.ADD_MULTIPLIED_BASE)) modifier += a.modifier().amount();
			}
		}

		for (int i = 0; i < 6; ++i) {
			final ItemAttributeModifiers itemAttributeModifiers = items.get(i).getComponents().get(DataComponents.ATTRIBUTE_MODIFIERS);
			if (itemAttributeModifiers == null) continue;

			final List<ItemAttributeModifiers.Entry> modifiers = itemAttributeModifiers.modifiers();
			for (ItemAttributeModifiers.Entry a : modifiers) {
				if (a.attribute().equals(Attributes.ATTACK_SPEED) && a.modifier().operation().equals(AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)) modifier_final += a.modifier().amount();
			}
		}
		attack_speed.put(
					Attributes.ATTACK_SPEED,
					new AttributeModifier(nonsenseResourceLocation(), modifier, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
		);
		attack_speed.put(
					Attributes.ATTACK_SPEED,
					new AttributeModifier(nonsenseResourceLocation(), modifier_final, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
		);

		if (player.hasEffect(MobEffects.DIG_SPEED)) attack_speed.put(
					Attributes.ATTACK_SPEED,
					new AttributeModifier(nonsenseResourceLocation(), (Objects.requireNonNull(player.getEffect(MobEffects.DIG_SPEED)).getAmplifier() + 1) * .1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
		);

		if (player.hasEffect(MobEffects.CONDUIT_POWER)) attack_speed.put(
					Attributes.ATTACK_SPEED,
					new AttributeModifier(nonsenseResourceLocation(), (Objects.requireNonNull(player.getEffect(MobEffects.CONDUIT_POWER)).getAmplifier() + 1) * .1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
		);

		return attack_speed;
	}

	private static @NotNull ResourceLocation nonsenseResourceLocation() {
		return ResourceLocation.withDefaultNamespace(UUID.randomUUID().toString());
	}

	public void tick(@NotNull Player player) {
		Inventory inv = player.getInventory();
		if (last_selected != inv.selected) {
			last_selected = inv.selected;
			Multimap<Holder<Attribute>, AttributeModifier> attack_speed = getAttackSpeed(player);
			AttributeMap attributeMap = player.getAttributes();

			Set<AttributeModifier> mods = Objects.requireNonNull(attributeMap.getInstance(Attributes.ATTACK_SPEED)).getModifiers();
			Multimap<Holder<Attribute>, AttributeModifier> remove = ArrayListMultimap.create();
			mods.forEach(attributeModifier -> remove.put(Attributes.ATTACK_SPEED, attributeModifier));
			attributeMap.removeAttributeModifiers(remove);
			attributeMap.addTransientAttributeModifiers(attack_speed);
		}
	}
}

