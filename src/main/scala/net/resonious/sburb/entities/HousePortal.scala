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
    val r = entity.asInstanceOf[HousePortal].r

    GL11.glPushMatrix()
    GL11.glTranslated(x, y, z)
    bindTexture(tex)
    GL11.glColor3f(1f, 0.2f, 0.5f)
    GL11.glDisable(GL11.GL_BLEND)

    mc.entityRenderer.disableLightmap(0)

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

        // Magnitudes of the points
        // val mag1 = point1.magnitude
        // val mag2 = point2.magnitude

        // Angle Between Points
        val abp = (point2 - point1) match {
          case (x12, y12) => atan2(y12, x12)
        }
        // Rotate 90 degrees
        val pCos = cos(abp + Pi/2)
        val pSin = sin(abp + Pi/2)

        // TODO if things get crazy, maybe do some benchmarking with the atan2 vs.
        // this weird method:
        /*
        val pCos = (point1 dot point2) / (mag1 * mag2)
        val pSin =
          if (point1.y < point2.y)
            sqrt(1 - pow(abs(pCos), 2))
          else
            -sqrt(1 - pow(abs(pCos), 2))
        */

        /*
        if (!printedShit) {
          Sburb log "===================THETA:"+theta+" ======================"
          Sburb log "======POINT 1: "+point1.disp+"========="
          Sburb log "======POINT 2: "+point2.disp+"========="
          Sburb log "Cos between: "+pCos
          Sburb log "Sin between: "+pSin
          Sburb log "---------------------------------------------------------"
        }
        */

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

        // TODO make a rectangle out of point1 and point2
        // t.addVertex(x, 0.5, z)
        // t.addVertex(x, 0.5, z+0.1)
        // t.addVertex(x+0.1, 0.5, z+0.1)
        // t.addVertex(x+0.1, 0.5, z)

        point1 = point2
      }
      theta += Pi/60
    }
    // printedShit = true

    t.draw()

    GL11.glPopMatrix()
    mc.entityRenderer.enableLightmap(0)
  }

  override def getEntityTexture(entity: Entity): ResourceLocation = {
    return null
  }
}

// TODO make ActiveEntity or something...?

class HousePortal(world: World, var targetPos: Vector3[Int], var targetDim: Int)
extends Entity(world) {
  // Angle used to fluctuate r
  var r: Double = HousePortalRenderer.initial_r
  var phi: Double = 0.0
  var fluxTimeout: Int = 5 * 20

  def this(world: World) = this(world, new Vector3[Int](0, 50, 0), 0)

  override def entityInit(): Unit = {
    phi = 0
    Sburb log "PORTAL HAS JOINED THE FIGHT"
  }

  // TODO we probably need a not-shitty bounding box
  override def onCollideWithPlayer(player: EntityPlayer): Unit = {
    // Sburb log "WARP!!!!!!!!!"
  }

  override def onUpdate(): Unit = {
    super.onUpdate()

    if (Sburb.isClient) {
      if (fluxTimeout > 0) {
        fluxTimeout -= 1
        return
      }

      phi += (Pi / 2.0) / 20.0

      if (phi == Pi) {
        fluxTimeout = 10
        phi = 0
      }
      else if (phi > Pi) {
        phi = Pi
      }

      r = HousePortalRenderer.initial_r - 0.2 * sin(phi)
    }
  }

  override def readEntityFromNBT(comp: NBTTagCompound): Unit = {
    comp.setInteger("targetDim", targetDim)
    comp.setInteger("targetPosX", targetPos.x)
    comp.setInteger("targetPosY", targetPos.y)
    comp.setInteger("targetPosZ", targetPos.z)
  }
  override def writeEntityToNBT(comp: NBTTagCompound): Unit = {
    targetDim = comp.getInteger("targetDim")
    targetPos.x = comp.getInteger("targetPosX")
    targetPos.y = comp.getInteger("targetPosY")
    targetPos.z = comp.getInteger("targetPosZ")
  }
}