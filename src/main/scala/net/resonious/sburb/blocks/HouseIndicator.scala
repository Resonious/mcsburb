package net.resonious.sburb.blocks

import net.resonious.sburb.abstracts.ActiveBlock
import net.minecraft.block.material.Material
import net.resonious.sburb.gui.ComputerGui
import net.minecraft.entity.player.EntityPlayer
import net.resonious.sburb.Sburb
import net.minecraft.world.World
import net.resonious.sburb.gui.HouseIndicatorGui
import net.resonious.sburb.abstracts.ActiveTileEntity
import net.minecraft.nbt.NBTTagCompound
import net.resonious.sburb.packets.ActivePacket
import io.netty.buffer.ByteBuf
import net.resonious.sburb.abstracts.PacketPipeline
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side
import net.minecraft.client.Minecraft
import net.resonious.sburb.abstracts.Pimp._
import cpw.mods.fml.common.network.ByteBufUtils
import net.minecraft.entity.player.EntityPlayerMP
import net.resonious.sburb.game.SburbGame
import scala.collection.mutable.ArrayBuffer

// Might not need?
// Scratch that. need.
// SCRATCH THAT. KILLIING IT ALL.

/*class HouseIndicatorUpdatePacket extends ActivePacket {
  var x, y, z = 0
  var id = ""
  var openGuiOnClient = false
  
  @SideOnly(Side.CLIENT)
  def send(_x:Int,_y:Int,_z:Int) = {
    x=_x;y=_y;z=_z
    val plr = Minecraft.getMinecraft.thePlayer
    val hi = HouseIndicatorEntity.at(plr.worldObj, x, y, z)
    id = hi.houseId
    openGuiOnClient = false
    PacketPipeline.sendToServer(this)
    PacketPipeline.sendToDimension(this, plr.dimension)
  }
  
  @SideOnly(Side.SERVER)
  def send(plr: EntityPlayer, _x:Int,_y:Int,_z:Int) = {
    x=_x;y=_y;z=_z
    val hi = HouseIndicatorEntity.at(plr.worldObj, x, y, z)
    id = hi.houseId
    openGuiOnClient = true
    PacketPipeline.sendTo(this, plr.asInstanceOf[EntityPlayerMP])
  }
  
  override def write(buf: ByteBuf) = {
    buf writeInt x
    buf writeInt y
    buf writeInt z
    ByteBufUtils.writeUTF8String(buf, id)
    buf writeBoolean openGuiOnClient
  }
  override def read(buf: ByteBuf) = {
    x = buf.readInt
    y = buf.readInt
    z = buf.readInt
    id = ByteBufUtils.readUTF8String(buf)
    openGuiOnClient = buf.readBoolean
  }
  override def onServer(player: EntityPlayer) = {
    HouseIndicatorEntity.at(player.worldObj, x, y, z).houseId = id
  }
  override def onClient(player: EntityPlayer) = {
    val houseindi = HouseIndicatorEntity.at(player.worldObj, x, y, z)
    if (houseindi != null) {
    	houseindi.houseId = id
    	if (openGuiOnClient)
    		HouseIndicatorGui.open(player, x, y, z)
    }
  }
}

object HouseIndicatorEntity {
  def at(world:World, x:Int,y:Int,z:Int) = {
    val i = world.getTileEntity(x, y, z)
    if (i != null) i.asInstanceOf[HouseIndicatorEntity]
    else null
  }
}

class HouseIndicatorEntity(world: World) extends ActiveTileEntity {
  def this() = this(null)
  var houseId = ""
    
  override def writeToNBT(comp: NBTTagCompound) = {
    super.writeToNBT(comp)
    comp.setString("houseId", houseId)
  }
  override def readFromNBT(comp: NBTTagCompound) = {
    super.readFromNBT(comp)
    houseId = comp.getString("houseId")
  }
}
*/

/*
class HouseIndicatorSyncPacket extends ActivePacket {
  @SideOnly(Side.SERVER)
  def send(player: EntityPlayer) = {
    PacketPipeline.sendTo(this, player.asInstanceOf[EntityPlayerMP])
  }
  
  override def write(buf: ByteBuf) = {
    buf writeInt SburbGame.availableHouses.size
    SburbGame.availableHouses foreach {
      buf writeString _.name
    }
  }
  override def read(buf: ByteBuf) = {
    val size = buf.readInt
    HouseIndicator.availableHouses.clear
    for (i <- 0 until size)
      HouseIndicator.availableHouses += buf.readString
  }
}
*/

object HouseIndicator extends ActiveBlock("House Indicator") {
	// this hasTileEntity classOf[HouseIndicatorEntity]
	setHardness(10000F)
  // val packet = new HouseIndicatorSyncPacket
  val availableHouses = new ArrayBuffer[String]
	
	override def onBlockActivated(world:World, x:Int, y:Int, z:Int, player:EntityPlayer, dimension:Int, px:Float, py:Float, pz:Float):Boolean = {
	  if (Sburb.isServer) {
	    // packet.send(player)
	    player.openGui(Sburb, HouseIndicatorGui.id, world, x, y, z)
	  }
	  true
	}
}