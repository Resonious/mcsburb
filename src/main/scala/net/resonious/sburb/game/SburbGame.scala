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
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

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
        Sburb logError "OH GOD THE SBURB FILE IS CORRUPT. KILL IT."
          val f = new File(fileName)
          f.delete()
        null
      }
    }
  }

  def randomHouseName(): String = {
    (new File("houses")).listFiles(new FilenameFilter {
      override def accept(_dir: File, name: String): Boolean = name contains ".sst"
    }) match {
      case null => {
        // TODO perhaps support no house... lol
        // Also maybe add some default houses into the jar.
        throw new SburbException("No houses folder found.")
      }

      case Array() => {
        throw new SburbException("No .sst structure files in houses folder were found.")
      }

      case files => files(rand.nextInt(files.length)).getName.replace(".sst", "")
    }
  }
  
  // Reads from houses.dat to populate house data.
  // This should be called AFTER SburbGames are loaded.
  // TODO actually can this. We do not want anymore!!!
  def readHouseData(games: Iterable[SburbGame]): Unit = {
    throw new SburbException("NO MORE HOUSES.DAT!")

    def asInts(str: String) = {
      str.split(',') map { i=> (Integer valueOf i).asInstanceOf[Int] }
    }
    
    Sburb log "Doing the sburb houses thing!"
    
    // All house names that are currently being used by players
    // (We also add the house name to allHouseNames)
    val takenHouses = new HashSet[String]
    games foreach { g =>
      g.takenHouseNames foreach { name => 
        takenHouses add name
        if (!(allHouseNames contains name))
        	allHouseNames += name
      }
    }
    
    Sburb log "There are "+takenHouses.size+" taken houses."
    
    // Read the file
    val sburbFile = new File("./houses.dat")
    if (!sburbFile.exists) {
      Sburb logError "There is no houses.dat file in the server folder. "+
      								"Without sburb.dat, the server doesn't know where "+
      								"the houses are!"
      return
    }
    
    val fileIn = new FileInputStream(sburbFile)
    val scan = new Scanner(fileIn)
    if (!scan.hasNextLine())
      Sburb log "What?? No houses??"
    
    while (scan.hasNextLine) {
      try {
        val line = scan.nextLine
        // House name and coordinate data are separated by a colon.
        val houseNameAndData = line split ':'
        
        val houseName = houseNameAndData(0)
        var msg = "Checking house "+houseName+"... "
        if (houseName == "DEFAULT") {
          msg += "It is the default!"
          // Default house should only have 1 vector3 in its coordinate data.
          defaultSpawn = new Vector3(asInts(houseNameAndData(1)))
        }
        // If the house isn't already taken, add it to available houses.
        else if (!takenHouses(houseName)) {
          // The spawn and 2 corners are separated by pipes.
          val data = houseNameAndData(1) split '|'
          
          // First is spawn (3 ints separated by commas).
          val position = new Vector3[Int](asInts(data(0)))
          // Last 2 are the X/X coords of the corners that make the boundary
          // of the server player (2 ints separated by commas).
          val corner1 = asInts(data(1))
          val corner2 = asInts(data(2))
          
          // TODO since this function is gone, we gotta change that constructor
          // availableHouses += new PlayerHouse(houseName, position, corner1, corner2)
          
          if (!(SburbGame.allHouseNames contains houseName))
            SburbGame.allHouseNames += houseName
          
          msg += "Added it to registry!"
        }
        else
        	msg += "Looks like it's already taken by a player."
        
        Sburb log msg
      }
      catch {
        case e: Exception => {
          Sburb logError "Something's wrong with sburb.dat!"
          e.printStackTrace()
        }
      }
    }
    
    Sburb log "All done! Houses: "
    Sburb log (allHouseNames mkString ", ")
  }
  
  // The place to throw people who aren't playing SBURB.
  var defaultSpawn = new Vector3[Int]
  // Every house name.
  val allHouseNames = new ArrayBuffer[String]
  // List of houses ripe for the taking.
  // TODO NO MORE! ALL HOUSES IN /houses ARE FAIR GAME ALWAYS
  // val availableHouses = new ArrayBuffer[PlayerHouse]
  
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

    val grist = new HashMap[Grist.Value, Long]
    val house: PlayerHouse = h
    
    def hasServer = !server.isEmpty
    def hasClient = !client.isEmpty
    def serverEntry = game entryOf server
    def clientEntry = game entryOf client
    
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
  
  class PlayerHouse(_name:String, world: World) extends Serializable {
    // TODO hopefully assigning this spawn variable won't be too difficult......
    var _spawn: Vector3[Int] = new Vector3[Int]
    var minX: Int = 0
    var maxX: Int = 0
    var minZ: Int = 0
    var maxZ: Int = 0

    @transient val rand = new Random

    def genTestCoord: Int = {
      // NOTE Random#nextInt gives from 0 to max, and we want some negatives too.
      val r = rand.nextInt(10000)
      r - r/2
    }

    try {
      val struct = Structure.load("houses/"+_name+".sst")

      // Right here... Place the house.
      var tryCount = 1
      var found = false

      while (!found) {
        val testPoint = new Vector3[Int](genTestCoord, 200, genTestCoord)

        Sburb log "Checking ["+testPoint.x+", "+testPoint.y+", "+testPoint.z+"] with a 500 block radius for house spawn point."

        struct.findReasonableSpawnPoint(world, testPoint, 500) match {
          case Some(point) => {
            Sburb log "Found a spot! ["+point.x+", "+point.y+", "+point.z+"]"

            minX = point.x - struct.centerOffset.x
            maxX = point.x + struct.centerOffset.x
            minZ = point.z - struct.centerOffset.z
            maxZ = point.z + struct.centerOffset.z

            val minSize = 26
            val halfMinSize = minSize / 2

            val xDif = maxX - minX
            if (xDif < minSize) {
              maxX += halfMinSize - xDif
              minX -= halfMinSize - xDif
            }
            val zDif = maxZ - minZ
            if (zDif < minSize) {
              maxZ += halfMinSize - zDif
              minZ -= halfMinSize - zDif
            }

            _spawn.y = point.y + 5
            _spawn.x = minX
            _spawn.z = minZ

            Sburb log "Placing structure..."
            struct.placeAt(world, point, false)
            Sburb log "Done."

            found = true
          }

          case None => {
            tryCount += 1

            Sburb log "Try #"+tryCount+" - couldn't find any good spot."

            if (tryCount >= 10) {
              throw new SburbException("Tried 10 TIMES TO PLACE A DAMN HOUSE. BRUH.")
            }
          }
        }
      }

    } catch {
      case e: IOException => {
        throw new HouseDoesNotExistException(_name)
      }
    }

    // So this now means the file name.
    var name = _name

    var spawn: Vector3[Int] = _spawn
    
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
  
  private var players = new HashMap[String, PlayerEntry]
  
  def takenHouseNames = players.values map { _.house.name }
  
  def onLoad() = {
    players foreach { kv =>
      val plr = kv._2
      plr.game = this
      checkPlayerGrist(plr)
    }
    this
  }
  
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

    entityPlr.inventory.addItemStackToInventory(new ItemStack(SburbDisc, 1))
    
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
  def entryOf(plr: Any) = {
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
  
  // Of course, save to the appropriate file
  def save() = if (!currentlySaving) {
    currentlySaving = true
    Future {
      var fileOut = new FileOutputStream(gameId.sburb)
      var out = new ObjectOutputStream(fileOut)
      
      out.writeObject(this)
      out.close()

      currentlySaving = false
    }
  }
}