package com.leyn13.client.mixin;

import com.leyn13.client.toast.AchievementBannerToast;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Remplace le toast vanilla des progrès (coin haut-droit) par notre bannière
 * animée centrée en haut de l'écran.
 */
@Mixin(ToastManager.class)
public class ToastManagerMixin {

	@Inject(method = "addToast", at = @At("HEAD"), cancellable = true)
	private void betteradvancements$replaceAdvancementToast(Toast toast, CallbackInfo ci) {
		if (toast instanceof AdvancementToast advancementToast) {
			AdvancementHolder holder = ((AdvancementToastAccessor) advancementToast).betteradvancements$getAdvancement();
			((ToastManager) (Object) this).addToast(new AchievementBannerToast(holder));
			ci.cancel();
		}
	}
}
