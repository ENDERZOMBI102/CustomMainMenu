package com.enderzombi102.cmm;

import com.enderzombi102.cmm.client.gui.MainMenu;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;

public class CustomMainMenu implements ModInitializer, ClientModInitializer {

	@Override
	public void onInitialize() {
	}

	@Override
	public void onInitializeClient() {
		new MainMenu();
	}
}
