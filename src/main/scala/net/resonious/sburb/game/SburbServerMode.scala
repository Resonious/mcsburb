package net.resonious.sburb.game

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.ConcurrentModificationException
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import org.lwjgl.input.Keyboard
import SburbServerMode._
import cpw.mods.fml.relauncher.SideOnly
import io.netty.buffer.ByteBuf
import cpw.mods.fml.common.network.ByteBufUtils
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.potion.PotionEffect
import net.minecraft.util.FoodStats
import net.minecraftforge.common.util.Constants
import net.resonious.sburb.ForgeEvents
import net.resonious.sburb.Sburb
import net.resonious.sburb.abstracts.MathStuff.TupleVec
import net.resonious.sburb.abstracts.MathStuff.Vec32Tuple3
import net.resonious.sburb.abstracts.PacketPipeline
import net.resonious.sburb.abstracts.Pimp._
import net.resonious.sburb.abstracts.SburbException
import net.resonious.sburb.game.grist._
import net.resonious.sburb.hax.Injection
import net.resonious.sburb.hax.injections.EntityCollisionInjector
import net.resonious.sburb.packets.ActivePacket
import net.resonious.sburb.abstracts.Vector3
import cpw.mods.fml.relauncher.Side
import net.minecraft.world.World

object SburbServerMode {
  // Injection to make server players not collide with entities
  EntityCollisionInjector inject new Injection[Entity, Entity]() {
    def apply(e1:Entity, e2:Entity): Boolean = {
      def isServerMode(e: Entity): Boolean = (e.isInstanceOf[EntityPlayer] &&
        (SburbProperties of e.asInstanceOf[EntityPlayer]).serverMode.activated)

      !(isServerMode(e1) || isServerMode(e2))
    }
  }

  class UpdateActivatedPacket(servMode: SburbServerMode) extends ActivePacket {
    def this() = this(null)
    var activated = servMode != null && servMode.activated
    override def write(buffer: ByteBuf) = {
      buffer writeBoolean activated
  	}
  	override def read(buffer: ByteBuf) = {
  	  activated = buffer.readBoolean
  	}
  	// This packet is just to update the activated state on the client when the server's changes
    override def onClient(player: EntityPlayer) = {
      val serverMode = (SburbProperties of player).serverMode
      serverMode.activated = activated
    }
    // Quick and easily send the data!
    def send(a: Boolean = servMode.activated) = {
      if (Sburb.isClient) throw new SburbException("Only the server should be sending UpdateActivatedPackets.")
      activated = a
      PacketPipeline.sendTo(this, servMode.mpPlayer)
    }
  }

  // Not currently in use
  class SetFoodPacket(f: Int) extends ActivePacket {
    def this() = this(0)
    var food = f
    override def write(buffer: ByteBuf) = buffer writeInt food
    override def read(buffer: ByteBuf) = food = buffer.readInt()
    override def onClient(player: EntityPlayer) = {
      player.getFoodStats.setFoodLevel(food)
    }
  }

  // Server sends this to the client to tell the client how much build grist the player's sburb client has
  class SyncClientsBuildGristPacket(p: EntityPlayer) extends ActivePacket {
    def this() = this(null)
    var amount: Long = 0

    @SideOnly(Side.SERVER)
    def send() = PacketPipeline.sendTo(this, p.asInstanceOf[EntityPlayerMP])

    override def write(buffer: ByteBuf) =
      buffer.writeLong((SburbProperties of p).gameEntry.clientEntry.grist(Grist.Build))
    override def read(buffer: ByteBuf) =
      amount = buffer.readLong

    override def onClient(player: EntityPlayer) = {
      val props = SburbProperties of player
      props.serverMode.clientClientsBuildGristCache = amount
    }
  }

  def serverInvKeyCode = Keyboard.KEY_SEMICOLON
}

class SburbServerMode(props: SburbProperties = null) {
  def player = props.player
  private def mpPlayer = player.asInstanceOf[EntityPlayerMP]
  def foodStats = player.getFoodStats

  val foodExhaustion = if (Sburb.isServer)
      try {
        classOf[FoodStats].getDeclaredField("field_75126_c")
      } catch {
        case e: java.lang.NoSuchFieldException =>
          classOf[FoodStats].getDeclaredField("foodExhaustionLevel")
      }
    else
      null
  if (Sburb.isServer) foodExhaustion.setAccessible(true)
  val foodLevel = if(Sburb.isServer)
    try {
      classOf[FoodStats].getDeclaredField("field_75127_a")
      } catch {
        case e: java.lang.NoSuchFieldException =>
          classOf[FoodStats].getDeclaredField("foodLevel")
      }
    else
      null
  if (Sburb.isServer) foodLevel.setAccessible(true)
  val activePotionsMap = if(Sburb.isServer)
    try {
      classOf[EntityLivingBase].getDeclaredField("field_70713_bf")
      } catch {
        case e: java.lang.NoSuchFieldException =>
          classOf[EntityLivingBase].getDeclaredField("activePotionsMap")
      }
    else
      null
  if (Sburb.isServer) activePotionsMap.setAccessible(true)

  private var _activated = false
  private val activatedPacket = new UpdateActivatedPacket(this)

  // For properly initializing otherPos on the first activation.
  private var beenToClientBefore = false

  private var otherPos = (300.0, 200.0, 1000.0)
  private var otherInventory = new NBTTagList
  private var otherHealth = 100.0f
  private var otherFoodLevel = 20
  private var otherPotionEffects = new NBTTagList

  private var clientHouseName = ""

  private var clientGristCache: HashMap[Grist.Value, Long] = if(Sburb.isClient) new HashMap[Grist.Value, Long] else new HashMap[Grist.Value, Long]
  private var clientClientsBuildGristCache: Long = 0

  private val defaultBoundingBox = player.boundingBox
  private val serverBoundingBox = player.boundingBox.copy.setBounds(0,0,0,0,0,0)

  // Grab bounding box, obfuscated or otherwise!
  private val boundingBoxField = try {
    classOf[Entity].getDeclaredField("field_70121_D")
  } catch {
    case e: NoSuchFieldException => classOf[Entity].getDeclaredField("boundingBox")
  }
  boundingBoxField.setAccessible(true)
  // Hack out the final modifier on bounding box!
  private val modifiersField = classOf[Field].getDeclaredField("modifiers")
  modifiersField.setAccessible(true)
  modifiersField.setInt(boundingBoxField, boundingBoxField.getModifiers() & ~Modifier.FINAL)

  def grist: HashMap[Grist.Value, Long] = if (Sburb.isClient) clientGristCache else props.gameEntry.clientEntry.grist
  @SideOnly(Side.CLIENT)
  def clientsBuildGrist = clientClientsBuildGristCache

  val clientsBuildGristPacket: SyncClientsBuildGristPacket =
    if (Sburb.isServer)
      new SyncClientsBuildGristPacket(player)
    else
      null

  @SideOnly(Side.SERVER)
  private def swapPos(): Unit = {
    val plrPos = (player.posX, player.posY, player.posZ)
    player.setPositionAndUpdate(otherPos.x, otherPos.y, otherPos.z)
    // TODO player world may have to be set at some point, oh boy!
    otherPos = plrPos
  }
  @SideOnly(Side.SERVER)
  private def swapInv(): Unit = {

    // Serialize inventory + clear it
    var currentInv = new NBTTagList
    for (i <- 0 until player.inventory.mainInventory.length) {
      val stack = player.inventory.getStackInSlot(i)
      if (stack != null) {
        val tag = new NBTTagCompound
        tag.setInteger("slotNum", i)
        stack.writeToNBT(tag)
        currentInv.appendTag(tag)

        player.inventory.mainInventory(i) = null
      }
    }
    // Read from other inventory
    for (i <- 0 until otherInventory.tagCount) {
      val tag = otherInventory.getCompoundTagAt(i)
      val slot = tag.getInteger("slotNum")
      player.inventory.mainInventory(slot) = ItemStack.loadItemStackFromNBT(tag)
    }
    // Saved inventory is now our old one
    otherInventory = currentInv
  }
  @SideOnly(Side.SERVER)
  def swapHealth() = {
    val curHealth = player.getHealth
    player.setHealth(otherHealth)
    otherHealth = curHealth
  }
  @SideOnly(Side.SERVER)
  def swapFood() = {
    val curFood = foodStats.getFoodLevel
    foodLevel.setInt(foodStats, otherFoodLevel)
    PacketPipeline.sendTo(new SetFoodPacket(otherFoodLevel), mpPlayer)
    otherFoodLevel = curFood
  }
  @SideOnly(Side.SERVER)
  def swapPotion(to: Boolean) = {
    // We only actually want to save potion effects when switching TO server mode
    if (to) {
      // Serialize and clear current effects
      otherPotionEffects = new NBTTagList
      val plrEffects = player.getActivePotionEffects.iterator
      while (plrEffects.hasNext) {
        val effect = plrEffects.next.asInstanceOf[PotionEffect]
        val newTag = new NBTTagCompound
        effect.writeCustomPotionEffectToNBT(newTag)
        otherPotionEffects appendTag newTag
      }

      // Gotta do this funky recursive stuff because of Concurrent Modofi-garbage.
      // Perhaps it could be a good idea to hack the activePotionsMap to be
      // synchronized instead of this garbage.
      def removeEffects(): Unit = {
        try {
          val potionIdIterator = activePotionsMap.get(player).asInstanceOf[java.util.HashMap[Object, Object]].keySet.iterator
          val potionIdsScala = potionIdIterator.asScala
          val potionIds = potionIdsScala collect {
            case i: Integer => i
          }
          potionIds foreach {
            player removePotionEffect _
          }
        } catch {
          case e: ConcurrentModificationException => {
            After(5, 'ticks) execute removeEffects
          }
        }
      }
      removeEffects()
    } else {
      // Read from effect tags
      for (i <- 0 until otherPotionEffects.tagCount) {
        val tag = otherPotionEffects.getCompoundTagAt(i)
        player.addPotionEffect(PotionEffect.readCustomPotionEffectFromNBT(tag))
      }
      otherPotionEffects = new NBTTagList
    }
  }

  def setCapabilities() = {
    player.capabilities.allowFlying = activated
    player.capabilities.isFlying = activated
    player.capabilities.disableDamage = activated
    if (!activated)
    	player.noClip = false
    player.setInvisible(activated)

  	After(1, 'second) execute {
    	boundingBoxField.set(player,
    	    if (activated) serverBoundingBox
    	    else defaultBoundingBox)
    	if (activated)
    		player.noClip = true
    	if (Sburb.isClient) {
    	  player.setInvisible(activated)
        After(2, 'seconds) execute {
          player.setInvisible(activated)
        }
      }
    	player.setPositionAndUpdate(player.posX, player.posY, player.posZ)
  	}
  }

  def activated = _activated
  def activated_=(to:Boolean):Unit = {
    if (to != _activated) {
      if (Sburb.isServer) {
        // Set spawn point to client player's house if it's the first activation.
        if (!beenToClientBefore) {
          otherPos = props.gameEntry.clientEntry.house.spawn tupMap { _.toDouble }
          beenToClientBefore = true
        }

        swapPos()
        swapInv()
        swapHealth()
        swapFood()
        swapPotion(to)
        activatedPacket.send(to)
      } else {
        onClientActivated(to)
      }
    	player.fallDistance = 0
    }
    _activated = to

    setCapabilities()
  }
  @SideOnly(Side.CLIENT)
  def onClientActivated(to: Boolean) = {
    if (to) ForgeEvents addRenderer    GristShopHud
  	else    ForgeEvents removeRenderer GristShopHud
  }

  def update() = {
    if (activated) {
      if (!player.capabilities.isFlying)
        player.capabilities.isFlying = true
      if (Sburb.isServer) {
        // Keep hunger from depleting
        try {
        	foodExhaustion.setFloat(foodStats, 0)
        } catch {
          case e: Exception => {
            Sburb logError e.getMessage
          }
        }
        // Make sure they don't go out of bounds.
        val plrPos = new Vector3[Double](player)
        val oob = props.gameEntry.clientEntry.house.outOfBounds(plrPos)
        if (!oob.isEmpty) {
        	oob match {
        	  case s :: '> :: _ => plrPos(s) -= 1
        	  case s :: '< :: _ => plrPos(s) += 1
        	}
        	plrPos applyTo player
        }
      }
    }
  }

  def onJoin() = {
    if (Sburb.isServer) {
      // The reason we check that the player is in a game, is because when
      // the server deletes a game file, the player will join without knowing
      // that the game data is gone - this will clean up the game before
      // making any assumptions.
      if (props.hasGame) {
        setCapabilities()
      	activatedPacket.send()
      	if (props.gameEntry.hasClient) {
      	  After(2, 'seconds) execute clientsBuildGristPacket.send
      	}
      }
    }

    // Make sure invisibility is correct
    After(6, 'second) execute player.setInvisible(activated)
  }

  def save(comp: NBTTagCompound) = {
    comp.setBoolean("isServMode", _activated)

    comp.setDouble("servX", otherPos.x)
    comp.setDouble("servY", otherPos.y)
    comp.setDouble("servZ", otherPos.z)

    comp.setTag("servInv", otherInventory)
    comp.setFloat("servHp", otherHealth)
    comp.setInteger("servFood", otherFoodLevel)
    comp.setTag("servPots", otherPotionEffects)

    comp.setBoolean("b2cb4", beenToClientBefore)
    comp.setString("clientHouse", clientHouseName)
  }

  def load(comp: NBTTagCompound) = {
    val coord = { x:Char => comp.getDouble("serv"+x) }
    otherPos = (coord('X'), coord('Y'), coord('Z'))

    otherInventory = comp.getTagList("servInv", Constants.NBT.TAG_COMPOUND)
    otherHealth = comp.getFloat("servHp")
    otherFoodLevel = comp.getInteger("servFood")
    otherPotionEffects = comp.getTagList("servPots", Constants.NBT.TAG_COMPOUND)

    beenToClientBefore = comp.getBoolean("b2cb4")
    clientHouseName = comp.getString("clientHouse")

    _activated = comp.getBoolean("isServMode")
  }
}
