package org.jade.hitcooldownsync.features;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;


public class AttackCooldownFix {
	private static final EquipmentSlot[] SLOTS = {
				EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
	};
	int last_selected = -1;

	public static Multimap<Attribute, AttributeModifier> getAttackSpeed(@NotNull Player player) {
		Inventory inv = player.getInventory();
		Multimap<Attribute, AttributeModifier>  attack_speed = HashMultimap.create();
		List<ItemStack> items = new ArrayList<>(inv.armor);
		items.add(inv.getSelected());
		items.add(inv.offhand.get(0));
		double base = 0.0;
		double modifier = 0.0;
		double modifier_final = 0.0;

		for (int i = 0; i < 6; ++i) {
			Multimap<Attribute, AttributeModifier> mod = items.get(i).getAttributeModifiers(SLOTS[i]);
			for (Map.Entry<Attribute, AttributeModifier> a : mod.entries()) {
				if (a.getKey().equals(Attributes.ATTACK_SPEED) && a.getValue().getOperation().equals(AttributeModifier.Operation.ADDITION)) base += a.getValue().getAmount();
			}
		}
		attack_speed.put(
					Attributes.ATTACK_SPEED,
					new AttributeModifier(UUID.randomUUID().toString(), base, AttributeModifier.Operation.ADDITION)
		);

		for (int i = 0; i < 6; ++i) {
			Multimap<Attribute, AttributeModifier> mod = items.get(i).getAttributeModifiers(SLOTS[i]);
			for (Map.Entry<Attribute, AttributeModifier> a : mod.entries()) {
				if (a.getKey().equals(Attributes.ATTACK_SPEED) && a.getValue().getOperation().equals(AttributeModifier.Operation.MULTIPLY_BASE))
					modifier += a.getValue().getAmount();
			}
		}

		for (int i = 0; i < 6; ++i) {
			Multimap<Attribute, AttributeModifier> mod = items.get(i).getAttributeModifiers(SLOTS[i]);
			for (Map.Entry<Attribute, AttributeModifier> a : mod.entries()) {
				if (a.getKey().equals(Attributes.ATTACK_SPEED) && a.getValue().getOperation().equals(AttributeModifier.Operation.MULTIPLY_TOTAL))
					modifier_final += a.getValue().getAmount();
			}
		}
		attack_speed.put(
					Attributes.ATTACK_SPEED,
					new AttributeModifier(UUID.randomUUID().toString(), modifier, AttributeModifier.Operation.MULTIPLY_BASE)
		);
		attack_speed.put(
					Attributes.ATTACK_SPEED,
					new AttributeModifier(UUID.randomUUID().toString(), modifier_final, AttributeModifier.Operation.MULTIPLY_TOTAL)
		);

		if (player.hasEffect(MobEffects.DIG_SPEED)) attack_speed.put(
					Attributes.ATTACK_SPEED,
					new AttributeModifier(UUID.randomUUID().toString(), (Objects.requireNonNull(player.getEffect(MobEffects.DIG_SPEED)).getAmplifier() + 1) * .1, AttributeModifier.Operation.MULTIPLY_TOTAL)
		);

		if (player.hasEffect(MobEffects.CONDUIT_POWER)) attack_speed.put(
					Attributes.ATTACK_SPEED,
					new AttributeModifier(UUID.randomUUID().toString(), (Objects.requireNonNull(player.getEffect(MobEffects.CONDUIT_POWER)).getAmplifier() + 1) * .1, AttributeModifier.Operation.MULTIPLY_TOTAL)
		);

		return attack_speed;
	}

	public void tick(@NotNull Player player) {
		Inventory inv = player.getInventory();
		if (last_selected != inv.selected) {
			last_selected = inv.selected;
			Multimap<Attribute, AttributeModifier> attack_speed = getAttackSpeed(player);
			AttributeMap attributeMap = player.getAttributes();

			Set<AttributeModifier> mods = Objects.requireNonNull(attributeMap.getInstance(Attributes.ATTACK_SPEED)).getModifiers();
			Multimap<Attribute, AttributeModifier> remove = ArrayListMultimap.create();
			mods.forEach(attributeModifier -> remove.put(Attributes.ATTACK_SPEED, attributeModifier));
			attributeMap.removeAttributeModifiers(remove);
			attributeMap.addTransientAttributeModifiers(attack_speed);
		}
	}
}

