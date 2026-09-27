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
public final class AdvancementEntry {
	private final Advancement node;
	private final Component title;
	private final Component description;
	private final FrameType type;
	private final ItemStack icon;
	private final Advancement root;

	public AdvancementEntry(Advancement node, Component title, Component description, FrameType type,
			ItemStack icon, Advancement root) {
		this.node = node;
		this.title = title;
		this.description = description;
		this.type = type;
		this.icon = icon;
		this.root = root;
	}

	public Advancement node() { return node; }
	public Component title() { return title; }
	public Component description() { return description; }
	public FrameType type() { return type; }
	public ItemStack icon() { return icon; }
	public Advancement root() { return root; }
}
