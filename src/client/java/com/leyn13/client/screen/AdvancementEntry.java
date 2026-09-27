package com.leyn13.client.screen;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.FrameType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.ItemStack;

/**
 * Une entrée de progrès prête à afficher : données extraites de l'arbre
 * vanilla et mises en forme une seule fois (au rebuild), pas à chaque frame.
 */
public record AdvancementEntry(
		Advancement node,
		Component title,
		Component description,
		FrameType type,
		ItemStack icon,
		Advancement root) {
}
