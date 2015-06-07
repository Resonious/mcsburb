package net.resonious.sburb.entities

import net.resonious.sburb.game.After
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.resonious.sburb.abstracts.Vector3
import net.resonious.sburb.Sburb
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.Entity
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData
import net.minecraft.client.renderer.entity.Render
import net.minecraft.client.renderer.Tessellator
import net.minecraft.world.World
import org.lwjgl.opengl.GL11
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ResourceLocation
import scala.math._
import scala.util.Random
import net.resonious.sburb.game.SburbProperties
import net.minecraft.util.ResourceLocation
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderHelper
import net.resonious.sburb.packets.ActivePacket
import net.resonious.sburb.abstracts.PacketPipeline
import com.xcompwiz.mystcraft.api.impl.InternalAPI
import com.xcompwiz.mystcraft.world.agedata.AgeData
import net.minecraftforge.common._
import cpw.mods.fml.common.registry._
import io.netty.buffer.ByteBuf

abstract class Portal(world: World) extends Entity(world) with IEntityAdditionalSpawnData {
  var targetPos: Vector3[Int] = new Vector3[Int](0, 50, 0)
  var targetDim: Int = 0
  var color: Vector3[Float] = new Vector3[Float](1, 1, 1)

  val warpRadius: Double = 1.25

  def setColorFromString(colorStr: String) = {
    color = colorStr match {
      case "Black"  => new Vector3[Float](0f, 0f, 0f)
      case "Red"    => new Vector3[Float](1f, 0f, 0f)
      case "Green"  => new Vector3[Float](0f, 1f, 0f)
      case "Blue"   => new Vector3[Float](0f, 0f, 1f)
      case "Yellow" => new Vector3[Float](1f, 0.9f, 0f)
      case "White"  => new Vector3[Float](0f, 0f, 0f)
      case what     => {
        if (what != "any")
          Sburb log "Unexpected color "+what+" for portal!"
        new Vector3[Float](rand.nextFloat, rand.nextFloat, rand.nextFloat)
      }
    }
  }

  def setColorFromWorld(): Portal = {
    if (InternalAPI.dimension.isMystcraftAge(world.provider.dimensionId)) {
      val age = AgeData.getAge(world.provider.dimensionId, Sburb.isClient)

      val colorStr = age.cruft.get("sburbcolor")
      setColorFromString(colorStr.toString.replace("\"", ""))
    }
    else
      setColorFromString("any")
    this
  }

  override def onCollideWithPlayer(player: EntityPlayer): Unit = {
    if (Sburb.isServer) {
      (posX - player.posX, posZ - player.posZ) match {
        case (x, y) => if (sqrt(x*x+y*y) <= warpRadius) {
          val props = SburbProperties of player
          if (!props.serverMode.activated)
            Sburb.warpPlayer(player, targetDim, targetPos)
        }
      }
    }
  }

  override def writeSpawnData(buf: ByteBuf): Unit = {
    buf.writeFloat(color.r)
    buf.writeFloat(color.g)
    buf.writeFloat(color.b)
  }
  override def readSpawnData(buf: ByteBuf): Unit = {
    color.r = buf.readFloat
    color.g = buf.readFloat
    color.b = buf.readFloat
  }

  override def writeEntityToNBT(comp: NBTTagCompound): Unit = {
    comp.setInteger("targetDim", targetDim)
    comp.setInteger("targetPosX", targetPos.x)
    comp.setInteger("targetPosY", targetPos.y)
    comp.setInteger("targetPosZ", targetPos.z)
    comp.setFloat("colorR", color.r)
    comp.setFloat("colorG", color.g)
    comp.setFloat("colorB", color.b)
  }
  override def readEntityFromNBT(comp: NBTTagCompound): Unit = {
    targetDim = comp.getInteger("targetDim")
    targetPos.x = comp.getInteger("targetPosX")
    targetPos.y = comp.getInteger("targetPosY")
    targetPos.z = comp.getInteger("targetPosZ")
    color.r = comp.getFloat("colorR")
    color.g = comp.getFloat("colorG")
    color.b = comp.getFloat("colorB")
  }
}