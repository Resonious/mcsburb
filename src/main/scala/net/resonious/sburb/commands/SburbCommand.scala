package net.resonious.sburb.commands

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

  @Command
  def agename(player: EntityPlayer, args: Array[String]): Unit = {
    val age = AgeData.getAge(Integer.parseInt(args(1)), false)
    player chat "Age name: "+age.getAgeName()
  }

  @Command
  def medium(player: EntityPlayer, args: Array[String]): Unit = {
    val props = SburbProperties of player
    if (!props.hasGame) {
      player chat "No sburb game!"
      return
    }

    val serverPlayer = props.gameEntry.serverPlayer
    if (serverPlayer != null) {
      val serverProps = SburbProperties of serverPlayer
      serverProps.serverMode.activated = false
    }

    val playerEntry = props.gameEntry
    val house       = playerEntry.house

    val savedHouse = new Structure(
      player.worldObj,
      (house.minX, house.minY, house.minZ),
      (house.maxX, 500,        house.maxZ),
      Map("minecraft:dirt" -> 'ignore)
    )
    savedHouse.centerOffset.y = house.centerY

    val dimensionId = InternalAPI.dimension.createAge
    val age         = AgeData.getAge(dimensionId, false)

    age.setInstabilityEnabled(false)
    val rand = new Random

    var symbols = new ListBuffer[String]
    symbols ++= List("DenseOres", "Caves", "BioConSingle")

    val terrain = Array(
      "TerrainNormal", "TerrainAplified", "TerrainEnd"
    )
    symbols += terrain(rand.nextInt(terrain.length))

    val obstructions = Array(
      "TerModSpheres", "FloatIslands", "Tendrils", "Obelisks",
      "StarFissure", "HugeTrees", "GenSpikes", "CryForm"
    )
    symbols += obstructions(rand.nextInt(obstructions.length))
    symbols += obstructions(rand.nextInt(obstructions.length))

    val exploration = Array(
      "Villages", "NetherFort", "Mineshafts", "Dungeons",
      "Ravines", "LakesDeep"
    )
    symbols += exploration(rand.nextInt(exploration.length))
    symbols += exploration(rand.nextInt(exploration.length))

    val colors = Array(
      "ModBlack", "ModRed", "ModGreen", "ModBlue",
      "ModYellow", "ModWhite"
    )
    val color = colors(rand.nextInt(colors.length))
    symbols ++= List(
      color, "ColorFog",
      color, "ColorFogNat",
      color, "ColorSky",
      color, "ColorSkyNat",
      color, "ColorWater",
      color, "ColorWaterNat",
      color, "ColorGrass",
      color, "ColorFoliage"
    )
    symbols ++= List(colors(rand.nextInt(colors.length)), "ColorCloud")
    symbols ++= List(colors(rand.nextInt(colors.length)), "ColorSkyNight")
    if (rand.nextInt(3) == 1) symbols += "Rainbow"

    // TODO the land of ?? and ??
    age setAgeName player.getDisplayName() + " medium"
    age setPages (symbols.map(Page.createSymbolPage))
      .asJava
      .asInstanceOf[java.util.List[ItemStack]]

    playerEntry.mediumId = dimensionId
    // Events.scala makes the player invincible while houseCurrentlyBeingMoved is
    // true. This makes them not die from fall damage here.
    playerEntry.houseCurrentlyBeingMoved = true
    Sburb.warpPlayer(player, dimensionId, new Vector3(0, 100, 0))

    val newWorld = DimensionManager.getWorld(dimensionId)
    val ticket = ForgeChunkManager.requestTicket(Sburb, newWorld, ForgeChunkManager.Type.NORMAL)
    val forceLoadChunk = new ChunkCoordIntPair(0, 0)
    ForgeChunkManager.forceChunk(ticket, forceLoadChunk)

    After(5, 'seconds) execute {
      Sburb log "PLACING HOUSE INTO MEDIUM"
      house.placeIntoWorld(savedHouse, newWorld)
      house.wasMoved = true
      Sburb log "PLACING HOUSE INTO MEDIUM: DONE"

      // TODO unsure if this is any good
      playerEntry.spawnPointDirty = true

      val housePos = house.spawn
      player.setPositionAndUpdate(
          housePos.x,
          housePos.y,
          housePos.z)
      val coords = new ChunkCoordinates(housePos.x, housePos.y, housePos.z)
      player.setSpawnChunk(coords, true, dimensionId)

      Sburb log "SET PLAYER'S SPAWN POINT IN MEDIUM"
      playerEntry.houseCurrentlyBeingMoved = false
      ForgeChunkManager.unforceChunk(ticket, forceLoadChunk)
    }
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
