package net.resonious.sburb.game.grist

import scala.collection.JavaConversions._
import java.util.ArrayList
import net.minecraft.util.ResourceLocation
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11
import net.minecraft.client.gui.inventory.GuiContainer
import net.resonious.sburb.abstracts.GuiId
import net.minecraft.client.resources.I18n
import net.resonious.sburb.abstracts.Renderer
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.FontRenderer
import net.resonious.sburb.game.SburbProperties
import net.resonious.sburb.Sburb
import net.minecraft.inventory.Slot
import net.resonious.sburb.blocks.GristShopItem
import net.resonious.sburb.packets.SburbGamePacket

object GristShopHud extends Renderer {
  val gristCountBg = new ResourceLocation("sburb", "textures/gui/server_mode_hud_1.png")
  private val player = mc.thePlayer
  val props = SburbProperties of player

  val width = 169
  val height = 148

  override def render(resolution: ScaledResolution) = {
    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F)
    this.mc.getTextureManager.bindTexture(gristCountBg)
    this.drawTexturedModalRect(10, 10, 0, 0, width, height)

    val msg = props.clientPlayerName + "'s build grist"
    val msg2 = "x "+props.serverMode.clientsBuildGrist

    drawString(fontRendererObj, msg, 24, 14, 16777215)
    drawString(fontRendererObj, msg2, 46,33, 16777215)
  }
}

object GristShopGui extends GuiId {
  val iconLocation = new ResourceLocation("sburb", "textures/gui/grist_shop.png")
}

class GristShopGui(plr: EntityPlayer) extends GuiContainer(new GristShopContainer(plr)) {

  private var xSize_lo: Float = 0

  private var ySize_lo: Float = 0

  private val inventory = GristShopInventory

  var buttons = new ArrayList[GuiButton]
  buttonList = buttons

  var topText = "Grist Shop"
  val exitButton = 0

  override def initGui():Unit = {
    super.initGui()

    buttons add new GuiButton(exitButton, guiLeft - 13, guiTop - 25, "Exit server mode")
  }

  override def actionPerformed(button: GuiButton): Unit = {
    button.id match {
      case exitButton => SburbGamePacket.serverMode.deactivate()
    }
  }

  override def drawScreen(par1: Int, par2: Int, par3: Float) {
    super.drawScreen(par1, par2, par3)
    this.xSize_lo = par1.toFloat
    this.ySize_lo = par2.toFloat
  }

  override protected def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) {
    var s = "Grist Shop"
    inventorySlots.inventorySlots foreach {
      case slot: Slot => {
        // stuff don't seem to show up
        val slotX = slot.xDisplayPosition
        val slotY = slot.yDisplayPosition// - 115
        val mX = mouseX - guiLeft
        val mY = mouseY - guiTop

        if (slot.getHasStack()
         && mX >= slotX && mX <= slotX+16
         && mY >= slotY && mY <= slotY+16) {
          val stack = slot.getStack
          val gsItem = GristShopItem of stack.getItem()
          if (gsItem != null)
          	s = stack.getDisplayName + ": " + gsItem.cost + " Build Grist"
          else
            s = "??!!"
        }
      }
    }
    // NOTE final param on these calls are color lol I think
    this.fontRendererObj.drawString(s, this.xSize / 2 - this.fontRendererObj.getStringWidth(s) / 2, 3, 4210752)
    this.fontRendererObj.drawString(I18n.format("container.inventory"), 26, this.ySize - 96 + 4,
      4210752)
  }

  protected def drawGuiContainerBackgroundLayer(par1: Float, par2: Int, par3: Int) {
    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F)
    this.mc.getTextureManager.bindTexture(GristShopGui.iconLocation)
    val k = (this.width - this.xSize) / 2
    val l = (this.height - this.ySize) / 2
    this.drawTexturedModalRect(k, l, 0, 0, this.xSize, this.ySize)
    val i1: Int = 0
  }
}
