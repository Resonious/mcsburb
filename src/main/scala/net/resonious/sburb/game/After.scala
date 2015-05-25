package net.resonious.sburb.game

import scala.collection.mutable.ArrayBuffer

object TimedEvent {
  var timedEvents = new ArrayBuffer[TimedEvent]
  private var toRemove = new ArrayBuffer[TimedEvent]

  def tick() = {
    timedEvents foreach { event =>
      if (event.tick())
        toRemove += event
    }
    toRemove foreach { event =>
      timedEvents -= event
    }
    toRemove.clear()
  }
}
trait TimedEvent {
  def tick(): Boolean
}

/*
 * Easy delayed execution:
 * 
 * After(5, 'seconds) execute { Unit => println("whoaaa") }
 */
object After {
  class Event(target: Int, function: => Unit) extends TimedEvent {
    var timer: Int = 0
    def tick(): Boolean = {
      timer += 1
      if (timer >= target) {
        function
        true
      }
      else false
    }
  }

  class Preliminary(amount:Int) {
    def execute(f: => Unit) = TimedEvent.timedEvents += new Event(amount, f)
  }
	def apply(amount: Int, scale: Symbol) = {
    val actualAmount = scale match {
      case 'ticks | 'tick => amount
	    case 'seconds | 'second => amount * 20
	    case 'minutes | 'minute => amount * 20 * 60
	    case 'hours | 'hour => amount * 20 * 60 * 60
	  }
    
    new Preliminary(actualAmount)
	}
}