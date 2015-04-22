package net.resonious.sburb.gui

import java.util.ArrayList

import org.lwjgl.opengl.GL11

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import net.resonious.sburb.abstracts.DummyContainer
import net.resonious.sburb.abstracts.GuiId


object ComputerGui extends GuiId

class ComputerGui(player: EntityPlayer) extends GuiScreen {
  // public static final ResourceLocation texture = new ResourceLocation(ModInfo.ID.toLowerCase(), "textures/gui/deployer.png");
  val bgTex = new ResourceLocation("sburb", "textures/gui/computer.png")
  val xSize = 242
  val ySize = 165
  
  var buttons = new ArrayList[GuiButton]
  buttonList = buttons
  
  override def initGui() = {
    super.initGui()
    
    buttons add new GuiButton(2, 72, 72, 80, 20, "WHERE THIS AT")
  }
  
	override def drawScreen(mouseX:Int, mouseY:Int, f:Float):Unit = {
    GL11.glColor4f(1F, 1F, 1F, 1F);
	  
    Minecraft.getMinecraft.getTextureManager.bindTexture(bgTex)
    val k = (width - xSize) / 2
    val l = (height - ySize) / 2
    drawTexturedModalRect(k, l, 0, 0, xSize, ySize)
    
    super.drawScreen(mouseX, mouseY, f);
	}
  
  override def doesGuiPauseGame() = false
}