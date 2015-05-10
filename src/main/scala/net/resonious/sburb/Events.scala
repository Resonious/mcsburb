package net.resonious.sburb

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.eventhandler.Event.Result
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent
import cpw.mods.fml.common.gameevent.TickEvent
import cpw.mods.fml.common.gameevent.PlayerEvent
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraftforge.client.event.MouseEvent
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent
import net.minecraftforge.event.entity.player.EntityItemPickupEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.resonious.sburb.abstracts.PacketPipeline
import net.resonious.sburb.game.SburbProperties
import net.resonious.sburb.game.grist.Grist._
import net.resonious.sburb.game.grist._
import net.resonious.sburb.items.SburbDisc
import net.minecraft.block.Block
import net.resonious.sburb.game.After
import net.resonious.sburb.packets.ActivePacket
import net.resonious.sburb.abstracts.Pimp._
import java.io.ObjectOutputStream
import java.io.ObjectInputStream
import io.netty.channel.ChannelHandlerContext
import io.netty.buffer.ByteBuf
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashSet
import net.resonious.sburb.abstracts.Renderer
import net.minecraftforge.event.entity.player.AttackEntityEvent
import net.minecraftforge.event.world.WorldEvent
import scala.collection.JavaConverters._
import net.resonious.sburb.abstracts.SburbException
import net.minecraft.item.ItemStack
import net.minecraftforge.event.entity.living.LivingDeathEvent
import scala.util.Random
import net.resonious.sburb.blocks.MultiBlock
import net.resonious.sburb.blocks.GristShopItem

object ForgeEvents {
  private var inited = false
  val blockHardness = try {
    classOf[Block].getDeclaredField("field_149782_v")
  } catch {
    case _: java.lang.NoSuchFieldException =>
      classOf[Block].getDeclaredField("blockHardness")
  }
  blockHardness.setAccessible(true)

	@SubscribeEvent
	def onEntityJoinWorldEvent(event: EntityJoinWorldEvent) = {
	  event.entity match {
	    case player: EntityPlayer => {
        // Call `register` here instead of `of` in case the player died and the
        // properties need to be reassigned.
        val props = SburbProperties register player

	      props.onJoin()
	    }
	    case _ =>
	  }
	}

  @SubscribeEvent
  def onLivingUpdateEvent(event: LivingUpdateEvent) = {
    event.entity match {
      case player: EntityPlayer => {
        val props = SburbProperties of player
        if (props.hasGame) {
          props.serverMode.update()
        }
      }
      case _ =>
    }
  }

  @SubscribeEvent
  def onEntityConstructing(event: EntityConstructing) = {
    event.entity match {
      case player: EntityPlayer => SburbProperties register player
      case _ =>
    }
  }

  @SubscribeEvent
  def onEntityItemPickupEvent(event: EntityItemPickupEvent):Unit = {
    val item = event.item.getEntityItem.getItem
    if (item.isGristItem) {
      val player = event.entityPlayer
      val props = (SburbProperties of player)
      if (!props.hasGame || props.serverMode.activated)
        return

      val stackSize = event.item.getEntityItem.stackSize
      val gitem = item.asInstanceOf[GristItem]

      props.gameEntry.grist(gitem.gristType) += stackSize
      Sburb log "Grabbed "+stackSize+" "+gitem.gristType+" Grist  for "+player.getDisplayName+"!"
      event.setResult(Result.ALLOW)
      event.item.setDead()
      props.gristPacket.sync(gitem.gristType)

      val serverPlayer = props.gameEntry.serverPlayer
      if (serverPlayer != null)
        (SburbProperties of serverPlayer).serverMode.clientsBuildGristPacket.send()
    }
  }

  @SubscribeEvent
  def onPlayerInteractEvent(event: PlayerInteractEvent): Unit = {
    val props = SburbProperties of event.entityPlayer
    if (event.action == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK) {
      if (props.serverMode.activated) {
        if (props.disallowBlockGrab) {
          event setCanceled true
          return
        }

        val clientsGrist = if (Sburb.isServer)
          								   props.gameEntry.clientEntry.grist(Grist.Build)
          								 else
          								   props.serverMode.clientsBuildGrist
        val world = event.entityPlayer.getEntityWorld
        val block = world.getBlock(event.x, event.y, event.z)
        val meta = world.getBlockMetadata(event.x, event.y, event.z)
        val cost = if ((GristShopItem of block) == null)
            blockHardness.get(block).asInstanceOf[Float].ceil.toInt * 2
          else 0

        if (cost < 0 || clientsGrist < cost) {
          event setCanceled true
          return
        }

        // Only charge for breaking non grist shop items
        if (Sburb.isServer) {
          props.gameEntry.clientEntry.grist(Grist.Build) -= cost
          props.serverMode.clientsBuildGristPacket.send()
        }

        // Server player is given the block
        val newStack: ItemStack = (
          block match {
            case mb: MultiBlock => new ItemStack(mb.item)
            case _ => new ItemStack(block, 1, meta)
          })
        event.entityPlayer.inventory.addItemStackToInventory(newStack)
        world.func_147480_a(event.x,event.y,event.z,false)

        props.disallowBlockGrab = true
        After(10, 'ticks) execute { props.disallowBlockGrab = false }
      }
    }
  }

  @SubscribeEvent
  def onLivingDeathEvent(event: LivingDeathEvent) = {
    // TODO I guess add different grist types
    if (Sburb.isServer)
    	event.entityLiving.dropItem(
        Grist.item(Grist.Build),
        Random.nextInt(event.entityLiving.getMaxHealth.ceil.toInt)
      )

    if (event.entityLiving.isInstanceOf[EntityPlayer]) {
      val player = event.entityLiving.asInstanceOf[EntityPlayer]
      val props = SburbProperties of player
      SburbProperties.ofDeadPlayers(player.getGameProfile.getId.toString) = props
    }
  }

  @SubscribeEvent
  def onAttackEntityEvent(event: AttackEntityEvent) = {
    /*val props = SburbProperties of event.entityPlayer
    if (props.serverMode.activated) {
      event.setCanceled(true)
    }*/
  }

  @SubscribeEvent
  def onWorldSave(event: WorldEvent.Save) {
    // Gets all the online players' SburbGames, and saves them.
    if (Sburb.isServer) {
      event.world.playerEntities.asScala.toSet map ((e: Any) => {
        e match {
          case plr: EntityPlayer => (SburbProperties of plr).game
          case _ => throw new SburbException("How is there a non-player in this list?")
        }
      }) foreach { game =>
        if (game != null)
        	game.save()
      }
    }
  }

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	def onMouseEvent(event: MouseEvent) = {
	  //if (event.dwheel >= 120)
	  //  event.setCanceled(true)
	}

	private var renderers = new HashSet[Renderer]
	@SideOnly(Side.CLIENT) def addRenderer(g: Renderer) = renderers += g
	@SideOnly(Side.CLIENT) def removeRenderer(g: Renderer) = renderers -= g
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	def onRenderGameOverlay(event: RenderGameOverlayEvent.Post):Unit = {
	  if (event.isCancelable() || event.`type` != ElementType.EXPERIENCE) {
			return
		}
	  renderers foreach { _.render(event.resolution)	}
	}
}

object FmlEvents {

  class TestBaseClass {
    def thing() = println("base!")
  }

  // Server:
  @SubscribeEvent
  def onTick(event: TickEvent.ServerTickEvent) = {
    After.tick()
  }
  // Client:
  @SubscribeEvent
  def onTick(event: TickEvent.ClientTickEvent) = After.tick()

  @SideOnly(Side.CLIENT)
	@SubscribeEvent
	def onKeyInputEvent(event: KeyInputEvent) = {
    def packet(s: String) = { var p = new TestPacket; p.msg = s; p }

	  if (Keys.debug.isPressed) {
	    Sburb log "Sent out to server"
	    PacketPipeline.sendToServer(packet("From keypress (client)"))
	  }
	  if (Keys.debug2.isPressed) {
	    Sburb log "Sent out to all"
	    // PacketPipeline.sendToAll(packet("From keypress to all!"))
	  }
	  if (Keys.gristShop.isPressed) {
	    val plr = Minecraft.getMinecraft.thePlayer
	    if ((SburbProperties of plr).serverMode.activated) {
  	    plr.closeScreen()
  	    GristShopGui.open()
	    }
	  }
	}

  @SideOnly(Side.SERVER)
  @SubscribeEvent
  def onLogout(event: PlayerEvent.PlayerLoggedOutEvent) = {
    SburbDisc.playerLoggedOut(event.player)
  }
}
class TestPacket extends ActivePacket {
  var msg = ""
  override def write(out: ByteBuf) = out writeString msg
  override def read(in: ByteBuf) = msg = in.readString
  override def onClient(plr: EntityPlayer) =
    Sburb log "On client "+plr.getDisplayName+": "+msg
  override def onServer(plr: EntityPlayer) =
    Sburb log "On server "+plr.getDisplayName+": "+msg
}
