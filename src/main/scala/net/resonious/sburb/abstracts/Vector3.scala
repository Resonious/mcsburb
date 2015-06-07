package net.resonious.sburb.abstracts

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.Vec3

object Vector3 {
  implicit class IntTuple2Vec3(t: (Int,Int,Int)) {
    def vec() = new Vector3(t._1,t._2,t._3)
  }

  def from[T](f: () => T): Vector3[T] = new Vector3[T](f(), f(), f())
}

class Vector3[T](var x:T=0, var y:T=0, var z:T=0) extends Serializable {
  def this(xyz: (T,T,T)) = this(xyz._1,xyz._2,xyz._3)
  def this(xyz: Array[T]) = this(xyz(0), xyz(1), xyz(2))
  def this(p: EntityPlayer) = this(
      p.posX.asInstanceOf[T],
      p.posY.asInstanceOf[T],
      p.posZ.asInstanceOf[T])
  def this(v: Vector3[T]) = this(v.x, v.y, v.z)

  def r = x
  def g = y
  def b = z

  def r_=(other: T) = x = other
  def g_=(other: T) = y = other
  def b_=(other: T) = z = other
      
  def applyTo(p: EntityPlayer) = {
    p.setPositionAndUpdate(
        x.asInstanceOf[Double],
        y.asInstanceOf[Double], 
        z.asInstanceOf[Double])
  }

  def isZero() = x == 0 && y == 0 && z == 0
  
  def foreach(f: (Symbol, T) => Unit) = {
    f('x, x); f('y, y); f('z, z)
  }
  def foreach(f: (T) => Unit) = {
    f(x);f(y);f(z)
  }
  def map[R](f: (T) => R): Vector3[R] = {
    new Vector3(f(x), f(y), f(z))
  }
  def tupMap[R](f: (T) => R): (R,R,R) = {
    (f(x), f(y), f(z))
  }
  def set(s:Symbol, n:T) = {
    s match {
      case 'x => x=n
      case 'y => y=n
      case 'z => z=n
    }
  }
  def set(_x:T,_y:T,_z:T) = {
    x=_x;y=_y;z=_z
  }
  def apply(s: Symbol) = {
    s match {
      case 'x => x
      case 'y => y
      case 'z => z
    }
  }
  def apply(i: Int) = {
    i match {
      case 0 => x
      case 1 => y
      case 2 => z
    }
  }

  def instead(f: (Vector3[T]) => Unit): Vector3[T] = {
    var v = new Vector3[T](this)
    f(v)
    v
  }

  def update(s:Symbol, value:T) = {
    set(s, value)
  }
  def toParams = {
    
  }
  def tup() = (x,y,z)
  def disp() = "<"+x+", "+y+", "+z+">"
}