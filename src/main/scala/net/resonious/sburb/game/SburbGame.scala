package net.resonious.sburb.game

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.reflect.Field
import java.util.Scanner
import java.io.FilenameFilter

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.util.Random
import scala.util.{Success, Failure}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.annotation.meta.param

import SburbGame._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.server.MinecraftServer
import net.resonious.sburb.Sburb
import net.resonious.sburb.abstracts.SburbException
import net.resonious.sburb.abstracts.Vector3
import net.resonious.sburb.game.grist.Grist
import net.resonious.sburb.Structure
import net.minecraft.world.World
import net.minecraft.item.ItemStack
import net.resonious.sburb.items.SburbDisc
import net.resonious.sburb.commands.SburbCommand.PlayerWithChat

import com.xcompwiz.mystcraft.api.impl.InternalAPI

class AlreadyHasServerException(plr: SburbGame.PlayerEntry) extends SburbException(
  plr.name + " already has a server player: " + plr.server
) {
  def playerEntry = plr
}
class AlreadyHasClientException(plr: SburbGame.PlayerEntry) extends SburbException(
  plr.name + " already has a client player: " + plr.client
) {
  def playerEntry = plr
}
class DifferentGameException(c: SburbGame.PlayerEntry, s: SburbGame.PlayerEntry) extends SburbException(
  c.name + " and " + s.name + " are in different Sburb games."
)
class NotInGameException(p1: SburbProperties, p2: SburbProperties) extends SburbException(
  {
    var msgs = new ArrayBuffer[String]
    val addToMsg = (p: SburbProperties) =>
      msgs += p.player.getDisplayName+" is not in an SBURB game"
    if (!p1.hasGame)
      addToMsg(p1)
    if (!p2.hasGame)
      addToMsg(p2)
    msgs mkString " | "
  }
)
class HouseDoesNotExistException(houseName: String) extends SburbException(
  "There is no house file "+houseName+".sst in the houses directory"
)

object SburbGame {
  val rand = new Random
  final val builtInHouseNames = Array("amber", "kyle", "neokyle", "r1", "ryan", "travis")

  // Because writing `+ ".sburb"` is simply too much...
  implicit class SburbFileString(str: String) {
    def sburb() = str + ".sburb"
  }
  
  // Load an SBURB game from a file
  def load(param: Any) = {
    val fileName = param match {
      case f: File => f.getPath
      case gid: String => gid.sburb
      case _ => throw new IllegalArgumentException("Can't load sburb game from " + param)
    }

    try {
      var fileIn = new FileInputStream(fileName)
      var in = new ObjectInputStream(fileIn)
      
      in.readObject().asInstanceOf[SburbGame].onLoad()
    } catch {
      case e: FileNotFoundException => {
        Sburb logWarning "Couldn't find "+fileName+"! Clearing SBURB data."
        new SburbGame
      }
      case e: Exception => {
        Sburb logError "Corrupt sburb file: "+e.getMessage
        new File(fileName).delete()
        null
      }
    }
  }

  // This is OLD SHIT.
  def readHouseData(games: Iterable[SburbGame]): Unit = {
    throw new SburbException("NO MORE HOUSES.DAT!")
  }
  
  // The place to throw people who aren't playing SBURB.
  // I don't think this is even used.
  var defaultSpawn = new Vector3[Int]

  // Every house name.
  // This either...
  val allHouseNames = new ArrayBuffer[String]

  // This structure contains all sburb-related state that used to be a part of SburbProperties
  class PlayerEntry(n:String = "", h:PlayerHouse = null) extends Serializable {
    @transient private var _game: SburbGame = null
    def game = _game
    def game_=(g:SburbGame) = 
      if (_game != null) 
        throw new SburbException("Cannot assign game to PlayerEntry!")
      else _game = g
    
    def name = n
    var server, client = ""
    var mediumId = 0
    var mediumColor: String = null
    var mediumPointOfInterest: Vector3[Int] = null
    var mediumCatacombsThemes: ArrayBuffer[Int] = new ArrayBuffer[Int]

    // Accessed by Medium.scala
    var lastPortalSpot: Vector3[Int] = null

    // Assigned by teleporting to medium, accessed in SburbProperties#onJoin.
    // This is in case the player crashes while house is generating.
    var spawnPointDirty = false
    var houseCurrentlyBeingMoved = false

    // Used by the homestuck command to know where it's at.
    var houseCurrentlyBeingGenerated = false
    // Also for homestuck command, so that relogging players can get their disc
    var needsSburbDisc = false

    val grist = new HashMap[Grist.Value, Long]
    val house: PlayerHouse = h
    
    final def hasServer = !server.isEmpty
    final def hasClient = !client.isEmpty
    final def serverEntry = game entryOf server
    final def clientEntry = game entryOf client
    final def tryServerEntry = game tryEntryOf server
    final def tryClientEntry = game tryEntryOf client

    final def eachServer(f: (PlayerEntry) => Unit): Unit = {
      tryServerEntry match {
        case Some(s) => {
          f(s)
          if (s.server == name) return
          else s.eachServer(f)
        }
        case None => return
      }
    }
    final def eachClient(f: (PlayerEntry) => Unit): Unit = {
      tryClientEntry match {
        case Some(s) => {
          f(s)
          if (s.client == name) return
          else s.eachClient(f)
        }
        case None => return
      }
    }
    
    // Gets the entities of the client / server players if they are online.
    def serverPlayer = 
      if (hasServer)
        Sburb.playerOfName(server)
      else null
    def clientPlayer =
      if (hasClient)
        Sburb.playerOfName(client)
      else null
    
    // This should be called before Sburb data is cleared so the house can be returned.
    // ===
    // At this point I think it doesn't matter TOO much if there are duplicate houses
    def beforeClear() = {
      // SburbGame.availableHouses += house
      // Sburb log "Returned house "+house.name
    }
      
    private def str(s: String) = if (s.isEmpty) "---" else s
    override def toString() = house.name+": "+str(server)+" -> "+str(name)+" -> "+str(client)
  }
  
  class PlayerHouse(_name:String, @(transient @param) world: World) extends Serializable {
    var _spawn: Vector3[Int] = new Vector3[Int]
    var minX: Int = 0
    var maxX: Int = 0
    var minZ: Int = 0
    var maxZ: Int = 0
    var minY: Int = 0
    var centerY: Int = 0

    // Flag for SburbServerMode to know when to refetch spawn and dimension
    var wasMoved = false

    @transient lazy val rand = new Random

    def genTestCoord: Int = {
      // NOTE Random#nextInt gives from 0 to max, and we want some negatives too.
      val r = rand.nextInt(10000)
      r - r/2
    }

    final def placeAt(struct: Structure, world: World, point: Vector3[Int]) = {
      minX = point.x - struct.centerOffset.x
      maxX = point.x + struct.centerOffset.x
      minZ = point.z - struct.centerOffset.z
      maxZ = point.z + struct.centerOffset.z
      centerY = struct.centerOffset.y
      minY = point.y - struct.centerOffset.y

      _spawn.x = minX + struct.spawnPoint.x
      _spawn.y = minY + struct.spawnPoint.y
      _spawn.z = minZ + struct.spawnPoint.z

      // TODO this is super lame, but quicker than fixing the structure file...
      if (_name == "kyle") _spawn.y += 1

      // Some houses are small and not comfortable for the server player to move
      // around within the boundary.
      val minSize = 20
      val halfMinSize = minSize / 2

      val xDif = maxX - minX
      val halfXDif = xDif / 2
      if (xDif < minSize) {
        maxX += halfMinSize - halfXDif
        minX -= halfMinSize - halfXDif
      }
      val zDif = maxZ - minZ
      val halfZDif = zDif / 2
      if (zDif < minSize) {
        maxZ += halfMinSize - halfZDif
        minZ -= halfMinSize - halfZDif
      }

      Sburb log "Placing structure..."
      struct.placeAt(world, point, false)
      Sburb log "Done."
    }

    final def placeIntoWorld(struct: Structure, world: World, callback: (Vector3[Int]) => Unit, takingTooLong: (Int) => Unit): Unit =
      placeIntoWorld(struct, world, 1, callback, takingTooLong)

    final def placeIntoWorld(struct: Structure, world: World, callback: (Vector3[Int]) => Unit): Unit =
      placeIntoWorld(struct, world, 1, callback, null)

    final def placeIntoWorld(
      struct: Structure,
      world: World,
      tryCount: Int,
      callback: (Vector3[Int]) => Unit,
      takingTooLong: (Int) => Unit
    ): Unit = {
      // Right here... Place the house.
      val testPoint = new Vector3[Int](genTestCoord, 150, genTestCoord)

      val radius = 500
      Sburb log "Checking ["+testPoint.x+", "+testPoint.y+", "+testPoint.z+"] with a "+radius+" block radius for house spawn point."

      val acceptWater = InternalAPI.dimension.isMystcraftAge(world.provider.dimensionId)
      struct.findReasonableSpawnPoint(world, testPoint, radius, acceptWater) onceDone {
        case Some(point) => {
          Sburb log "Found a spot! ["+point.x+", "+point.y+", "+point.z+"]"

          placeAt(struct, world, point)
          
          if (callback != null) callback(point)
        }

        case None => {
          Sburb log "Try #"+tryCount+" - couldn't find any good spot."

          if (takingTooLong != null) takingTooLong(tryCount)
          if (tryCount >= 2) {
            if (_whenFailedToPlace == null)
              throw new SburbException("Tried 3 TIMES TO PLACE A DAMN HOUSE. BRUH.")
            else {
              _whenFailedToPlace(tryCount)
            }
          }
          else
            placeIntoWorld(struct, world, tryCount + 1, callback, takingTooLong)
        }
      }
    }

    def load() = {
      try {
        val struct = Structure.load("houses/"+_name+".sst")
        placeIntoWorld(struct, world, {
          point =>
            // Sburb log "PLACED HOUSE"
            if (_onceLoaded != null) _onceLoaded(point)
            // else Sburb log "AND HAD NO CALLBACK!!!!!"
        },
        { i => if (_whenTakingAwhile != null) _whenTakingAwhile(i) })
      } catch {
        case e: IOException => {
          throw new HouseDoesNotExistException(_name)
        }
      }
    }
    @transient var _onceLoaded: (Vector3[Int]) => Unit = null
    def onceLoaded(callback: (Vector3[Int]) => Unit) = _onceLoaded = callback

    // This is kind of a hack so that you can optionally inform the player that
    // the house is in fact still being generated...
    @transient var _whenTakingAwhile: (Int) => Unit = null
    def whenTakingAwhile(callback: (Int) => Unit) = _whenTakingAwhile = callback

    // Just as hacky as the previous callback, this gets called when the thing
    // fails to place entirely.
    @transient var _whenFailedToPlace: (Int) => Unit = null
    def whenFailedToPlace(callback: (Int) => Unit) = _whenFailedToPlace = callback

    // So this now means the file name.
    var name = _name

    def spawn: Vector3[Int] = _spawn
    
    @transient lazy val maxFields = getClass.getDeclaredFields filter { 
      _.getName contains "max" }
    @transient lazy val minFields = getClass.getDeclaredFields filter { 
      _.getName contains "min" }
    
    // Returns a string indicating which coordinate is out of bounds, and in
    // which direction. i.e. "x>" if pos.x is greater than maxX
    def outOfBounds(pos: Vector3[Double]): List[Symbol] = {
      def findField(fields: Array[Field], symb: Symbol) = {
        (fields find { f=>
          f.getName endsWith symb.toString.toUpperCase()(1)+"" 
        }).get
      }
      
      pos foreach { (s:Symbol, v:Double) =>
        if (s != 'y) {
          val max = findField(maxFields, s)
          val min = findField(minFields, s)
          
          max setAccessible true
          min setAccessible true
          
          if (v > max.getInt(this))
            return s :: '> :: Nil
          if (v < min.getInt(this))
            return s :: '< :: Nil
        }
      }
      
      Nil
    }
  }
}

class SburbGame(gid: String = "") extends Serializable {
  @transient val rand = new Random

  @transient var currentlySaving = false
  
  var players = new HashMap[String, PlayerEntry]
  
  def takenHouseNames = players.values map { _.house.name }
  
  def onLoad() = {
    players foreach { kv =>
      val plr = kv._2
      plr.game = this
      checkPlayerGrist(plr)
    }
    this
  }

  def mediumColors() = players.values
                        .map(_.mediumColor)
                        .filter(_ != null)
  
  // Makes sure the player's grist has all the correct grist types.
  def checkPlayerGrist(plr: PlayerEntry) = {
    Sburb log "Grist for "+plr.name+": "+plr.grist.toString
    Grist.values filterNot plr.grist.keySet foreach {
      plr.grist(_) = 0L
    }
  }
  
  // If this is a new game; assign it a new game ID
  var gameId = if(gid.isEmpty) {
    var str = ""
    rand.alphanumeric take 10 foreach { str += _ }
    str
  } else gid

  def randomHouseName(): String = {
    val houseFiles =
      (new File("houses")).listFiles(new FilenameFilter {
        override def accept(_dir: File, name: String): Boolean = name contains ".sst"
      }) match {
        case null => Array[String]()

        case files => files.map(_.getName.replace(".sst", ""))
      }

    val houseNames: Array[String] = houseFiles ++ SburbGame.builtInHouseNames
    houseNames.filterNot(n => takenHouseNames.exists(_ == n)) match {
      // If all houses are taken, then we have no choice...
      case Array() => { houseNames(SburbGame.rand.nextInt(houseNames.length)) }
      // Otherwise don't produce duplicates.
      case availableHouses => { availableHouses(SburbGame.rand.nextInt(availableHouses.length)) }
    }
  }
  
  
  // Assign a client-server relationship. Players will be created if they don't exist.
  // Also doesn't care whether or not these names are real players, so don't call this.
  private def assignNames(client: String, server: String, force: Boolean = false) {
    def assure(s: String) = {
      entryOf(s) // Used to add players if not existent
    }
    val clientPlr = assure(client)
    val serverPlr = assure(server)
    if (!force) {
      if (clientPlr.hasServer) throw new AlreadyHasServerException(clientPlr)
      if (serverPlr.hasClient) throw new AlreadyHasClientException(serverPlr)
    }
    if (client equalsIgnoreCase server)
      Sburb logWarning "Assigning "+client+" as his/her own server...."
    clientPlr.server = server
    serverPlr.client = client
    save()
  }
  // This'll assign real players as client->server if it's a valid combo
  def assign(client: EntityPlayer, server: EntityPlayer, force: Boolean = false) {
    val clientProps = SburbProperties of client
    val serverProps = SburbProperties of server
    if (clientProps.hasGame && serverProps.hasGame) {
      if (clientProps.gameId != serverProps.gameId)
        // Client/server can never be assigned to players of
        // separate games.
        // TODO perhaps fuse the games, though?
        // But... We don't actually have support for multiple games atm.
        throw new DifferentGameException(entryOf(client), entryOf(server))
    } else {
      // Since houses must be assigned first, both players must be playing
      // in order to become client / server
      throw new NotInGameException(clientProps, serverProps)
    }
    assignNames(clientProps.playerName, serverProps.playerName, force)
  }
  
  // Add a new player with no associations to the game
  def newPlayer(plr: Any, wantedHouse: Any, logError: Boolean = true):Boolean = {
    var entityPlr: EntityPlayer = null
    val name = plr match {
      case ep: EntityPlayer => {
        entityPlr = ep
        entityPlr.getGameProfile.getName
      }
      case props: SburbProperties => {
        entityPlr = props.player
        entityPlr.getGameProfile.getName
      }
      case str: String => {
        entityPlr = Sburb.playerOfName(str)
        str
      }
    }

    if (players contains name) {
      if(logError) Sburb logError "Game "+gameId+" already has an entry for "+name
      return false
    }
    if (entityPlr == null) {
      if(logError) Sburb logError "Player "+name+" is not logged in"
      return false
    }

    val house = wantedHouse match {
      case s: String => new PlayerHouse(s, entityPlr.worldObj)
      case h: PlayerHouse => h
    }
    house.load()

    val newEntry = new PlayerEntry(name, house)

    newEntry.game = this
    players.put(name, newEntry)
    checkPlayerGrist(newEntry)
    if (entityPlr != null) {
      (SburbProperties of entityPlr).game = this
    }
    true
  }
  
  // Get player entry of the given player, or throw exception if it isn't there
  final def entryOf(plr: Any) = {
    val aname = plr match {
      case entityPlr: EntityPlayer => entityPlr.getGameProfile.getName
      case str: String => str
    }
    // For testing once again...
    val name = if (("Player\\d+".r findAllIn aname).length > 0) "Player" else aname
    // TODO throw or return null?
    try {
      players(name)
    } catch {
      case e: NoSuchElementException => throw new SburbException("There is no entry for "+name+" in game "+gameId) 
    }
  }

  final def tryEntryOf(plr: Any): Option[PlayerEntry] = {
    try Some(entryOf(plr)) catch {
      case e: SburbException => None
    }
  }

  // Of course, save to the appropriate file
  def save() = if (!currentlySaving) {
    currentlySaving = true
    val gameData = this

    val saving = Future {
      var fileOut = new FileOutputStream(gameData.gameId.sburb)
      var out = new ObjectOutputStream(fileOut)

      out.writeObject(gameData)
      out.close()
    }

    saving onComplete {
      case Success(_) => gameData.currentlySaving = false
      case Failure(e) => {
        Sburb logError "ERROR WHILE SAVING: "+e.getMessage
        gameData.currentlySaving = false
      }
    }
  }
}
