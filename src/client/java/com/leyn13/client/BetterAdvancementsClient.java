package com.leyn13.client;

import com.leyn13.client.screen.BetterAdvancementsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

public class BetterAdvancementsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Intercepte l'écran des progrès vanilla et le remplace par le nôtre.
		// On compare le nom de la classe plutôt que son type : le nom de l'écran
		// vanilla peut varier selon les versions (AdvancementsScreen/AdvancementScreen).
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			String className = screen.getClass().getSimpleName();
			if (className.equals("AdvancementsScreen") || className.equals("AdvancementScreen")) {
				client.setScreen(new BetterAdvancementsScreen());
			}
		});
	}
}
