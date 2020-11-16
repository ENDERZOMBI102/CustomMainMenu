package com.enderzombi102.cmm.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

	@Shadow
	public Screen currentScreen;

	@Shadow
	public ClientWorld world;

	@Shadow
	public ClientPlayerEntity player;


	@Final
	@Shadow
	public Mouse mouse;

	@Final
	@Shadow
	public GameOptions options;

	@Final
	@Shadow
	public InGameHud inGameHud;

	@Final
	@Shadow
	private SoundManager soundManager;

	@Final
	@Shadow
	private Window window;

	@Shadow
	public boolean skipGameRender;

	@Shadow
	public void updateWindowTitle() {

	}

	@Shadow @Final private static Logger LOGGER;

	/**
	 * @author ENDERZOMBI102
	 */
	@Overwrite(  )
	public void openScreen( @Nullable Screen screen ) {
		LOGGER.info("ENDER'S EDIT!");
		if (this.currentScreen != null) {
			this.currentScreen.removed();
		}

		if (screen == null && this.world == null) {
			screen = new TitleScreen();
		} else if (screen == null && this.player.isDead()) {
			if (this.player.showsDeathScreen()) {
				screen = new DeathScreen(null, this.world.getLevelProperties().isHardcore() );
			} else {
				this.player.requestRespawn();
			}
		}

		if (screen instanceof TitleScreen || screen instanceof MultiplayerScreen) {
			this.options.debugEnabled = false;
			this.inGameHud.getChatHud().clear(true);
		}

		this.currentScreen = screen;
		if (screen != null) {
			this.mouse.unlockCursor();
			KeyBinding.unpressAll();
			screen.init( MinecraftClient.getInstance(), this.window.getScaledWidth(), this.window.getScaledHeight());
			this.skipGameRender = false;
			NarratorManager.INSTANCE.narrate( screen.getNarrationMessage() );
		} else {
			this.soundManager.resumeAll();
			this.mouse.lockCursor();
		}

		this.updateWindowTitle();
	}

}
