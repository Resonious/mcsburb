package net.resonious.sburb.commands

import net.resonious.sburb.Sburb
import net.resonious.sburb.Structure
import net.resonious.sburb.abstracts.ActiveCommand
import net.resonious.sburb.abstracts.SburbException
import net.resonious.sburb.commands.SburbCommand.PlayerWithChat
import net.resonious.sburb.game.SburbGame
import net.resonious.sburb.game.SburbProperties
import net.minecraft.util.ChunkCoordinates
import scala.collection.JavaConverters.seqAsJavaListConverter
import net.minecraft.command.ICommandSender
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.block.Block
import net.minecraft.server.MinecraftServer
import net.minecraft.item.ItemStack
import net.resonious.sburb.items.SburbDisc
import net.minecraft.server.management.ServerConfigurationManager
import net.minecraft.util.RegistryNamespaced
import net.minecraft.util.RegistrySimple
import net.minecraft.util.ObjectIntIdentityMap
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import scala.math

object HomestuckCommand extends ActiveCommand {
  override def getCommandName() = "homestuck"
  override def getCommandUsage(sender: ICommandSender) = "/homestuck <house name>"
  override def getCommandAliases() = List("homestuck", "hivebent").asJava

  override def canCommandSenderUseCommand(sender: ICommandSender) = {
    sender match {
      case player: EntityPlayer => true
      case _ => false
    }
  }

  override def addTabCompletionOptions(sender: ICommandSender, args: Array[String]) = {
    null
  }

  override def isUsernameIndex(args: Array[String], i: Int) = false

  override def processCommand(sender: ICommandSender, args: Array[String]): Unit = {
    val player = sender.asInstanceOf[EntityPlayer]

    val houseName = if (args.length > 0) args(0) else SburbGame.randomHouseName

    val props = SburbProperties of player
    if (props.hasGame) {
      player chat "You're already in!"
      return
    }

    val game = Sburb.games.size match {
      case 0 => Sburb.newGame
      case 1 => Sburb.games.values.iterator.next
      case _ => throw new SburbException("Don't know what to do with more than 1 game yet!")
    }
    if (game.newPlayer(player, houseName, true)) {
      val house = props.gameEntry.house
      Sburb log "Generating "+house.name+" for "+player.getCommandSenderName
      player chat "Generating house..."
      house onceLoaded { _ =>
        player.inventory.addItemStackToInventory(new ItemStack(SburbDisc, 1))

        val housePos = props.gameEntry.house.spawn
        player.setPositionAndUpdate(
            housePos.x,
            housePos.y,
            housePos.z)
        val coords = new ChunkCoordinates(housePos.x, housePos.y, housePos.z)
        player.setSpawnChunk(coords, true, 0)
        player chat "Welcome home."
      }
    }
    else
      throw new SburbException("Something went wrong adding "+player.getDisplayName+" to a Sburb game!")
  }
}