package net.resonious.sburb.commands

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import io.netty.buffer.ByteBuf
import net.minecraft.command.ICommandSender
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.server.MinecraftServer
import net.resonious.sburb.Sburb
import net.resonious.sburb.abstracts._
import net.resonious.sburb.abstracts.ActiveCommand
import net.resonious.sburb.abstracts.Pimp._
import net.resonious.sburb.abstracts.Command
import net.resonious.sburb.game._
import net.resonious.sburb.packets.ActivePacket
import net.minecraft.client.entity.EntityClientPlayerMP

object SburbCommand extends ActiveCommand {
	// This is a really shitty way to send chats but it works and I can't figure other shit out
	implicit class PlayerWithChat(player: ICommandSender) {
	  def chat(msg: String) = {
	    player.addChatMessage(
	    	player.func_145748_c_.appendText(": "+msg)
	    )
	  }
	}
	
	var methods = new HashMap[String, Method]
	getClass.getMethods foreach { method =>
	  if (method.getAnnotations exists {  _.annotationType.getSimpleName equals "Command" })
	  	methods(method.getName) = method
	}
  
	override def getCommandName() = "sburb"

	override def getCommandUsage(sender: ICommandSender) = {
		"ask nigel"
	}

	override def getCommandAliases() = List("sburb").asJava
	
	private def getArgumentPlr(sender: EntityPlayer, args: Array[String]):EntityPlayer = {
	  var plr:EntityPlayer = null
	  if (args.length >= 2) {
	    plr = Sburb.playerOfName(args(1))
	    if (plr == null)
	      sender chat "Couldn't find player "+args(1)+'.'
	    plr
	  }
	  else sender
	}
  implicit class CmdArguments(args: Array[String]) {
	  def playerAt(index: Int) =
  	  if (index >= args.length)
  	    null
  	  else
  	    Sburb.playerOfName(args(index)) 
	}

	override def canCommandSenderUseCommand(sender: ICommandSender) = {
    false
  }

	override def addTabCompletionOptions(sender: ICommandSender, args: Array[String]) = {
		null
	}

	override def isUsernameIndex(args: Array[String], i: Int) = false
	
	override def processCommand(sender: ICommandSender, args: Array[String]): Unit = {	  
    val cmd = if (args.length > 0) args(0) else "sburb"
    if (methods contains cmd) {
      try {
        val method = methods(cmd)
        if (method.getParameterTypes.length > 1) {
          // If it takes an entityplayer specifically, make sure it gets one!
          if (method.getParameterTypes()(0) == classOf[EntityPlayer]) {
            sender match {
              case p: EntityPlayer => method.invoke(this, p, args)
              case _ => return
            }
          } else method.invoke(this, sender, args)
        }
        else if (method.getParameterTypes.length == 1) {
          // Copy/pase of that. Bad I know, but it does work and I don't trust scala
          if (method.getParameterTypes()(0) == classOf[EntityPlayer]) {
            sender match {
              case p: EntityPlayer => method.invoke(this, p)
              case _ => return
            }
          } else method.invoke(this, sender)
          // You know, it'd be pretty cool to make it figure out if it wants a player arg
        }
        else
          method.invoke(this)
      } catch {
        case e: InvocationTargetException => throw e.getTargetException
      }
    } else {
      sender chat "NO SUCH COMMAND"
    }
	} 
	
	// COMMAND METHODS!   --Note: args are essentially 1-based here :/
	@Command
	def sburb(player: EntityPlayer) = {
	  val props = SburbProperties of player
	  if (props.hasGame)
	  	props.serverMode.activated = !props.serverMode.activated
	  else
	    player chat "You aren't even playing sburb! Use 'sburb server <playername>' to assign someone as your server."
	}
	
	@Command
	def allhouses(player: EntityPlayer): Unit = {
		player chat (SburbGame.allHouseNames mkString ", ")
	}
	
	@Command
	def games(player: EntityPlayer): Unit = {
	  var i: Short = 0
	  player chat (Sburb.games.values mkString ": "+i+", ")
	}
	
	@Command
	def server(player: EntityPlayer, args: Array[String]): Unit = {
	  val serverToBe = args playerAt 1
	  if (serverToBe == null) {
	    player chat args(1) + " either doesn't exist or is not online."
	    return
	  }
	  val clientProps = SburbProperties of player
	  try {
	  	clientProps assignServer serverToBe
	  	player chat "Assigned server player! "+clientProps.gameEntry
	  	serverToBe chat "You are now the server player of "+player.getDisplayName
	  } catch {
	    case e: SburbException => {
	      e.printStackTrace()
	      player chat "*********** Got "+e.getClass.getSimpleName
	      player chat e.getMessage
	      player chat "Check server logs or tell host to check server logs"
	    }
	  }
	}

	@Command
	def status(player: EntityPlayer, args: Array[String]): Unit = {
	  val plr = getArgumentPlr(player, args)
	  if (plr == null) return
	  val props = SburbProperties of plr
	  player chat {if (props.hasGame) props.gameId + " | " + props.gameEntry.toString
	               else plr.getDisplayName + " is not playing Sburb."}
	}
	
	@Command
	def grist(player: EntityPlayer, args: Array[String]):Unit = {
	  val plr = getArgumentPlr(player, args)
	  if (plr == null) return
	  val props = SburbProperties of plr
	  if (props.hasGame) {
	    var msg = ""
	    props.gameEntry.grist foreach { kv =>
	      msg += "\n"+kv._1.toString+": "+kv._2 }
	    player chat msg
	  } 
	  else
	  	player chat plr.getDisplayName+" is not playing Sburb."
	}
	
	@Command
	def clear(player: EntityPlayer, args: Array[String]): Unit = {
	  val plr = getArgumentPlr(player, args)
	  if (plr == null) return
	  val props = SburbProperties of plr
	  props.game = null
	  player chat "Cleared Sburb game data for "+plr.getDisplayName+"!"
	}

	class TestPacket extends ActivePacket {
	  override def read(b:ByteBuf) = Sburb log "READING"
	  override def write(b:ByteBuf) = Sburb log "WRITING"
	  override def onClient(p:EntityPlayer) = Sburb log "RECEIVED ON CLIENT"
	  override def onServer(p:EntityPlayer) = Sburb log "RECEIVED ON SERVER"
	}
	@Command
	def test(plr: EntityPlayer) = {
	  PacketPipeline.sendTo(new TestPacket, plr.asInstanceOf[EntityPlayerMP])
	}
	
	@Command
	def test_with_args(args: Array[String]) = {
	  Sburb log "le args: "
	  Sburb log "----------------"
	  args foreach { Sburb log _ }
	  Sburb log "----------------"
	}
}
