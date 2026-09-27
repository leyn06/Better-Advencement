package com.example.client;

import com.leyn13.client.screen.BetterAdvancementsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;

public class ExampleModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Intercepte l'écran des progrès vanilla et le remplace par le nôtre.
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof AdvancementsScreen) {
				ClientPacketListener connection = client.getConnection();
				if (connection != null) {
					client.setScreen(new BetterAdvancementsScreen(connection.getAdvancements()));
				}
			}
		});
	}
}
