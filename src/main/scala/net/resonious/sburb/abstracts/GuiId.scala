package net.resonious.sburb.abstracts

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.client.gui.GuiScreen
import scala.collection.mutable.ArrayBuffer
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side
import net.resonious.sburb.packets.ActivePacket
import net.resonious.sburb.Sburb
import io.netty.buffer.ByteBuf
import net.resonious.sburb.game.After
import java.lang.reflect.InvocationTargetException

class OpenGuiPacket(gui: GuiId) extends ActivePacket {
  def this() = this(null)
  var id = if (gui != null) gui.id else -1
  var sendCoords = false
  var x, y, z = 0
  
  @SideOnly(Side.CLIENT)
  def send() = {
    sendCoords = false
    PacketPipeline.sendToServer(this)
  }
  @SideOnly(Side.CLIENT)
  def send(_x:Int, _y:Int, _z:Int) = {
    x = _x; y = _y; z = _z
    sendCoords = true
    PacketPipeline.sendToServer(this)
  }
  
  override def write(b: ByteBuf) = {
    b writeInt gui.id
    b writeBoolean sendCoords
    if (sendCoords) {
      b writeInt x; b writeInt y; b writeInt z
    }
  }
  override def read(b: ByteBuf) = {
    id = b.readInt
    sendCoords = b.readBoolean
    if (sendCoords) {
      x = b.readInt; y = b.readInt; z = b.readInt
    }
  }
  override def onServer(player: EntityPlayer) = {
    if (!sendCoords) {
      x = player.posX.asInstanceOf[Int]
      y = player.posY.asInstanceOf[Int]
      z = player.posZ.asInstanceOf[Int]
    }
    Sburb log "Opening gui: "+GuiId.get(id).getClass.getSimpleName
    player.openGui(Sburb, id, player.worldObj, x, y, z)
  }
}

object GuiId {
  var guis = new ArrayBuffer[GuiId]
  def get(id: Int) = {
    guis(id)
  }
}

abstract class GuiId {
  // Don't touch
	private var _id = -1
	def id = _id
	
	private var isSet = false
	def id_=(i: Int) = {
	  if (isSet) throw new SburbException("Attempted second ID registration on GuiId "+getClass.getSimpleName)
	  _id = i
	  packet.id = i
	  isSet = true
	}
	
	private val packet = new OpenGuiPacket(this)
	
	// By default, this will create a new instance of the same named class
	// with, or without an EntityPlayer parameter
	@SideOnly(Side.CLIENT)
	def createGuiScreen(player:EntityPlayer, x:Integer,y:Integer,z:Integer): Object = {
	  val objClassName = getClass.getName
	  val clazz = Class forName objClassName.substring(0, objClassName.length-1)
    
	  Sburb log "GUI CLASS: " + clazz.getName
	  
	  try {
      clazz.getConstructors foreach { constr =>
        val paramTypes = constr.getParameterTypes
        
        if (paramTypes.length == 0) {
          return constr.newInstance().asInstanceOf[Object]
        }
        else if (paramTypes.deep == Array(classOf[EntityPlayer]).deep) {
          return constr.newInstance(player).asInstanceOf[Object]
        }
        else if (paramTypes.deep == 
              Array(classOf[EntityPlayer],classOf[Int],classOf[Int],classOf[Int]).deep) {
          return constr.newInstance(player,x,y,z).asInstanceOf[Object]
        }
        else if (paramTypes.deep == 
              Array(classOf[Int],classOf[Int],classOf[Int]).deep) {
          return constr.newInstance(x,y,z).asInstanceOf[Object]
        }
      }
	  } catch {
	    case t: InvocationTargetException => throw t.getCause
	  }
	  throw new SburbException("Looks like you might want to implement createGuiScreen() on "+objClassName)
	}
	// By default, this will attempt to find a class of the same name, but
	// with "Gui" replaced with "Container", and instantiate it with, or 
	// without the EntityPlayer parameter
	def createContainer(player:EntityPlayer, x:Integer,y:Integer,z:Integer): Container = {
	  val objClassName = getClass.getName
	  try {
	  	val containerClass = Class forName objClassName.substring(0, objClassName.length-1).replace("Gui", "Container")
	  	
	  	Sburb log "CONTAINER CLASS: " + containerClass.getName
	  	
      containerClass.getConstructors foreach { c =>
        val paramTypes = c.getParameterTypes

        if (paramTypes.length == 0) {
          return c.newInstance().asInstanceOf[Container]
        }
        else if (paramTypes.deep == Array(classOf[EntityPlayer]).deep) {
          return c.newInstance(player).asInstanceOf[Container]
        }
        else if (paramTypes.deep == 
              Array(classOf[EntityPlayer],classOf[Int],classOf[Int],classOf[Int]).deep) {
          return c.newInstance(player,x,y,z).asInstanceOf[Container]
        }
        else if (paramTypes.deep == 
              Array(classOf[Int],classOf[Int],classOf[Int]).deep) {
          return c.newInstance(x,y,z).asInstanceOf[Container]
        }
      }
	  } catch {
	    case e: ClassNotFoundException =>
	    case t: InvocationTargetException => throw t.getCause
	  }
	  Sburb log "doing dummy though"
	  DummyContainer
	}
	
	// Opens the GUI; can be called from the server or the client.
  // Please try for not both.
	// You may optionally supply x,y,z coordinates of a block
  def open(player: EntityPlayer = null) = {
    if (Sburb.isClient) {
      packet.send()
    } else {
      if (player == null) throw new SburbException("The server cannot open a gui unless a player is specified!")
      packet.sendCoords = false
      packet.onServer(player)
    }
  }
  def open(player: EntityPlayer, x:Int,y:Int,z:Int) = {
    if (Sburb.isClient) {
      packet.send(x, y, z)
    } else {
      packet.x=x;packet.y=y;packet.z=z
      packet.sendCoords = true
      packet.onServer(player)
    }
  }
  def open(x:Int,y:Int,z:Int) = {
    if (Sburb.isServer) throw new SburbException("The server cannot open a gui unless a player is specified!")
    packet.send(x, y, z)
  }
}