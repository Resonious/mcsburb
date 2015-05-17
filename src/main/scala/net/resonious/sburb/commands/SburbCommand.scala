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
import net.resonious.sburb.Structure
import scala.util.control.Breaks
import net.minecraft.block.BlockWood
import net.minecraft.block.BlockLeaves
import net.minecraft.block.BlockLiquid
import net.minecraft.block.BlockStaticLiquid
import net.minecraft.block.BlockDynamicLiquid
import net.minecraft.block.material.Material
import net.minecraft.util.ChunkCoordinates
import scala.math

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
    // sender.getCommandSenderName == "Metreck"
    true
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
	def s(player: EntityPlayer) = {
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

  // TODO can this. The logic is in Structure.scala.
  @Command
  def housegen(player: EntityPlayer, args: Array[String]): Unit = {
    // Gonna be a bumpy ride...
    val world = player.worldObj
    var start = new Vector3[Int](
      player.posX.intValue,
      player.posY.intValue,
      player.posZ.intValue
    )

    val structure = Structure.load(if (args.length > 1) args(1) else "structure.sst")

    def blockAt(s: Vector3[Int]) = world.getBlock(s.x, s.y, s.z)

    // Ignore and trees, for now.
    def ignoreBlockAt(s: Vector3[Int]) = {
      val block = blockAt(s)

      world.isAirBlock(s.x, s.y, s.z) ||
      block.isInstanceOf[BlockWood]   ||
      block.isInstanceOf[BlockLeaves]
    }

    def heightAt(s: Vector3[Int]): Int =
      if (ignoreBlockAt(s))
        if (ignoreBlockAt(s.instead(_.y -= 1)))
          heightAt(s.instead( _.y -= 1 ))
        else
          s.y
      else
        heightAt(s.instead( _.y += 1 ))

    // So that we can cache heights at any position - this function will be called
    // many times at any given x/z posibion.
    // Also whether or not the ground block at that given point is water.
    var heights = new HashMap[(Int, Int), (Int, Boolean)]

    // Estimate of top-down structure area - we assume the structure has some
    // padding in it (that its bounds are greater than the actual thing contained)
    // and so we use 3.5 instead of 4, arbitrarily.
    val structArea = 3.5 * structure.centerOffset.x * structure.centerOffset.z
    // Accept up to 1/16th of the approx structure area of extremes.
    val extremesTolerance = structArea / 16

    // Accept up to 1/4th of the ground below the structure to be water.
    val watersTolerance = structArea / 4

    // This will get called tonnnnnnnnnns of times. Returns a position with the same
    // x and z coords as input, and y value of the smallest (sane) height recorded
    // during the scan IF it determines the given center is deemed suitable.
    def acceptableCenterAt(center: Vector3[Int]): Option[Vector3[Int]] = {
      var curPos = new Vector3[Int](center)

      var maxHeight = heightAt(curPos)
      var minHeight = maxHeight

      // We'll call any sudden large drop or rise in block height an "extreme".
      // Too many of these, and we will consider this an center unacceptable.
      var extremes = 0

      // Count how many ground blocks are water. We don't want houses on rivers or lakes.
      var waters = 0

      // Assume the x/z of centerOffset is effectively the "radius" of the
      // structure's top-down area.
      for (xOffset <- -structure.centerOffset.x to structure.centerOffset.x)
      for (zOffset <- -structure.centerOffset.z to structure.centerOffset.z) {
        curPos = center instead { s => s.x += xOffset; s.z += zOffset }

        val heightInfo = heights.get((curPos.x, curPos.z)) match {
          case Some(values) => values

          case None => {
            val h = heightAt(curPos)
            val b = blockAt(curPos.instead(_.y = h - 1))
            val bIsWater = b.getMaterial == Material.water

            heights((curPos.x, curPos.z)) = (h, bIsWater)
            (h, bIsWater)
          }
        }

        // === Check if we're over too much water ===
        val isWater = heightInfo match { case (_, b) => b }

        if (isWater) waters += 1
        if (waters > watersTolerance) return None

        // === Make sure terrain is still acceptable ===
        val height = heightInfo match { case (h, _) => h }

        // If we encounter an extreme, don't count it toward min/max height
        if (height - maxHeight > 5) {
          extremes += 1
        } else if (minHeight - height > 5) {
          extremes += 1
        }
        else if (height < minHeight) minHeight = height
        else if (height > maxHeight) maxHeight = height

        if (extremes > extremesTolerance) return None
        else if (maxHeight - minHeight > 6) return None
      }

      Some(center.instead(_.y = minHeight))
    }

    def spawnHouseAt(location: Vector3[Int]) = {
      structure.placeAt(world, location)
    }

    // Search 100 blocks in either direction (make this a parameter later, probably)
    val radius = 100

    for (checkX <- start.x - radius until start.x + radius)
    for (checkZ <- start.z - radius until start.z + radius) {
      acceptableCenterAt(new Vector3[Int](checkX, start.y, checkZ)) match {
        case Some(result) => {
          spawnHouseAt(result)
          return
        }

        case None => {}
      }
    }

    player chat "OMG no dice...."
    Sburb log "DUDE tried to spawn house but no fucking luck."
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
