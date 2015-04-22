package net.resonious.sburb.abstracts

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.ScaledResolution

// Used for HUD stuff (not guiscreen)
abstract class Renderer extends Gui {
  protected val mc = Minecraft.getMinecraft
  protected val fontRendererObj = mc.fontRenderer
	def render(resolution: ScaledResolution)
}