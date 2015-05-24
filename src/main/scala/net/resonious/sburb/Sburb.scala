package net.resonious.sburb

import scala.collection.mutable.HashMap
import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.Mod.EventHandler
import cpw.mods.fml.common.SidedProxy
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import cpw.mods.fml.common.event.FMLServerStartingEvent
import cpw.mods.fml.common.registry.GameRegistry
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.ForgeChunkManager
import net.resonious.sburb.abstracts.PacketPipeline
import net.resonious.sburb.game.SburbGame
import net.resonious.sburb.game.grist.Grist
import net.resonious.sburb.proxy.CommonProxy
import cpw.mods.fml.common.FMLLog
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import org.apache.logging.log4j.Logger
import cpw.mods.fml.common.network.NetworkRegistry
// import api.player.render.RenderPlayerAPI
// import net.resonious.sburb.game.SburbPlayerRenderer
import cpw.mods.fml.relauncher.Side
import net.minecraft.server.MinecraftServer
import net.minecraft.entity.player.EntityPlayerMP
import java.io.File
import scala.collection.JavaConversions._
import org.reflections.Reflections
import net.resonious.sburb.packets.ActivePacket
import net.resonious.sburb.abstracts._
import net.resonious.sburb.commands._
import cpw.mods.fml.client.registry.ClientRegistry
import net.resonious.sburb.blocks.MultiBlock
import net.resonious.sburb.game.SburbServerMode
import net.minecraftforge.client.MinecraftForgeClient
import net.resonious.sburb.blocks.HouseIndicator
import net.resonious.sburb.blocks.HouseExtension1
import java.util.Random
import net.minecraftforge.client.IItemRenderer
import net.resonious.sburb.blocks.GristShopItem
import net.resonious.sburb.game.grist.GristShopItemRenderer
import com.xcompwiz.mystcraft.api.impl.InternalAPI
import net.minecraft.util.ChunkCoordinates
import net.minecraft.entity.player.EntityPlayer
import net.dinkyman.sburb.DMain

@Mod(modid = "sburb", version = "0.0.0", modLanguage = "scala",
  dependencies = "required-after:OpenComputers@[1.5.0,)")
object Sburb {
  @SidedProxy(modId="sburb",clientSide="net.resonious.sburb.proxy.ClientProxy",
                            serverSide="net.resonious.sburb.proxy.CommonProxy")
	var proxy:CommonProxy = new CommonProxy

	var logger: Logger = null
	def log(msg: String) = logger.info(msg)
	def logWarning(msg: String) = logger.warn(msg)
	def logError(msg: String) = logger.error(msg)

  val reflections = new Reflections("net.resonious.sburb")

	private var _isClient = false
	def isClient = _isClient

	private var _isServer = false
	def isServer = _isServer

  def playerOfName(name: String): EntityPlayerMP = {
    MinecraftServer.getServer.getConfigurationManager.playerEntityList foreach { player =>
      val entityPlayer = player.asInstanceOf[EntityPlayerMP]
      if (entityPlayer.getCommandSenderName == name)
        return entityPlayer
    }
    null
  }

  def warpPlayer(player: EntityPlayer, dim: Int, to: Vector3[Int]): Unit = {
    if (player.worldObj.provider.dimensionId != dim) {
      val link = InternalAPI.linking.createLinkInfoFromPosition(player.worldObj, player)
      link.setDimensionUID(dim)
      link.setSpawn(new ChunkCoordinates(to.x, to.y, to.z))
      InternalAPI.linking.linkEntity(player, link)
    } else {
      player.setPositionAndUpdate(to.x, to.y, to.z)
    }
  }

	var games = new HashMap[String, SburbGame]
  def newGame(): SburbGame = {
    val game = new SburbGame
    games(game.gameId) = game
    game.save()
    return game
  }

  def sortedSubclasses[T](c: Class[T]) = {
    val subtypes = reflections.getSubTypesOf(c)
    val arr = subtypes.toArray(new Array[Class[T]](subtypes.size))
    arr sortWith {
      (c1, c2) => (c1.getSimpleName compareTo c2.getSimpleName) >= 1
    }
  }
  def sortedSingletons[T](c: Class[T]) = {
    sortedSubclasses(c).map { clazz =>
        val f = clazz.getFields find { _.getName == "MODULE$" }
        if (f.isEmpty) null
        else f.get get null
  	  }.filterNot(_==null)
  }

  @EventHandler
  def preInit(event: FMLPreInitializationEvent) {
    // Logger!
    logger = event.getModLog
    _isClient = event.getSide == Side.CLIENT
    _isServer = event.getSide == Side.SERVER

    DMain.preInit(event)
  }

  @EventHandler
  def init(event: FMLInitializationEvent) {
    // Register items and blocks
    sortedSingletons(classOf[ActiveItem]) foreach {
      case item: ActiveItem =>
      	GameRegistry.registerItem(item, item.getUnlocalizedName)
    }
    Grist.generateGristItems foreach { item =>
      log("Registering grist item: "+item.getUnlocalizedName)
      GameRegistry.registerItem(item, item.getUnlocalizedName)
    }
    sortedSubclasses(classOf[ActiveTileEntity]) foreach { c =>
      val name = c.getSimpleName
      GameRegistry.registerTileEntity(c, name.charAt(0).toLower + name.substring(1))
    }
    sortedSingletons(classOf[ActiveBlock]) foreach {
      case mblock: MultiBlock => {
        GameRegistry.registerBlock(mblock, mblock.getUnlocalizedName)
        val item = mblock.generateItem()
        GameRegistry.registerItem(item, item.getUnlocalizedName)
        log("Registered multiblock "+item.getUnlocalizedName)
      }
      case block: ActiveBlock => {
        GameRegistry.registerBlock(block, block.getUnlocalizedName)
        if (block.stairs != null) {
          GameRegistry.registerBlock(block.stairs, block.stairs.getUnlocalizedName)
        }
      }
    }

    // Gui
    log("***Registering GUIs***")
    sortedSingletons(classOf[GuiId]) foreach {
      case gui: GuiId => {
        gui.id = GuiId.guis.length
        GuiId.guis += gui
        log("Registering gui " + gui.getClass.getName)
      }
    }
    log("-----------")

    // RenderPlayer
    // if (isClient)
    	// RenderPlayerAPI.register("sburb", classOf[SburbPlayerRenderer])

    // Network
    NetworkRegistry.INSTANCE.registerGuiHandler(this, proxy)

    // Events
    MinecraftForge.EVENT_BUS register ForgeEvents
    FMLCommonHandler.instance.bus register FmlEvents

    // Packet pipeline and packets
    PacketPipeline.initialise
    log("Registering packets...")
    sortedSubclasses(classOf[ActivePacket]) foreach { p =>
      log("Registering packet " + p.getName)
      PacketPipeline registerPacket p
    }

    if (isClient) {
      // Tile Entity Renderers
      sortedSingletons(classOf[ActiveTileEntityRenderer]) foreach {
        case r: ActiveTileEntityRenderer => {
          log("Registering Tile Entity Renderer " + r.getClass.getName)
          ClientRegistry.bindTileEntitySpecialRenderer(r.tileEntityType, r)
        }
      }
      Keys.init()

      // Item renderers
      /*sortedSingletons(classOf[GristShopItem]) foreach { gsi =>
        val item = gsi match {
          case i: ActiveItem => i
          case mb: MultiBlock => mb.item
          case b: ActiveBlock => b.getItemDropped(0, new Random, 0)
        }
        MinecraftForgeClient.registerItemRenderer(item, GristShopItemRenderer)
      }*/
    }

    // Run implicit constructor:
    SburbServerMode

    DMain.init(event)
  }

  @EventHandler
  def postInit(event: FMLPostInitializationEvent) {
    PacketPipeline.postInitialise
    ForgeChunkManager.setForcedChunkLoadingCallback(this, ForgeEvents)
    DMain.postInit(event)
  }

  @EventHandler
  def onServerStart(event: FMLServerStartingEvent): Unit = {
    sortedSingletons(classOf[ActiveCommand]) foreach {
      case s: ActiveCommand => event.registerServerCommand(s)
    }

    // Load Sburb games if server
    if (isClient) return

    val sburbGameFiles = new File("./").listFiles filter { _.getName endsWith ".sburb" }
    Sburb log "Doin' the Sburb files thing"
    if (sburbGameFiles.isEmpty)
      Sburb logWarning "No sburb files found!"

    sburbGameFiles foreach { f =>
      Sburb log "Loading "+f
      val game = SburbGame.load(f)
      if (game != null) games(game.gameId) = game
    }

    // SburbGame.readHouseData(games.values)
  }
}
