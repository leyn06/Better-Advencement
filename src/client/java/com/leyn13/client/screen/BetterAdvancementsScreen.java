package com.leyn13.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Écran des progrès amélioré : squelette avec pagination.
 * Étape 1 : structure de base (pages, boutons précédent/suivant).
 * Les progrès eux-mêmes seront dessinés dans les prochaines étapes.
 */
public class BetterAdvancementsScreen extends Screen {

	// Nombre de pages (temporaire, sera calculé selon les progrès plus tard)
	private static final int TOTAL_PAGES = 3;

	private int page = 1;
	private Button previousButton;
	private Button nextButton;

	public BetterAdvancementsScreen() {
		super(Component.translatable("advancements"));
	}

	@Override
	protected void init() {
		previousButton = addRenderableWidget(Button.builder(Component.literal("<"), button -> {
			if (page > 1) {
				page--;
			}
			updateButtons();
		}).bounds(this.width / 2 - 160, this.height - 40, 150, 20).build());

		nextButton = addRenderableWidget(Button.builder(Component.literal(">"), button -> {
			if (page < TOTAL_PAGES) {
				page++;
			}
			updateButtons();
		}).bounds(this.width / 2 + 10, this.height - 40, 150, 20).build());

		updateButtons();
	}

	private void updateButtons() {
		previousButton.active = page > 1;
		nextButton.active = page < TOTAL_PAGES;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		// Fond semi-transparent au lieu du fond vanilla
		guiGraphics.fill(0, 0, this.width, this.height, 0x88000000);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.drawCenteredString(this.font, getTitle(), this.width / 2, 15, 0xFFFFFF);
		guiGraphics.drawCenteredString(this.font,
				Component.literal("Page " + page + " / " + TOTAL_PAGES),
				this.width / 2, this.height - 55, 0xFFFFFF);
	}
}
