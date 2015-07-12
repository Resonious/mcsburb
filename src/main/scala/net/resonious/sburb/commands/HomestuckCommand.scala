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
import net.resonious.sburb.game.After
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
  override def getCommandUsage(sender: ICommandSender) = "/homestuck [house name]"
  override def getCommandAliases() = List("homestuck", "hivebent", "housetrapped").asJava

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

    val props = SburbProperties of player
    if (props.hasGame) {
      if (props.gameEntry.houseCurrentlyBeingGenerated)
        player chat "Still working on generating your house, don't worry."
      else
        player chat "You're already in!"
      return
    }

    val game = Sburb.games.size match {
      case 0 => Sburb.newGame
      case 1 => Sburb.games.values.iterator.next
      case _ => throw new SburbException("Don't know what to do with more than 1 game yet!")
    }

    val houseName = if (args.length > 0) args(0) else game.randomHouseName

    if (game.newPlayer(player, houseName, true)) {
      // Keep track of this in case the player dies or something during the process...
      val playerName = player.getCommandSenderName
      // Keep track of game entry separately from properties for same reason.
      val gameEntry = props.gameEntry
      val house = gameEntry.house

      gameEntry.houseCurrentlyBeingGenerated = true

      Sburb log "Generating "+house.name+" for "+player.getCommandSenderName
      player chat "Looking for a good spot to place your new house..."

      house onceLoaded { _ =>
        gameEntry.houseCurrentlyBeingGenerated = false

        Sburb playerOfName playerName match {
          case null => {
            gameEntry.needsSburbDisc = true
            gameEntry.spawnPointDirty = true
            Sburb log "Finished house for "+playerName+". They will be there once they log back on."
          }

          case player => {
            player.inventory.addItemStackToInventory(new ItemStack(SburbDisc, 1))

            val housePos = props.gameEntry.house.spawn
            player.setPositionAndUpdate(
                housePos.x,
                housePos.y,
                housePos.z)
            val coords = new ChunkCoordinates(housePos.x, housePos.y, housePos.z)
            player.setSpawnChunk(coords, true, 0)
            After(1, 'second) execute { player chat "Welcome home." }
          }
        }
      }
      house whenTakingAwhile { numberOfAttempts =>
        try if (numberOfAttempts == 1 || numberOfAttempts % 3 == 0)
            player chat "Don't worry, still working on it..."
          else if (numberOfAttempts == 5)
            player chat "Sometimes this just takes an absurd amount of time. I'm sorry."
        catch {
          case e: Throwable =>
        }
      }
    }
    else
      throw new SburbException("Something went wrong adding "+player.getDisplayName+" to a Sburb game!")
  }
}
