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
import scala.util.control.Breaks._

object SburbDisc extends ActiveItem("Sburb Disc") {
  val waitingPlayers = new HashMap[String, HashSet[String]]
  val interestedPlayers = new HashMap[String, HashSet[(String, String)]]

  li.cil.oc.api.Driver.add(DriverFloppySburbDisc)

  // Called from Events.scala.
  def playerLoggedOut(player: EntityPlayer): Unit = {
    val props = SburbProperties of player
    if (!props.hasGame) return
    clearStateFor(props.gameId, player.getCommandSenderName())
  }

  def clearStateFor(gameId: String, name: String): Unit = {
    if (waitingPlayers.contains(gameId))
      waitingPlayers(gameId) -= name

    if (interestedPlayers.contains(gameId)) {
      val interested = interestedPlayers(gameId)

      interested foreach {
        case (a, b) => if (a == name || b == name)
          interested -= ((a, b))
      }
    }

    // Sburb log "Cleared disc state for "+name+" (game id "+gameId+")"
  }
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
      if (player == null) {
        Sburb logError "Lua tried to access non-player "+name
        result(null, "Player " + name + " not found")
      }
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
    def serverPlayerOf(context: Context, args: Arguments):Array[AnyRef] = {
      playerPropsFrom(args) match {
        case (props: SburbProperties, _, _) =>
          if (props.gameEntry.hasServer) return result(props.gameEntry.server)
          else return result(null)
        case _ => return result(null)
      }
    }

    @Callback(direct = true, limit = 20)
    def clientPlayerOf(context: Context, args: Arguments):Array[AnyRef] = {
      playerPropsFrom(args) match {
        case (props: SburbProperties, _, _) =>
          if (props.gameEntry.hasClient) return result(props.gameEntry.client)
          else return result(null)
        case _ => return result(null)
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

    @Callback(direct = false, limit = 5)
    def waitForClient(context: Context, args: Arguments): Array[AnyRef] = {
      playerPropsFrom(args) match {
        case a: Array[AnyRef] => return a
        case (props: SburbProperties, _, name: String) => {
          try {
            if (!props.hasGame)
              return result("Player "+name+" is not in a game")
            else if (props.gameEntry.hasClient)
              return result("Player "+name+" already has a client!")

            if (!SburbDisc.waitingPlayers.contains(props.gameId))
              SburbDisc.waitingPlayers += props.gameId -> new HashSet[String]

            SburbDisc.waitingPlayers(props.gameId) += name
            Sburb log "Added "+name+" to waiting players for their game"
            result(null)
          } catch {
            case e: Throwable => {
              Sburb logError "sburb.waitForClient: "+e.toString
              result(e.toString())
            }
          }
        }
      }
    }

    @Callback(direct = false, limit = 5)
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

    @Callback(direct = true, limit = 5)
    def listWaitingServers(context: Context, args: Arguments): Array[AnyRef] = {
      playerPropsFrom(args) match {
        case a: Array[AnyRef] => return a
        case (props: SburbProperties, _, name: String) => {
          if (!props.hasGame)
            return result(null, "Player "+name+" is not in a game")

          if (!SburbDisc.waitingPlayers.contains(props.gameId))
            return result(Array())

          result(SburbDisc.waitingPlayers(props.gameId))
        }
      }
    }

    @Callback(direct = false, limit = 5)
    def selectServer(context: Context, args: Arguments): Array[AnyRef] = {
      try {
        playerPropsFrom(args) match {
          case a: Array[AnyRef] => return a
          case (props: SburbProperties, _, name: String) => {
            if (!props.hasGame)
              return result("Player "+name+" is not in a game")

            val interestedIn = args.checkString(1)

            Sburb log "So, "+name+" is interested in "+interestedIn

            if (!SburbDisc.interestedPlayers.contains(props.gameId))
              SburbDisc.interestedPlayers += props.gameId -> new HashSet[(String, String)]

            val players = SburbDisc.interestedPlayers(props.gameId)
            players foreach {
              case (a, b) => if (a == name) players -= ((a, b))
            }
            players += ((name, interestedIn))

            /*
            players foreach {
              case (a, b) => {
                Sburb log "Summary: "+a+" wants "+b
              }
            }
            */

            // Sburb log "Reached the end of selectServer"
            result(null)
          }
        }
      } catch {
        case e: Throwable => {
          Sburb logError "sburb.selectServer: "+e.toString
          result(e.toString())
        }
      }
    }

    @Callback(direct = false, limit = 5)
    def cancelSelection(context: Context, args: Arguments): Array[AnyRef] = {
      try {
        playerPropsFrom(args) match {
          case a: Array[AnyRef] => return a
          case (props: SburbProperties, _, name: String) => {
            if (!props.hasGame)
              return result("Player "+name+" is not in a game")

            if (!SburbDisc.interestedPlayers.contains(props.gameId))
              return result(null)

            val players = SburbDisc.interestedPlayers(props.gameId)
            players foreach {
              case (a, b) => if (a == name) {
                Sburb log a+" is no longer interested in (or was rejected by) "+b
                players -= ((a, b))
              }
            }
            // Sburb log "and done purging selections for "+name
            result(null)
          }
        }
      } catch {
        case e: Throwable => {
          Sburb logError "sburb.cancelSelection: "+e.toString
          result(e.toString())
        }
      }
    }

    @Callback(direct = true, limit = 5)
    def checkIfSelected(context: Context, args: Arguments): Array[AnyRef] = {
      try {
        playerPropsFrom(args) match {
          case a: Array[AnyRef] => return a
          case (props: SburbProperties, _, name: String) => {
            if (!props.hasGame)
              return result("Player "+name+" is not in a game")

            if (!SburbDisc.interestedPlayers.contains(props.gameId))
              return result(null)

            var selectedBy: String = null
            breakable {
              SburbDisc.interestedPlayers(props.gameId) foreach {
                case (a, b) =>
                  if (b == name) {
                    selectedBy = a
                    break
                  }
              }
            }
            // Sburb log "And we conclude that "+name+" was selected by "+selectedBy
            result(selectedBy != null, selectedBy)
          }
        }
      } catch {
        case e: Throwable => {
          Sburb logError "sburb.checkIfSelected: "+e.toString
          result(e.toString(), null)
        }
      }
    }

    @Callback(direct = false, limit = 1)
    def appointServer(context: Context, args: Arguments): Array[AnyRef] = {
      try {
        playerPropsFrom(args) match {
          case a: Array[AnyRef] => return a
          case (props: SburbProperties, _, name: String) => {
            if (!props.hasGame)
              return result("Player "+name+" is not in a game")

            val serverName = args.checkString(1)
            props assignServer Sburb.playerOfName(serverName)
            Sburb log "Assigned "+serverName+" as "+name+"'s server!"
            result(null)
          }
        }
      } catch {
        case e: Throwable => {
          Sburb logError "sburb.appointServer: "+e.toString()
          result(e.toString())
        }
      }
    }

    @Callback(direct = false, limit = 5)
    def clearStateFor(context: Context, args: Arguments): Array[AnyRef] = {
      playerPropsFrom(args) match {
        case a: Array[AnyRef] => return a
        case (props: SburbProperties, _, name: String) => {
          if (props.hasGame)
            SburbDisc.clearStateFor(props.gameId, name)
          result(null)
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