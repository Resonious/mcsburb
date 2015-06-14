package net.resonious.sburb.commands

import net.minecraft.entity.passive.EntityPig
import net.resonious.sburb.game.Medium
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashMap
import scala.util.Random
import io.netty.buffer.ByteBuf
import net.minecraft.command.ICommandSender
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.server.MinecraftServer
import net.resonious.sburb.Sburb
import net.resonious.sburb.abstracts._
import net.minecraft.world.biome.BiomeGenBase
import net.minecraft.world.ChunkCoordIntPair
import net.resonious.sburb.abstracts.ActiveCommand
import net.resonious.sburb.abstracts.Pimp._
import net.resonious.sburb.abstracts.Command
import net.resonious.sburb.game._
import net.resonious.sburb.packets.ActivePacket
import net.minecraft.client.entity.EntityClientPlayerMP
import net.resonious.sburb.Structure
import scala.util.control.Breaks
import net.resonious.sburb.entities.HousePortal
import net.resonious.sburb.entities.ReturnNode
import net.resonious.sburb.entities.HousePortalRenderer
import net.resonious.sburb.entities.ReturnNode
import net.minecraft.block.BlockWood
import net.minecraft.block.BlockLeaves
import net.minecraft.block.BlockLiquid
import net.minecraft.block.BlockStaticLiquid
import net.minecraft.block.BlockDynamicLiquid
import net.minecraft.block.material.Material
import net.minecraft.item.ItemStack
import net.minecraft.util.ChunkCoordinates
import com.xcompwiz.mystcraft.api.impl.InternalAPI
import com.xcompwiz.mystcraft.world.agedata.AgeData
import com.xcompwiz.mystcraft.world.WorldProviderMyst
import com.xcompwiz.mystcraft.api.util.Color
import com.xcompwiz.mystcraft.page.Page
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.common.ForgeChunkManager
import net.minecraft.nbt.NBTTagString
import scala.math

object SburbCommand extends ActiveCommand {
	// This is a really shitty way to send chats but it works and I can't figure other shit out
	implicit class PlayerWithChat(player: ICommandSender) {
	  def chat(msg: String) = {
	    player.addChatMessage(
	    	player.func_145748_c_.appendText(": "+msg)
	    )
	  }

    def chatAndLog(msg: String) = {
      chat(msg)
      Sburb log player.getCommandSenderName+": "+msg
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

  /*
  // NOW FOR RETURN NODE
  @Command
  def set(player: EntityPlayer, args: Array[String]): Unit = {
    if (args.length < 3) {
      player chat "/set varname somenumber"
      return
    }

    val varToChange = args(1)

    def setVar(value: Double) = {
      varToChange match {
        case "er" => ReturnNode.er = value
        case "eR" => ReturnNode.eR = value
        case "ed" => ReturnNode.ed = value
        case "es" => ReturnNode.es = value
        case "doCyc" => ReturnNode.doCyc = value != 0.0
        case _ => player chat "No"
      }
    }

    // So, "19/5" should actually become 3.8 or whatever
    args(2).split('/') match {
      case Array(p, q) => setVar(p.toDouble/q.toDouble)
      case Array(a) =>
        if (a.contains("pi"))
          setVar(a.split("pi")(0).toDouble * math.Pi)
        else
          setVar(a.toDouble)
    }
  }
  // Also for testing house portal renderer
  @Command
  def tell(player: EntityPlayer): Unit = {
    player chat "er = "+ReturnNode.er
    player chat "eR = "+ReturnNode.eR
    player chat "ed = "+ReturnNode.ed
    player chat "es = "+ReturnNode.es
    if (ReturnNode.doCyc)
      player chat "epicycloid: on"
    else
      player chat "epicycloid: off"
  }
  */

  @Command
  def agename(player: EntityPlayer, args: Array[String]): Unit = {
    val age = AgeData.getAge(Integer.parseInt(args(1)), false)
    player chat "Age name: "+age.getAgeName()
  }

  @Command
  def medium(player: EntityPlayer, args: Array[String]): Unit = {
    if (!(player.getCommandSenderName == "Metreck" || player.getCommandSenderName == "joe42542")) return
    Medium.generate(player)
  }

  @Command
  def mediumtest(eplayer: EntityPlayer, args: Array[String]): Unit = {
    val player    = eplayer.asInstanceOf[EntityPlayerMP]
    val colorBlue = new Color(0.2f, 0.4f, 1f)

    val dimensionId = InternalAPI.dimension.createAge
    val age         = AgeData.getAge(dimensionId, false)

    // def addSymbol(sym: String) = age.addPage(Page.createSymbolPage(sym), 0)

    val link = InternalAPI.linking.createLinkInfoFromPosition(player.worldObj, player)

    Sburb log "Nabbed dimension "+dimensionId

    age.setInstabilityEnabled(false)

    // age.addSymbol("TerrainAplified", 0)
    // age.addSymbol("StarsTwinkle", 0)
    // age.addSymbol("FloatingIslands", 0)
    // age.addSymbol("Caves", 0)
    // age.addSymbol("ModBlue", 0)
    // age.addSymbol("ColorGrass", 0)
    // age.addSymbol("Obelisks", 0)
    // age.addSymbol("Dungeons", 0)
    // age.addSymbol("SkyRed", 0)
    // age.addSymbol("FogBlue", 0)
    val fixedSymbols = Array(
      "DenseOres", "Caves", "LightingNormal"
      // "LakesSurface",
      // "ModMat_tile.oreDiamond",
      // "ModMat_tile.grass",
      // "ModMat_tile.sponge",
      // "TerrainEnd", "ModYellow",
      // "ColorSky", "BiomeSwampland", "BioConSingle",
      // "ModGreen", "ColorWater", "ModMat_tile.stoneMoss", "GenSpikes"
    ) map Page.createSymbolPage
    args match {
      case Array(_) => {
        age setAgeName "le bullshit"
      }

      case Array(_, name) => {
        age setAgeName name
        age setPages fixedSymbols.toList.asJava.asInstanceOf[java.util.List[ItemStack]]
      }

      case Array(_, name, symbols@_*) => {
        age.setAgeName(name)
        age setPages (fixedSymbols ++ symbols.map(Page.createSymbolPage)).toList.asJava.asInstanceOf[java.util.List[ItemStack]]
      }
    }

    link.setDimensionUID(dimensionId)
    InternalAPI.linking.linkEntity(player, link)

    // player.setPositionAndUpdate(0, 200, 0)
  }

  @Command
  def spawnportal(player: EntityPlayer) = {
    val portal = new HousePortal(player.worldObj)
    portal.targetPos = new Vector3[Int](100, 100, 100)
    portal.setColorFromWorld()
    portal.setPosition(player.posX, player.posY, player.posZ + 4)
    player.worldObj.spawnEntityInWorld(portal)

    Sburb log "DONE. Spawned a portal"
  }

  @Command
  def rnode(player: EntityPlayer) = {
    val portal = new ReturnNode(player.worldObj)
    portal.targetPos = new Vector3[Int](100, 100, 100)
    portal.setColorFromWorld()
    portal.setPosition(player.posX, player.posY, player.posZ + 4)
    player.worldObj.spawnEntityInWorld(portal)

    Sburb log "DONE. Spawned a return node"
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
