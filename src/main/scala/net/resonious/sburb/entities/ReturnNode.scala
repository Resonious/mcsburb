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

object ReturnNode {
  // Radius of the entire epicycloid
  final val radius: Double = 1.3
  // kN (k numerator) is a constant prime number so that we don't get any insane dips in
  // number of cusps. (completely stupid it happens anyway because of fractions dummy)
  final val kN: Double = 11.0
  // k = 11/7 looks a lot like the Sburb portals, so that's where we start.
  final val initial_kD: Double = 7.0
  // Some algebra on the kD formula down there, so that we can start out at k = 11/7.
  final val initial_r: Double = radius / (1.0 + kN/initial_kD)
  // Calculates the denominator of r relative to a given r. If you pass in initial_r, you
  // should get 7.0!
  def kD(r: Double) = (kN * r) / (radius - r)
  // def k(r: Double) = kN / kD(r)

  var er: Double = 0.9
  var eR: Double = 1.7
  var ed: Double = 9
  var es: Double = 0.85
}

object ReturnNodeRenderer extends Render {
  import net.resonious.sburb.entities.HousePortalRenderer.VecTup
  import ReturnNode._

  lazy val t = Tessellator.instance
  lazy val mc = Minecraft.getMinecraft
  val tex = new ResourceLocation("sburb", "textures/tile_entities/houseportal.png")

  final val thetaMax = 20*Pi
  var printedShit = false

  override def doRender(entity: Entity, x: Double, y: Double, z: Double, yaw: Float, pitch: Float) = {
    val portal = entity.asInstanceOf[ReturnNode]
    val r = portal.r
    val color = portal.color

    GL11.glPushMatrix()
    GL11.glTranslated(x, y, z)
    bindTexture(tex)
    GL11.glColor3f(color.r, color.g, color.b)
    GL11.glDisable(GL11.GL_BLEND)

    mc.entityRenderer.disableLightmap(0)
    RenderHelper.disableStandardItemLighting()

    t.startDrawingQuads()

    def epicycloid(theta: Double) = {
      val k = kN / kD(r)
      (
        r*(k + 1)*cos(theta) - r*cos((k + 1)*theta),
        r*(k + 1)*sin(theta) - r*sin((k + 1)*theta)
      )
    }
    def otherthing(theta: Double) = {
      (
        es*(eR*cos(theta) + er*sin(ed*theta)),
        es*(eR*sin(theta) + er*cos(ed*theta))
      )
    }

    var pointFunc = otherthing(_)
    var drawCycloid = false

    var theta = 0.0
    var point1: (Double, Double) = null
    var point2: (Double, Double) = null
    while (theta <= thetaMax) {
      if (point1 == null) {
        point1 = pointFunc(theta)
      } else {
        point2 = pointFunc(theta)

        // Angle Between Points
        val abp = (point2 - point1) match {
          case (x12, y12) => atan2(y12, x12)
        }
        // Rotate 90 degrees
        val pCos = cos(abp + Pi/2)
        val pSin = sin(abp + Pi/2)

        // Rectangle size
        val s = 0.025

        val topLeft     = point1 match { case (x, y) => (x + s*pCos, y + s*pSin) }
        val bottomLeft  = point1 match { case (x, y) => (x - s*pCos, y - s*pSin) }
        val topRight    = point2 match { case (x, y) => (x + s*pCos, y + s*pSin) }
        val bottomRight = point2 match { case (x, y) => (x - s*pCos, y - s*pSin) }

        def vert(y: Double, points: (Double, Double)*) =
          points foreach { _ match { case (x, z) => { t.addVertex(x, y, z) } } }

        // Render both sides of rectangles
        vert(0.5, topLeft, topRight, bottomRight, bottomLeft)
        vert(0.5, bottomLeft, bottomRight, topRight, topLeft)

        point1 = point2
      }
      theta += Pi/60

      if (!drawCycloid && theta > thetaMax / 2) {
        drawCycloid = true
        theta = 0
        pointFunc = epicycloid(_)
        point1 = null
      }
    }

    t.draw()

    GL11.glPopMatrix()
    mc.entityRenderer.enableLightmap(0)
    RenderHelper.enableStandardItemLighting()
  }

  override def getEntityTexture(entity: Entity): ResourceLocation = {
    return null
  }
}

class ReturnNode(world: World) extends Portal(world) {
  def r = ReturnNode.initial_r

  override def entityInit(): Unit = {
  }

  override def setColorFromWorld(): Portal = {
    val result = super.setColorFromWorld()
    color = new Vector3[Float](color.r * 0.9f, color.g * 0.9f, color.b * 0.9f)
    result
  }
}