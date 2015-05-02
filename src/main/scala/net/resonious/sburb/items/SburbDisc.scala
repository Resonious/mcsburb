package net.resonious.sburb.items

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.resonious.sburb.abstracts
import net.resonious.sburb.Sburb
import net.resonious.sburb.game.SburbProperties
import net.resonious.sburb.abstracts.ActiveItem
import li.cil.oc.api.{FileSystem => IFileSystem}
import li.cil.oc.api.FileSystem
import li.cil.oc.api.fs
import li.cil.oc.api.Network
import li.cil.oc.api.driver._
import li.cil.oc.api.driver.item.Slot
import li.cil.oc.api.network._
import li.cil.oc.api.detail.FileSystemAPI
import li.cil.oc.api.prefab.DriverItem
import li.cil.oc.common.component._
import java.io
import li.cil.oc.server.component
import li.cil.oc.api.fs.Label
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import net.minecraft.nbt.NBTTagCompound
import scala.collection.mutable.HashSet
import scala.collection.mutable.HashMap

object SburbDisc extends ActiveItem("Sburb Disc") {
  val waitingPlayers = new HashMap[String, HashSet[String]]

  li.cil.oc.api.Driver.add(DriverFloppySburbDisc)
}

object DriverFloppySburbDisc extends DriverItem(new ItemStack(SburbDisc)) {
  object SburbDiscLabel extends Label() {
    def getLabel() = "sburb"
    def setLabel(_l: String) = {}
    def load(x: NBTTagCompound): Unit = {}
    def save(x: NBTTagCompound): Unit = {}
  }

  class Environment(filesys: fs.FileSystem, host: Option[EnvironmentHost] )
  extends component.FileSystem(filesys, SburbDiscLabel, host, Option(li.cil.oc.Settings.resourceDomain + ":floppy_access")) {

    @Callback(direct = true, limit = 15, doc = """function(handle:number, count:number):string or nil -- Reads up to the specified amount of data from an open file descriptor with the specified handle. Returns nil when EOF is reached.""")
    override def read(context: Context, args: Arguments): Array[AnyRef] = super.read(context, args)

    @Callback(direct = true, limit = 15, doc = """function(handle:number, whence:string, offset:number):number -- Seeks in an open file descriptor with the specified handle. Returns the new pointer position.""")
    override def seek(context: Context, args: Arguments): Array[AnyRef] = super.seek(context, args)

    @Callback(direct = true, limit = 20)
    def sburbTest(context: Context, args: Arguments): Array[AnyRef] = {
      Sburb log "OMG. From lua: " + args.checkString(0)
      result("this is it")
    }

    private def playerPropsFrom(args: Arguments) = {
      val name = args.checkString(0)
      val player = Sburb playerOfName name
      if (player == null)
        result(null, "Player " + name + " not found")
      else
        ((SburbProperties of player), player, name)
    }

    @Callback(direct = true, limit = 20)
    def playerHasGame(context: Context, args: Arguments): Array[AnyRef] = {
      playerPropsFrom(args) match {
        case a: Array[AnyRef] => return a
        case (props: SburbProperties, _, _) => return result(props.hasGame)

        case _ => return result(false)
      }
    }

    @Callback(direct = true, limit = 20)
    def playerHasClient(context: Context, args: Arguments): Array[AnyRef] = {
      playerPropsFrom(args) match {
        case a: Array[AnyRef] => return a
        case (props: SburbProperties, _, _) =>
          if (props.hasGame) return result(props.gameEntry.hasClient)
          else return result(false)
          
        case _ => return result(false)
      }
    }

    @Callback(direct = true, limit = 20)
    def playerHasServer(context: Context, args: Arguments): Array[AnyRef] = {
      playerPropsFrom(args) match {
        case a: Array[AnyRef] => return a
        case (props: SburbProperties, _, _) =>
          if (props.hasGame) return result(props.gameEntry.hasServer)
          else return result(false)

        case _ => return result(false)
      }
    }

    @Callback(direct = false, limit = 1)
    def toggleServerMode(context: Context, args: Arguments): Array[AnyRef] = {
      playerPropsFrom(args) match {
        case a: Array[AnyRef] => return a
        case (props: SburbProperties, _, name: String) =>
          if (!props.hasGame)
            result("Player "+name+" is not in a game")
          else if (!props.gameEntry.hasClient)
            result("Player "+name+" does not have a client")
          else {
            props.serverMode.activated = !props.serverMode.activated
            result(null)
          }
      }
    }

    @Callback(direct = false, limit = 1)
    def waitForClient(context: Context, args: Arguments): Array[AnyRef] = {
      playerPropsFrom(args) match {
        case a: Array[AnyRef] => return a
        case (props: SburbProperties, _, name: String) => {
          if (!props.hasGame)
            return result("Player "+name+" is not in a game")
          else if (props.gameEntry.hasClient)
            return result("Player "+name+" already has a client!")

          if (!SburbDisc.waitingPlayers.contains(props.gameId))
            SburbDisc.waitingPlayers += props.gameId -> new HashSet[String]

          SburbDisc.waitingPlayers(props.gameId) += name
          Sburb log "Added "+name+" to waiting players for their game"
          result(null)
        }
      }
    }

    @Callback(direct = false, limit = 1)
    def doneWaitingForClient(context: Context, args: Arguments): Array[AnyRef] = {
      playerPropsFrom(args) match {
        case a: Array[AnyRef] => return a
        case (props: SburbProperties, _, name: String) => {
          if (!props.hasGame)
            return result("Player "+name+" is not in a game")

          if (!SburbDisc.waitingPlayers.contains(props.gameId))
            return result("Player "+name+" is not currently waiting")

          SburbDisc.waitingPlayers(props.gameId) -= name
          Sburb log "Removed "+name+" from waiting players for their game"
          result(null)
        }
      }
    }

    @Callback(direct = false, limit = 1)
    def listWaitingServers(context: Context, args: Arguments): Array[AnyRef] = {
      playerPropsFrom(args) match {
        case a: Array[AnyRef] => return a
        case (props: SburbProperties, _, name: String) => {
          if (!props.hasGame)
            return result("Player "+name+" is not in a game")

          if (!SburbDisc.waitingPlayers.contains(props.gameId)) {
            Sburb log "Sending empty array for waiting servers in game "+props.gameId
            return result(Array())
          }

          result(SburbDisc.waitingPlayers(props.gameId))
        }
      }
    }
  }

  override def slot(stack: ItemStack) = Slot.Floppy

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = {
    val filesys = FileSystem.fromClass(Sburb.getClass, "sburb", "sburbdisk")
    /*
    FileSystem.asManagedEnvironment(
      filesys, "sburb", host,
      li.cil.oc.Settings.resourceDomain + ":hdd_access"
    )
    */
    new Environment(filesys, Option(host))
  }

}