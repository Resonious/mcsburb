package net.resonious.sburb.game

import scala.collection.mutable.ArrayBuffer
import SburbProperties._
import cpw.mods.fml.common.network.ByteBufUtils
import cpw.mods.fml.relauncher.SideOnly
import io.netty.buffer.ByteBuf
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.server.MinecraftServer
import net.minecraft.world.World
import net.minecraftforge.common.IExtendedEntityProperties
import net.resonious.sburb.Sburb
import net.resonious.sburb.abstracts._
import net.resonious.sburb.abstracts.PacketPipeline
import net.resonious.sburb.commands.SburbCommand._
import net.resonious.sburb.game.grist._
import net.resonious.sburb.packets.ActivePacket
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import scala.collection.mutable.HashMap

/*
 * ======= OBJECT ========
 */
object SburbProperties {
  val PROPERTIES_NAME = "sburbprops"
  var ofDeadPlayers = new HashMap[String, SburbProperties]

  // IN ENTITY:
  // protected HashMap<String, IExtendedEntityProperties> extendedProperties;
  val extendedProperties = classOf[Entity].getDeclaredField("extendedProperties")
  extendedProperties.setAccessible(true)

  def register(player: EntityPlayer) = {
    // When constructing the player entity, it has no game profile, but still
    // needs this shit registered.
    val name: String = try {
      player.getGameProfile.getId.toString
    } catch {
      case _: NullPointerException => null
    }

    if (name == null)
      Sburb log "NULL NAME BEING REGISTERED"
    else
      Sburb log "Registering player: " + name

    var key: String = null
    if (name != null && ofDeadPlayers.contains(name)) {
      val props = of(player)
      if (props != null && props.extendedPropertiesKey != null) {
        extendedProperties.get(player).
          asInstanceOf[java.util.HashMap[String, IExtendedEntityProperties]].
          remove(props.extendedPropertiesKey)
        key = player.registerExtendedProperties(PROPERTIES_NAME, ofDeadPlayers(name))
        ofDeadPlayers -= name
      }
    }
    else if (player.getExtendedProperties(PROPERTIES_NAME) == null)
      key = player.registerExtendedProperties(PROPERTIES_NAME, new SburbProperties(player))

    val props = of(player)
    if (key != null)
      props.extendedPropertiesKey = key
    props
  }

  def of(player: EntityPlayer) = player.getExtendedProperties(PROPERTIES_NAME).asInstanceOf[SburbProperties]

  // Sets client name, server name, and game id for the client
  class GameInfoPacket(var props: SburbProperties) extends ActivePacket {
    def this() = this(null)
    @SideOnly(Side.SERVER)
    def send() = {
      PacketPipeline.sendTo(this, props.mpPlayer)
    }

    var clientName = ""
    var serverName = ""
    var gameId = ""

    override def write(buf: ByteBuf) = {
      buf writeBoolean props.hasGame
      if (props.hasGame) {
        ByteBufUtils.writeUTF8String(buf, props.gameId)
        ByteBufUtils.writeUTF8String(buf, props.gameEntry.client)
        ByteBufUtils.writeUTF8String(buf, props.gameEntry.server)
      }
    }
    override def read(buf: ByteBuf) = {
      if (buf.readBoolean) {
        gameId = ByteBufUtils.readUTF8String(buf)
        clientName = ByteBufUtils.readUTF8String(buf)
        serverName = ByteBufUtils.readUTF8String(buf)
      } else {
        gameId = ""
        clientName = "???"
        serverName = "???"
      }
    }
    override def onClient(player: EntityPlayer) = {
      props = SburbProperties of player
      props.gameId = gameId
      props.clientPlayerName = clientName
      props.serverPlayerName = serverName
    }
  }

  // Sync grist by type
  class GristPacket(props: SburbProperties) extends ActivePacket {
    def this() = this(null)
    var grist: ArrayBuffer[(Grist.Value, Long)] = null

    @SideOnly(Side.SERVER)
    def sync(gtypes: Grist.Value*) = {
      grist = new ArrayBuffer[(Grist.Value, Long)]
      val entry = props.gameEntry
      gtypes foreach { gtype =>
        grist += gtype -> entry.grist(gtype)
      }
      PacketPipeline.sendTo(this, props.mpPlayer)
    }

    override def write(buf: ByteBuf) = {
      buf writeInt grist.length
      grist foreach { kv =>
        buf writeInt kv._1.id
        buf writeLong kv._2
      }
    }
    override def read(buf: ByteBuf) = {
      grist = new ArrayBuffer[(Grist.Value, Long)]
      val length = buf.readInt
      for (i <- 0 until length) {
        val gtype = Grist(buf.readInt)
        val amnt = buf.readLong
        grist += gtype -> amnt
      }
    }

    override def onClient(player: EntityPlayer) = {
      val props = SburbProperties of player
      val sm = props.serverMode
      grist foreach { kv => sm.grist(kv._1) = kv._2 }
    }
  }
}

/*
 * ======= CLASS ========
 */
class SburbProperties(_player: EntityPlayer) extends IExtendedEntityProperties {
  Sburb log "Registered some sburb propz"
  // After(5, 'seconds) execute { _=> Sburb log "It was for "+player.getDisplayName }

  // This is used in Events#onPlayerInteractEvent to make it so people
  // don't accidentally spend 500 grist on breaking all blocks in front of
  // them.
  var disallowBlockGrab = false

  // Keep track of this so that we can remove properties when trying to register
  // duplicates. (assigned by SburbProperties.register)
  var extendedPropertiesKey = ""

  val gameInfoPacket: GameInfoPacket =
    if(Sburb.isServer)
      new GameInfoPacket(this)
    else
      null
  val gristPacket = new GristPacket(this)

  // Accessors for the associated player
  def player = _player
  def playerName = _player.getGameProfile.getName
  lazy val mpPlayer = Sburb.playerOfName(player.getGameProfile.getName)

  // For the MC client (I hate 'client' and 'server' being common to SBURB and Minecraft)
  var clientPlayerName = "???"
  var serverPlayerName = "???"

  // Game ID of the player
  private var _gameId = ""
  def gameId = if(_gameId.isEmpty) "---" else  _gameId
  // Only set it if it's client!
  def gameId_=(id:String) = if (Sburb.isServer)
    												  throw new SburbException("Game ID should not be directly set on the server!")
                            else _gameId = id

  // Sburb game that this player belongs to (SERVER ONLY)
  private var _game: SburbGame = null
  def game = {
    if (_game == null)
      _game = if (Sburb.isClient) throw new SburbException("Client cannot access game info")
              else if (_gameId.isEmpty) null
              else if (!Sburb.games.contains(_gameId)) { Sburb logWarning "Couldn't find game "+_gameId
                game_=(null); null }
              else
                Sburb.games(gameId)
    _game
  }
  def game_=(g: SburbGame): Unit = {
    if (Sburb.isClient) throw new SburbException("No way can the client assign the Sburb game!")
    // Setting the game to null will clear all game data (hopefully)
    if (g == null) {
      try {
      	serverMode.activated = false
      } catch {
        case e: Exception => e.printStackTrace()
      }

      if (gameId == null || gameId.isEmpty)
        return

      val entry = _gameEntry
      if (entry != null)
      	entry.beforeClear()
      _game = null
      _gameId = ""
      _serverMode = new SburbServerMode(this)

      Sburb log playerName + "'s Sburb game was cleared!"
    }
    // Otherwise just sets it of course
    else {
      _gameId = g.gameId
      _game = g
      Sburb log playerName + " was assigned to game " + _gameId
    }
    gameInfoPacket.send()
  }

  // Whether or not this player is in an Sburb game
  def hasGame = !_gameId.isEmpty && (Sburb.isClient || game != null)

  def gameEntry = game entryOf player
  // Underscore version to combat bad recursion in game clearing.
  def _gameEntry = {
    try {
    	Sburb.games(_gameId) entryOf player
    } catch {
      case e: NoSuchElementException => null
    }
  }

  // Called from events
  def onJoin(): Unit = {
    if (hasGame) {
      serverMode.onJoin()
      if (Sburb.isServer) {
        After(3, 'seconds) execute {
          // Why in the devil I have to send this packet twice I do not know.
          gameInfoPacket.send()
          After(5, 'ticks) execute gameInfoPacket.send
        }
      }
    } else {
      // TODO make absolutly sure death doesn't screw this up
      After(2, 'seconds) execute {
        if (!hasGame && !SburbGame.defaultSpawn.isZero)
          player.setPositionAndUpdate(
            SburbGame.defaultSpawn.x,
            SburbGame.defaultSpawn.y,
            SburbGame.defaultSpawn.z)
      }
    }
  }

  // Assign server player, oh shit
  def assignServer(server: EntityPlayer) = {
    val serverProps = SburbProperties of server
    val sgame =
      if (hasGame) game
      else if(serverProps.hasGame) serverProps.game
      else Sburb.newGame

    sgame.assign(player, server, false)
    gameInfoPacket.send()
  }

  // Server mode module of the player
  private var _serverMode = new SburbServerMode(this)
  def serverMode = _serverMode

  // Overrides
  override def saveNBTData(comp: NBTTagCompound):Unit = {
    comp.setString("gameId", _gameId)
    // Sburb log "SAVED GAME ID OF " + _gameId
    serverMode save comp
  }

  override def loadNBTData(comp: NBTTagCompound):Unit = {
    _gameId = comp.getString("gameId")
    Sburb log "LOADED GAME ID OF " + _gameId
    serverMode load comp
  }

  override def init(entity: Entity, world: World) = {}
}
