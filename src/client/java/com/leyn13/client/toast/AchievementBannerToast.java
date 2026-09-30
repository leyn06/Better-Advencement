package com.leyn13.client.toast;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Bannière animée affichée en haut de l'écran lors du déblocage d'un progrès.
 * Remplace le toast vanilla (coin haut-droit) par une bannière centrée en haut,
 * qui glisse depuis le haut de l'écran puis se retire de la même façon.
 */
public class AchievementBannerToast implements Toast {

	private static final int WIDTH = 240;
	private static final int HEIGHT = 36;
	private static final long ANIM_MS = 350L;
	private static final long DISPLAY_MS = 3500L;

	private static final int COLOR_TOP = 0xFF1B2A4A;
	private static final int COLOR_BOTTOM = 0xFF131F38;
	private static final int COLOR_BORDER = 0xFF3D5A99;
	private static final int COLOR_BORDER_CHALLENGE = 0xFFE6B84C;
	private static final int COLOR_LABEL = 0xFFFFD700;
	private static final int COLOR_LABEL_CHALLENGE = 0xFFE6B84C;
	private static final int COLOR_TITLE = 0xFFE8EAF0;

	private final Component title;
	private final ItemStack icon;
	private final boolean challenge;

	private long startTime = -1L;
	private boolean finished = false;
	private float progress = 0f;
	private long displayDuration = DISPLAY_MS;

	public AchievementBannerToast(AdvancementHolder holder) {
		Advancement advancement = holder.value();
		Optional<DisplayInfo> displayOpt = advancement.display();
		if (displayOpt.isPresent()) {
			DisplayInfo display = displayOpt.get();
			this.title = display.getTitle();
			this.icon = display.getIcon().create();
			this.challenge = display.getType() == AdvancementType.CHALLENGE;
		} else {
			this.title = Component.literal(holder.id().toString());
			this.icon = ItemStack.EMPTY;
			this.challenge = false;
		}
	}

	private static float ease(float t) {
		t = Math.max(0f, Math.min(1f, t));
		return 1f - (1f - t) * (1f - t);
	}

	@Override
	public void update(ToastManager toastManager, long now) {
		if (startTime < 0) {
			startTime = now;
			displayDuration = Math.max(0L, (long) (DISPLAY_MS * toastManager.getNotificationDisplayTimeMultiplier()));
		}
		long elapsed = Math.max(0L, now - startTime);

		if (elapsed < ANIM_MS) {
			progress = ease(elapsed / (float) ANIM_MS);
		} else if (elapsed < ANIM_MS + displayDuration) {
			progress = 1f;
		} else if (elapsed < ANIM_MS * 2 + displayDuration) {
			float t = (elapsed - ANIM_MS - displayDuration) / (float) ANIM_MS;
			progress = 1f - ease(t);
		} else {
			progress = 0f;
			finished = true;
		}
	}

	@Override
	public Visibility getWantedVisibility() {
		return finished ? Visibility.HIDE : Visibility.SHOW;
	}

	@Override
	public SoundEvent getSoundEvent() {
		return challenge ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : null;
	}

	@Override
	public int width() {
		return WIDTH;
	}

	@Override
	public int height() {
		return HEIGHT;
	}

	@Override
	public int occcupiedSlotCount() {
		return (HEIGHT + 31) / 32;
	}

	@Override
	public float xPos(int screenWidth, float partialTick) {
		return (screenWidth - width()) / 2f;
	}

	@Override
	public float yPos(int slotIndex) {
		float baseY = 6 + slotIndex * 32;
		return baseY - (height() + 10) * (1f - progress);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, Font font, long now) {
		int alpha = (int) (255 * progress);
		if (alpha <= 0) {
			return;
		}
		int x = 0;
		int y = 0;

		int top = withAlpha(COLOR_TOP, alpha);
		int bottom = withAlpha(COLOR_BOTTOM, alpha);
		g.fillGradient(x, y, x + WIDTH, y + HEIGHT, top, bottom);

		int border = withAlpha(challenge ? COLOR_BORDER_CHALLENGE : COLOR_BORDER, alpha);
		g.fill(x, y, x + WIDTH, y + 1, border);
		g.fill(x, y + HEIGHT - 1, x + WIDTH, y + HEIGHT, border);
		g.fill(x, y, x + 1, y + HEIGHT, border);
		g.fill(x + WIDTH - 1, y, x + WIDTH, y + HEIGHT, border);

		if (!icon.isEmpty()) {
			g.item(icon, x + 8, y + (HEIGHT - 16) / 2);
		}

		int textX = x + 32;
		String label = challenge ? "\u2726 Défi terminé !" : "\u2726 Progrès débloqué !";
		g.text(font, label, textX, y + 7, withAlpha(challenge ? COLOR_LABEL_CHALLENGE : COLOR_LABEL, alpha), false);
		int availableWidth = WIDTH - textX - 8;
		if (font.width(title) <= availableWidth) {
			g.text(font, title, textX, y + 19, withAlpha(COLOR_TITLE, alpha), false);
		} else {
			String ellipsis = "…";
			String shortTitle = font.plainSubstrByWidth(title.getString(),
					Math.max(0, availableWidth - font.width(ellipsis))) + ellipsis;
			g.text(font, shortTitle, textX, y + 19, withAlpha(COLOR_TITLE, alpha), false);
		}
	}

	private static int withAlpha(int argb, int alpha) {
		return (alpha << 24) | (argb & 0x00FFFFFF);
	}
}
