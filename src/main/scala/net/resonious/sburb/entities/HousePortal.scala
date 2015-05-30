package net.resonious.sburb.entities

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
import net.minecraft.util.ResourceLocation
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderHelper
import net.resonious.sburb.packets.ActivePacket
import net.resonious.sburb.abstracts.PacketPipeline

object HousePortalRenderer extends Render {
  lazy val t = Tessellator.instance
  lazy val mc = Minecraft.getMinecraft
  val tex = new ResourceLocation("sburb", "textures/tile_entities/houseportal.png")

  implicit class VecTup(tup: (Double, Double)) {
    def x = tup match { case (n, _) => n }
    def y = tup match { case (_, n) => n }

    def dot(other: (Double, Double)): Double = tup match {
      case (x1, y1) => other match {
        case (x2, y2) => x1 * x2 + y1 * y2
      }
    }

    def -(other: (Double, Double)): (Double, Double) = tup match {
      case (x1, y1) => other match {
        case (x2, y2) => (x1 - x2, y1 - y2)
      }
    }

    def +(other: (Double, Double)): (Double, Double) = tup match {
      case (x1, y1) => other match {
        case (x2, y2) => (x1 + x2, y1 + y2)
      }
    }

    def magnitude = tup match { case (x, y) => math.sqrt(pow(x, 2) + pow(y, 2)) }

    def disp = tup match { case (x, y) => "<"+x+", "+y+">" }
  }

  final val thetaMax = 20*Pi

  // Radius of the entire epicycloid
  final val radius: Double = 1.3
  // kN (k numerator) is a constant prime number so that we don't get any insane dips in
  // number of cusps.
  final val kN: Double = 11.0
  // k = 11/7 looks a lot like the Sburb portals, so that's where we start.
  final val initial_kD: Double = 7.0
  // Some algebra on the kD formula down there, so that we can start out at k = 11/7.
  final val initial_r: Double = radius / (1.0 + kN/initial_kD) // ~0.5055555556
  // Calculates the denominator of r relative to a given r. If you pass in initial_r, you
  // should get 7.0!
  def kD(r: Double) = (kN * r) / (radius - r)
  // def k(r: Double) = kN / kD(r)

  override def doRender(entity: Entity, x: Double, y: Double, z: Double, yaw: Float, dt: Float) = {
    val portal = entity.asInstanceOf[HousePortal]
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

    var theta = 0.0
    var point1: (Double, Double) = null
    var point2: (Double, Double) = null
    while (theta <= thetaMax) {
      if (point1 == null) {
        point1 = epicycloid(theta)
      } else {
        point2 = epicycloid(theta)

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
    }
    // printedShit = true

    t.draw()

    GL11.glPopMatrix()
    mc.entityRenderer.enableLightmap(0)
    RenderHelper.enableStandardItemLighting()
  }

  override def getEntityTexture(entity: Entity): ResourceLocation = {
    return null
  }
}

// TODO make ActiveEntity or something...?
class HousePortal(
  world: World,
  var targetPos: Vector3[Int],
  var targetDim: Int,
  var color: Vector3[Float]
) extends Entity(world) {
  var r: Double = HousePortalRenderer.initial_r
  // Angle used to fluctuate r
  var phi: Double = 0.0

  var pulsatePlz = false

  def this(world: World, targetPos: Vector3[Int], targetDim: Int) = this(world, targetPos, targetDim, new Vector3[Float](1f, 1f, 1f))
  def this(world: World) = this(world, new Vector3[Int](0, 50, 0), 0, new Vector3[Float](1f, 1f, 1f))

  override def entityInit(): Unit = {
    phi = 0
    if (Sburb.isClient) pulsatePlz = true
  }

  // TODO we probably need a not-shitty bounding box
  override def onCollideWithPlayer(player: EntityPlayer): Unit = {
    if (Sburb.isClient)
      pulsatePlz = true

    Sburb.warpPlayer(player, targetDim, targetPos)
  }

  override def onUpdate(): Unit = {
    super.onUpdate()

    if (Sburb.isClient) {
      if (phi == 0.0 && !pulsatePlz) return
      pulsatePlz = false

      phi += (Pi / 2.0) / 20.0

      if (phi == Pi)
        phi = 0
      else if (phi > Pi)
        phi = Pi

      r = HousePortalRenderer.initial_r - 0.3 * sin(phi)
    }
  }

  override def readEntityFromNBT(comp: NBTTagCompound): Unit = {
    comp.setInteger("targetDim", targetDim)
    comp.setInteger("targetPosX", targetPos.x)
    comp.setInteger("targetPosY", targetPos.y)
    comp.setInteger("targetPosZ", targetPos.z)
    comp.setFloat("colorR", color.r)
    comp.setFloat("colorG", color.g)
    comp.setFloat("colorB", color.b)
  }
  override def writeEntityToNBT(comp: NBTTagCompound): Unit = {
    targetDim = comp.getInteger("targetDim")
    targetPos.x = comp.getInteger("targetPosX")
    targetPos.y = comp.getInteger("targetPosY")
    targetPos.z = comp.getInteger("targetPosZ")
    color.r = comp.getFloat("colorR")
    color.g = comp.getFloat("colorG")
    color.b = comp.getFloat("colorB")
  }
}