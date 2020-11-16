package com.enderzombi102.cmm.client.gui;

import com.google.common.util.concurrent.Runnables;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.CubeMapRenderer;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screen.options.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.options.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.realms.gui.screen.RealmsBridgeScreen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelSummary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecated")
@Environment(EnvType.CLIENT)
public class MainMenu extends Screen {

	private static final Logger logger = LogManager.getLogger();
	public static final CubeMapRenderer PANORAMA_CUBE_MAP = new CubeMapRenderer(new Identifier("textures/gui/title/background/panorama"));
	private static final Identifier PANORAMA_OVERLAY = new Identifier("textures/gui/title/background/panorama_overlay.png");
	private static final Identifier ACCESSIBILITY_ICON_TEXTURE = new Identifier("textures/gui/accessibility.png");
	private final boolean isMinceraft;
	@Nullable
	private String splashText;
	private ButtonWidget buttonResetDemo;
	private static final Identifier MINECRAFT_TITLE_TEXTURE = new Identifier("textures/gui/title/minecraft.png");
	private static final Identifier EDITION_TITLE_TEXTURE = new Identifier("textures/gui/title/edition.png");
	private boolean realmsNotificationsInitialized;
	private Screen realmsNotificationGui;
	private int copyrightTextWidth;
	private int copyrightTextX;
	private final RotatingCubeMapRenderer backgroundRenderer;
	private final boolean doBackgroundFade;
	private long backgroundFadeStart;

	public static MainMenu instance;


	public MainMenu() {
		this(false);
		instance = this;
	}

	public MainMenu(boolean doBackgroundFade) {
		super(new TranslatableText("narrator.screen.title"));
		this.backgroundRenderer = new RotatingCubeMapRenderer(PANORAMA_CUBE_MAP);
		this.doBackgroundFade = doBackgroundFade;
		this.isMinceraft = (double)(new Random()).nextFloat() < 1.0E-4D;
		instance = this;
	}

	private boolean areRealmsNotificationsEnabled() {
		return this.client.options.realmsNotifications && this.realmsNotificationGui != null;
	}

	public void tick() {
		if (this.areRealmsNotificationsEnabled()) {
			this.realmsNotificationGui.tick();
		}

	}

	public static CompletableFuture<Void> loadTexturesAsync(TextureManager textureManager, Executor executor) {
		return CompletableFuture.allOf(textureManager.loadTextureAsync(MINECRAFT_TITLE_TEXTURE, executor), textureManager.loadTextureAsync(EDITION_TITLE_TEXTURE, executor), textureManager.loadTextureAsync(PANORAMA_OVERLAY, executor), PANORAMA_CUBE_MAP.loadTexturesAsync(textureManager, executor));
	}

	public boolean isPauseScreen() {
		return false;
	}

	public boolean shouldCloseOnEsc() {
		return false;
	}

	public void init() {
		if (this.splashText == null) {
			this.splashText = this.client.getSplashTextLoader().get();
		}

		this.copyrightTextWidth = this.textRenderer.getWidth("Copyright Mojang AB. Do not distribute!");
		this.copyrightTextX = this.width - this.copyrightTextWidth - 2;
		int j = this.height / 4 + 48;
		if (this.client.isDemo()) {
			this.initWidgetsDemo(j, 24);
		} else {
			this.initWidgetsNormal(j, 24);
		}

		this.addButton(new TexturedButtonWidget(this.width / 2 - 124, j + 72 + 12, 20, 20, 0, 106, 20, ButtonWidget.WIDGETS_LOCATION, 256, 256, (buttonWidget) -> {
			this.client.openScreen(new LanguageOptionsScreen(this, this.client.options, this.client.getLanguageManager()));
		}, new TranslatableText("narrator.button.language")));
		this.addButton(new ButtonWidget(this.width / 2 - 100, j + 72 + 12, 98, 20, new TranslatableText("menu.options"), (buttonWidget) -> {
			this.client.openScreen(new OptionsScreen(this, this.client.options));
		}));
		this.addButton(new ButtonWidget(this.width / 2 + 2, j + 72 + 12, 98, 20, new TranslatableText("menu.quit"), (buttonWidget) -> {
			this.client.scheduleStop();
		}));
		this.addButton(new TexturedButtonWidget(this.width / 2 + 104, j + 72 + 12, 20, 20, 0, 0, 20, ACCESSIBILITY_ICON_TEXTURE, 32, 64, (buttonWidget) -> {
			this.client.openScreen(new AccessibilityOptionsScreen(this, this.client.options));
		}, new TranslatableText("narrator.button.accessibility")));
		this.client.setConnectedToRealms(false);
		if (this.client.options.realmsNotifications && !this.realmsNotificationsInitialized) {
			RealmsBridgeScreen realmsBridgeScreen = new RealmsBridgeScreen();
			this.realmsNotificationGui = realmsBridgeScreen.getNotificationScreen(this);
			this.realmsNotificationsInitialized = true;
		}

		if (this.areRealmsNotificationsEnabled()) {
			this.realmsNotificationGui.init(this.client, this.width, this.height);
		}

	}

	private void initWidgetsNormal(int y, int spacingY) {
		this.addButton(new ButtonWidget(this.width / 2 - 100, y, 200, 20, new TranslatableText("menu.singleplayer"), (buttonWidget) -> {
			this.client.openScreen(new SelectWorldScreen(this));
		}));
		boolean bl = this.client.isMultiplayerEnabled();
		ButtonWidget.TooltipSupplier tooltipSupplier = bl ? ButtonWidget.EMPTY : (buttonWidget, matrixStack, i, j) -> {
			if (!buttonWidget.active) {
				this.renderOrderedTooltip(matrixStack, this.client.textRenderer.wrapLines(new TranslatableText("title.multiplayer.disabled"), Math.max(this.width / 2 - 43, 170)), i, j);
			}

		};
		this.addButton(new ButtonWidget(this.width / 2 - 100, y + spacingY * 1, 200, 20, new TranslatableText("menu.multiplayer"), (buttonWidget) -> {
			Screen screen = this.client.options.skipMultiplayerWarning ? new MultiplayerScreen(this) : new MultiplayerWarningScreen(this);
			this.client.openScreen(screen);
		}, tooltipSupplier)).active = bl;
		this.addButton(new ButtonWidget(this.width / 2 - 100, y + spacingY * 2, 200, 20, new TranslatableText("menu.online"), (buttonWidget) -> {
			this.switchToRealms();
		}, tooltipSupplier)).active = bl;
	}

	private void initWidgetsDemo(int y, int spacingY) {
		boolean bl = this.createDemoWorld();
		this.addButton(new ButtonWidget(this.width / 2 - 100, y, 200, 20, new TranslatableText("menu.playdemo"), (buttonWidget) -> {
			if (bl) {
				this.client.startIntegratedServer("Demo_World");
			} else {
				DynamicRegistryManager.Impl impl = DynamicRegistryManager.create();
				this.client.method_29607("Demo_World", MinecraftServer.DEMO_LEVEL_INFO, impl, GeneratorOptions.method_31112(impl));
			}

		}));
		this.buttonResetDemo = this.addButton(new ButtonWidget(this.width / 2 - 100, y + spacingY, 200, 20, new TranslatableText("menu.resetdemo"), (buttonWidget) -> {
			LevelStorage levelStorage = this.client.getLevelStorage();

			try {
				LevelStorage.Session session = levelStorage.createSession("Demo_World");
				Throwable var4 = null;

				try {
					LevelSummary levelSummary = session.getLevelSummary();
					if (levelSummary != null) {
						this.client.openScreen(new ConfirmScreen(this::onDemoDeletionConfirmed, new TranslatableText("selectWorld.deleteQuestion"), new TranslatableText("selectWorld.deleteWarning", levelSummary.getDisplayName()), new TranslatableText("selectWorld.deleteButton"), ScreenTexts.CANCEL));
					}
				} catch (Throwable var14) {
					var4 = var14;
					throw var14;
				} finally {
					if (session != null) {
						if (var4 != null) {
							try {
								session.close();
							} catch (Throwable var13) {
								var4.addSuppressed(var13);
							}
						} else {
							session.close();
						}
					}

				}
			} catch (IOException var16) {
				SystemToast.addWorldAccessFailureToast(this.client, "Demo_World");
				logger.warn("Failed to access demo world", var16);
			}

		}));
		this.buttonResetDemo.active = bl;
	}

	private boolean createDemoWorld() {
		try {
			LevelStorage.Session session = this.client.getLevelStorage().createSession("Demo_World");
			Throwable var2 = null;

			boolean var3;
			try {
				var3 = session.getLevelSummary() != null;
			} catch (Throwable var13) {
				var2 = var13;
				throw var13;
			} finally {
				if (session != null) {
					if (var2 != null) {
						try {
							session.close();
						} catch (Throwable var12) {
							var2.addSuppressed(var12);
						}
					} else {
						session.close();
					}
				}

			}

			return var3;
		} catch (IOException err) {
			SystemToast.addWorldAccessFailureToast(this.client, "Demo_World");
			logger.warn("Failed to read demo world data", err);
			return false;
		}
	}

	private void switchToRealms() {
		RealmsBridgeScreen realmsBridgeScreen = new RealmsBridgeScreen();
		realmsBridgeScreen.switchToRealms(this);
	}

	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		if (this.backgroundFadeStart == 0L && this.doBackgroundFade) {
			this.backgroundFadeStart = Util.getMeasuringTimeMs();
		}

		float f = this.doBackgroundFade ? (float)(Util.getMeasuringTimeMs() - this.backgroundFadeStart) / 1000.0F : 1.0F;
		fill(matrices, 0, 0, this.width, this.height, -1);
		this.backgroundRenderer.render(delta, MathHelper.clamp(f, 0.0F, 1.0F));
		int j = this.width / 2 - 137;
		this.client.getTextureManager().bindTexture(PANORAMA_OVERLAY);
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.doBackgroundFade ? (float)MathHelper.ceil(MathHelper.clamp(f, 0.0F, 1.0F)) : 1.0F);
		drawTexture(matrices, 0, 0, this.width, this.height, 0.0F, 0.0F, 16, 128, 16, 128);
		float g = this.doBackgroundFade ? MathHelper.clamp(f - 1.0F, 0.0F, 1.0F) : 1.0F;
		int l = MathHelper.ceil(g * 255.0F) << 24;
		if ((l & -67108864) != 0) {
			this.client.getTextureManager().bindTexture(MINECRAFT_TITLE_TEXTURE);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, g);
			if (this.isMinceraft) {
				this.method_29343(j, 30, (integer, integer2) -> {
					this.drawTexture(matrices, integer + 0, integer2, 0, 0, 99, 44);
					this.drawTexture(matrices, integer + 99, integer2, 129, 0, 27, 44);
					this.drawTexture(matrices, integer + 99 + 26, integer2, 126, 0, 3, 44);
					this.drawTexture(matrices, integer + 99 + 26 + 3, integer2, 99, 0, 26, 44);
					this.drawTexture(matrices, integer + 155, integer2, 0, 45, 155, 44);
				});
			} else {
				this.method_29343(j, 30, (integer, integer2) -> {
					this.drawTexture(matrices, integer + 0, integer2, 0, 0, 155, 44);
					this.drawTexture(matrices, integer + 155, integer2, 0, 45, 155, 44);
				});
			}

			this.client.getTextureManager().bindTexture(EDITION_TITLE_TEXTURE);
			drawTexture(matrices, j + 88, 67, 0.0F, 0.0F, 98, 14, 128, 16);
			if (this.splashText != null) {
				RenderSystem.pushMatrix();
				RenderSystem.translatef((float)(this.width / 2 + 90), 70.0F, 0.0F);
				RenderSystem.rotatef(-20.0F, 0.0F, 0.0F, 1.0F);
				float h = 1.8F - MathHelper.abs(MathHelper.sin((float)(Util.getMeasuringTimeMs() % 1000L) / 1000.0F * 6.2831855F) * 0.1F);
				h = h * 100.0F / (float)(this.textRenderer.getWidth(this.splashText) + 32);
				RenderSystem.scalef(h, h, h);
				drawCenteredString(matrices, this.textRenderer, this.splashText, 0, -8, 16776960 | l);
				RenderSystem.popMatrix();
			}

			String string = "Minecraft " + SharedConstants.getGameVersion().getName();
			if (this.client.isDemo()) {
				string = string + " Demo";
			} else {
				string = string + ("release".equalsIgnoreCase(this.client.getVersionType()) ? "" : "/" + this.client.getVersionType());
			}

			if (this.client.isModded()) {
				string = string + I18n.translate("menu.modded");
			}

			drawStringWithShadow(matrices, this.textRenderer, string, 2, this.height - 10, 16777215 | l);
			drawStringWithShadow(matrices, this.textRenderer, "Copyright Mojang AB. Do not distribute! or not?", this.copyrightTextX, this.height - 10, 16777215 | l);
			if (mouseX > this.copyrightTextX && mouseX < this.copyrightTextX + this.copyrightTextWidth && mouseY > this.height - 10 && mouseY < this.height) {
				fill(matrices, this.copyrightTextX, this.height - 1, this.copyrightTextX + this.copyrightTextWidth, this.height, 16777215 | l);
			}

			for (AbstractButtonWidget abstractButtonWidget : this.buttons) {
				abstractButtonWidget.setAlpha(g);
			}

			super.render(matrices, mouseX, mouseY, delta);
			if (this.areRealmsNotificationsEnabled() && g >= 1.0F) {
				this.realmsNotificationGui.render(matrices, mouseX, mouseY, delta);
			}

		}
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		} else if (this.areRealmsNotificationsEnabled() && this.realmsNotificationGui.mouseClicked(mouseX, mouseY, button)) {
			return true;
		} else {
			if (mouseX > (double)this.copyrightTextX && mouseX < (double)(this.copyrightTextX + this.copyrightTextWidth) && mouseY > (double)(this.height - 10) && mouseY < (double)this.height) {
				this.client.openScreen(new CreditsScreen(false, Runnables.doNothing() ) );
			}

			return false;
		}
	}

	public void removed() {
		if (this.realmsNotificationGui != null) {
			this.realmsNotificationGui.removed();
		}

	}

	private void onDemoDeletionConfirmed(boolean delete) {
		if (delete) {
			try {
				LevelStorage.Session session = this.client.getLevelStorage().createSession("Demo_World");
				Throwable var3 = null;

				try {
					session.deleteSessionLock();
				} catch (Throwable var13) {
					var3 = var13;
					throw var13;
				} finally {
					if (session != null) {
						if (var3 != null) {
							try {
								session.close();
							} catch (Throwable var12) {
								var3.addSuppressed(var12);
							}
						} else {
							session.close();
						}
					}

				}
			} catch (IOException err) {
				SystemToast.addWorldDeleteFailureToast(this.client, "Demo_World");
				logger.warn("Failed to delete demo world", err);
			}
		}

		this.client.openScreen(this);
	}
}
