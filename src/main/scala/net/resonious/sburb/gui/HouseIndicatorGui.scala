package net.resonious.sburb.gui

import java.util.ArrayList
import HouseIndicatorGui.bgTex
import HouseIndicatorGui.modalHeight
import HouseIndicatorGui.modalWidth
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import net.minecraft.util.ResourceLocation
import net.resonious.sburb.abstracts.GuiId
import net.resonious.sburb.Sburb
import org.lwjgl.opengl.GL11
import org.lwjgl.input.Keyboard
import net.resonious.sburb.packets.ActivePacket
import net.resonious.sburb.blocks.HouseIndicator
import net.resonious.sburb.game.SburbGame
import net.resonious.sburb.packets.SburbGamePacket
import scala.util.Random

object HouseIndicatorGui extends GuiId {
  val modalWidth = 231
  val modalHeight = 255
  val bgTex = new ResourceLocation("sburb", "textures/gui/house_indicator_bg.png")
}

class HouseIndicatorGui(bx:Int, by:Int, bz:Int) extends GuiScreen {
  var centerX, centerY, modalX, modalY = 0
  //var houseIdField: GuiTextField = null
  
  /*val tileEntity = {
    val plr = Minecraft.getMinecraft.thePlayer
    val wrld = plr.worldObj
    val te = wrld.getTileEntity(bx, by, bz)
    if (te == null) {
      Sburb logError "Couldn't find tile entity at "+bx+", "+by+", "+bz
      null
    }
    else te.asInstanceOf[HouseIndicatorEntity]
  }*/
  
  var clicked = false
  var buttons = new ArrayList[GuiButton]
  buttonList = buttons
  
  override def initGui():Unit = {
    super.initGui()
    centerX = width/2
    centerY = height/2
    modalX = centerX - modalWidth/2
    modalY = centerY - modalHeight/2
    // houseIdField = new GuiTextField(fontRendererObj, modalX+11, modalY+24, 134, 20)
    // if (tileEntity == null) return
    //houseIdField setText tileEntity.houseId
    //buttons add new GuiButton(buttons.size, modalX+81,modalY+53, 63, 21, "Update")
    
    HouseIndicator.availableHouses foreach { house =>
      buttons add new GuiButton(buttons.size, modalX+9, (modalY+24)+(21*buttons.size+5), house)
    }
    if (buttons.size > 0)
    	buttons add new GuiButton(buttons.size, modalX+9, (modalY+24)+(21*buttons.size+10), "DON'T CARE")
    else {
      buttons add new GuiButton(0, modalX+9, (modalY+24), "No houses available. Sorry!")
      clicked = true
    }
  }
  
	override def drawScreen(mouseX:Int, mouseY:Int, f:Float):Unit = {
    GL11.glColor4f(1F, 1F, 1F, 1F);
	  Minecraft.getMinecraft.getTextureManager.bindTexture(bgTex)
	  this.drawTexturedModalRect(modalX, modalY, 0, 0, modalWidth, modalHeight)
	  
	  this.drawString(fontRendererObj, "PICK A HOUSE", modalX+37, modalY+6, 16777215)
	  
	  //houseIdField.drawTextBox()
    super.drawScreen(mouseX, mouseY, f);
	}
	
	override def mouseClicked(par1:Int, par2:Int, par3:Int) = {
	  super.mouseClicked(par1, par2, par3)
	  //houseIdField.mouseClicked(par1, par2, par3)
	}
	
	override def keyTyped(c: Char, i: Int) = {
	  super.keyTyped(c, i)
	  //houseIdField.textboxKeyTyped(c, i)
	  /*if (i == Keyboard.KEY_RETURN) {
	    save()
	  }*/
	}
	
	override def actionPerformed(button: GuiButton) = {
	  if (!clicked) {
  	  clicked = true
  	  val houseId = if (button.id < HouseIndicator.availableHouses.size) {
  	    Sburb log "I WANT "+HouseIndicator.availableHouses(button.id)
  	    button.id
  	  } else
  	  	Random.nextInt(button.id)
  	  
  	  SburbGamePacket.newPlayer send houseId
	  }
	}
	
	def save() = {
	  //tileEntity.houseId = houseIdField.getText
    //tileEntity.markDirty()
    //HouseIndicator.packet.send(bx,by,bz)
	}
  
  override def doesGuiPauseGame() = false
}