package net.resonious.sburb.commands

import net.resonious.sburb.Sburb
import net.resonious.sburb.Structure
import net.resonious.sburb.abstracts.ActiveCommand
import net.resonious.sburb.abstracts.SburbException
import net.resonious.sburb.commands.SburbCommand.PlayerWithChat
import scala.collection.JavaConverters.seqAsJavaListConverter
import net.minecraft.command.ICommandSender
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.block.Block
import net.minecraft.server.MinecraftServer
import net.minecraft.server.management.ServerConfigurationManager
import net.minecraft.util.RegistryNamespaced
import net.minecraft.util.RegistrySimple
import net.minecraft.util.ObjectIntIdentityMap
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import scala.math

object StructCommand extends ActiveCommand {
  val underlyingIntegerMap = try {
      classOf[RegistryNamespaced].getDeclaredField("field_148759_a")
    } catch {
      case e: java.lang.NoSuchFieldException =>
        classOf[RegistryNamespaced].getDeclaredField("underlyingIntegerMap")
    }
  underlyingIntegerMap.setAccessible(true)

  val blockMap = classOf[ObjectIntIdentityMap].getDeclaredField("field_148749_a")
  blockMap.setAccessible(true)

  val registryObjects = try {
      classOf[RegistrySimple].getDeclaredField("field_82596_a")
    } catch {
      case e: java.lang.NoSuchFieldException =>
        classOf[RegistrySimple].getDeclaredField("registryObjects")
    }
  registryObjects.setAccessible(true)

  class PlayerState {
    var corner1: (Int,Int,Int) = (0,0,0)
    var gotCorner1 = false
    var corner2: (Int,Int,Int) = (0,0,0)
    var gotCorner2 = false

    var lastStruct: Structure = null
  }

  val states = new HashMap[String, PlayerState]

  override def getCommandName() = "struct"
  override def getCommandUsage(sender: ICommandSender) = "uhhh"
  override def getCommandAliases() = List("struct", "structure").asJava

  override def canCommandSenderUseCommand(sender: ICommandSender) = {
    sender match {
      case player: EntityPlayer =>
        // MinecraftServer.getServer.getConfigurationManager.isPlayerOpped(player.getCommandSenderName)
        // TODO WTF? I don't know why the above line won't compile
        true
      case _ => false
    }
  }

  override def addTabCompletionOptions(sender: ICommandSender, args: Array[String]) = {
    null
  }

  override def isUsernameIndex(args: Array[String], i: Int) = false

  // Test method
  def chatAllBlockClassesTo(sender: ICommandSender) = {
    val iMap = underlyingIntegerMap.get(Block.blockRegistry).asInstanceOf[ObjectIntIdentityMap]
    val map = blockMap.get(iMap).asInstanceOf[java.util.IdentityHashMap[java.lang.Object, java.lang.Integer]]
    map.keySet() foreach {
      case block: Block => {
        sender chat block.getClass.getName
      }
      case _ => sender chat "what the fuck..."
    }
  }
  // Another test method
  def chatAllBlockNamesTo(sender: ICommandSender, filter: String = null) = {
    val objMap = registryObjects.get(Block.blockRegistry).asInstanceOf[java.util.Map[java.lang.Object, java.lang.Object]]
    objMap.keySet foreach {
      case name: String =>
        if (filter == null || (name contains filter))
          sender chat name
      case _ => sender chat "what the fuck..."
    }
  }

  def stateFor(player: EntityPlayer) =
    if (!(states contains player.getCommandSenderName)) {
      val s = new PlayerState
      states += player.getCommandSenderName -> s
      s
    } else {
      states(player.getCommandSenderName)
    }

  def grab(player: EntityPlayer) = {
    val state = stateFor(player)

    if (state.gotCorner1 && state.gotCorner2) {
      state.gotCorner1 = false
      state.gotCorner2 = false
    }

    val coords = (
        player.posX.asInstanceOf[Int],
        player.posY.asInstanceOf[Int],
        player.posZ.asInstanceOf[Int]
      )

    if (!state.gotCorner1) {
      state.corner1    = coords
      state.gotCorner1 = true
      player chat "Grabbed corner 1"
    } else if (!state.gotCorner2) {
      state.corner2    = coords
      state.gotCorner2 = true
      player chat "Grabbed corner 2. Now run '/struct make <blacklist>'"
    } else {
      player chat "You already have a structure ready! Run '/struct make <blacklist>'"
    }
  }

  def place(player: EntityPlayer): Unit = {
    val name = player.getCommandSenderName
    if (!(states contains name)) {
      player chat "Plz grab first"
      return
    }

    val state = states(name)
    if (state.lastStruct == null) {
      player chat "Finish your first structure first!"
      return
    }

    state.lastStruct.placeAt(player.worldObj,
        player.posX.asInstanceOf[Int] + 2,
        player.posY.asInstanceOf[Int],
        player.posZ.asInstanceOf[Int] + 2
      )
    player chat "boom"
  }

  // For blacklist args, pass like so:
  // /struct make minecraft:grass~air minecraft:planks~ignore
  //
  // The first arg meaning "replace all grass blocks with air"
  // the second meaning "Pretend planks don't exist at all".
  //
  // If no ~(ignore|air) is provided, it will default to 'ignore'
  // functionality.
  //
  // Additionally, you can use m:blockname instead of minecraft:blockname.
  def make(player: EntityPlayer, args: Array[String]): Unit = {
    val name = player.getCommandSenderName
    if (!(states contains name)) {
      player chat "Plz grab first"
      return
    }

    val state = states(name)
    if (!(state.gotCorner1 && state.gotCorner2)) {
      player chat "No structure in place: run '/structure' at 2 corners."
      return
    }

    val blacklist = new HashMap[String, Symbol]

    args foreach { arg: String =>
      arg.split('~') match {
        case Array(blockName, blType) =>
          blacklist += blockName.replace("m:", "minecraft:") -> Symbol(blType)

        case Array(blockName) =>
          blacklist += blockName.replace("m:", "minecraft:") -> 'ignore
      }
    }

    try {
      state.lastStruct = new Structure(
        player.worldObj,
        state.corner1, state.corner2,
        blacklist,
        false // Don't assume this structure will be loaded in the same server world
      )
      state.gotCorner1 = false
      state.gotCorner2 = false
      player chat "Structure completed. Place with '/struct place'."
    } catch {
      case e: SburbException => player chat e.getMessage
    }
  }

  def save(player: EntityPlayer, fileName: String): Unit = {
    val name = player.getCommandSenderName
    if (!(states contains name)) {
      player chat "Plz grab first"
      return
    }

    val state = states(name)
    if (state.lastStruct == null) {
      player chat "You currently have no structure."
      return
    }

    state.lastStruct.saveToFile(fileName)
    player chat "Done! Go check it OUT @ "+fileName
  }

  def load(player: EntityPlayer, fileName: String): Unit = {
    val state = stateFor(player)

    state.gotCorner2 = false
    state.gotCorner1 = false

    state.lastStruct = Structure.load(fileName)

    player chat "Loaded structure from "+fileName
  }

  def setground(player: EntityPlayer): Unit = {
    val name = player.getCommandSenderName
    if (!(states contains name)) {
      player chat "Plz grab first"
      return
    }

    val state = states(name)
    if (state.lastStruct == null) {
      player chat "You currently have no structure."
      return
    }

    val captureY = state.corner1 match {
      case (_, y1, _) => state.corner2 match {
        case (_, y2, _) => math.min(y1, y2)
      }
    }
    state.lastStruct.centerOffset.y = player.posY.intValue - captureY

    player chat "Set center offset y value to " + state.lastStruct.centerOffset.y
  }

  def setspawn(player: EntityPlayer): Unit = {
    val name = player.getCommandSenderName
    if (!(states contains name)) {
      player chat "Plz grab first"
      return
    }

    val state = states(name)
    if (state.lastStruct == null) {
      player chat "You currently have no structure."
      return
    }

    state.corner1 match {
      case (x1, y1, z1) => state.corner2 match {
        case (x2, y2, z2) => {
          // TODO test that this actually works (SburbGame.PlayerHouse currently does not use it)
          state.lastStruct.spawnPoint.x = player.posX.intValue - math.min(x1, x2)
          state.lastStruct.spawnPoint.y = player.posY.intValue - math.min(y1, y2)
          state.lastStruct.spawnPoint.z = player.posZ.intValue - math.min(z1, z2)
        }
      }
    }
  }

  override def processCommand(sender: ICommandSender, args: Array[String]): Unit = {
    sender match {
      case player: EntityPlayer => {
        if (args.length > 0)
          if (args(0) equalsIgnoreCase "place")
            place(player)
          else if (args(0) equalsIgnoreCase "make")
            make(player, args.tail)
          else if (args(0) equalsIgnoreCase "list")
            chatAllBlockNamesTo(player, if (args.length > 1) args(1) else null)
          else if (args(0) equalsIgnoreCase "save")
            save(player, if (args.length > 1) args(1) else "structure.sst")
          else if (args(0) equalsIgnoreCase "load")
            load(player, if (args.length > 1) args(1) else "structure.sst")
          else if (args(0) equalsIgnoreCase "setground")
            setground(player)
          else
            player chat "Dunno what you mean by "+args(0)
        else
          grab(player)
      }

      case _ => {
        sender chat "ur not a player ... who are you"
        Sburb log "What the hell is a "+sender.getClass.getName
      }
    }

  }
}