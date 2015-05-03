package net.resonious.sburb.game

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.reflect.Field
import java.util.Scanner

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

object SburbGame {
  // Quick helper function to get a valid filename from a game ID
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
  
  // Reads from houses.dat to populate house data.
  // This should be called AFTER SburbGames are loaded.
  def readHouseData(games: Iterable[SburbGame]): Unit = {
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
          
          availableHouses += new PlayerHouse(houseName, position, corner1, corner2)
          
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
  val availableHouses = new ArrayBuffer[PlayerHouse]
  
  // This structure contains all sburb-related state that used to be a part of SburbProperties
  class PlayerEntry(n:String="", h:PlayerHouse=null) extends Serializable {
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
    def beforeClear() = {
      SburbGame.availableHouses += house
      Sburb log "Returned house "+house.name
    }
      
    private def str(s: String) = if (s.isEmpty) "---" else s
    override def toString() = house.name+": "+str(server)+" -> "+str(name)+" -> "+str(client)
  }
  
  class PlayerHouse(_name:String, _spawn: Vector3[Int], corner1:Array[Int], corner2:Array[Int]) extends Serializable {
    var name = _name

    var minX = corner1(0) min corner2(0)
    var minZ = corner1(1) min corner2(1)
    var maxX = corner1(0) max corner2(0)
    var maxZ = corner1(1) max corner2(1)
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
  // Transient because don't serialize RNG
  @transient
  val rand = new Random

  @transient
  var currentlySaving = false
  
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
        throw new DifferentGameException(entryOf(client), entryOf(server))
    } else {
      // Since houses must be assigned first, both players must be playing
      // in order to become client / server
      throw new NotInGameException(clientProps, serverProps)
    }
    assignNames(clientProps.playerName, serverProps.playerName, force)
  }
  
  // Add a new player with no associations to the game
  def newPlayer(plr: Any, house: Any, logError: Boolean = true):Boolean = {
    var entityPlr: EntityPlayer = null
    val aname = plr match {
      case ep: EntityPlayer => {
        entityPlr = ep
        entityPlr.getGameProfile.getName
      }
      case props: SburbProperties => {
        entityPlr = props.player
        entityPlr.getGameProfile.getName
      }
      case str: String => str
    }
    val houseIndex = house match {
      case i: Int => i
      case s: String => availableHouses indexOf (availableHouses filter { _.name == s })
      case h: PlayerHouse => availableHouses indexOf h
    }
    // TODO maybe good idea to check that houseIndex is valid here
    
    // Ignore this garbage
    // For testing; if the name is Player123 or whatever, it's always the same damn person
    val name = aname // if (("Player\\d+".r findAllIn aname).length > 0) "Player" else aname
    
    if (players contains name) {
      if(logError) Sburb logError "Game "+gameId+" already has an entry for "+name
      return false
    }
    val newEntry = new PlayerEntry(name, availableHouses(houseIndex))
    availableHouses remove houseIndex
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