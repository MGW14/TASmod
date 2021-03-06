package de.scribble.lp.tasmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import de.scribble.lp.tasmod.gui.GuiMultiplayerWarn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;

@Mixin(GuiMainMenu.class)
public class MixinGuiMainMenu {
	@Redirect(method = "actionPerformed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;displayGuiScreen(Lnet/minecraft/client/gui/GuiScreen;)V", ordinal = 3))
	public void redirectOpenGuiMultiplayer(Minecraft mc) {
		mc.displayGuiScreen(new GuiMultiplayerWarn((GuiMainMenu)(Object)this));
	}
}
