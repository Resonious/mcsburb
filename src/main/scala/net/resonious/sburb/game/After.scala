package net.resonious.sburb.game

import scala.collection.mutable.ArrayBuffer

/*
 * Easy delayed execution:
 * 
 * After(5, 'seconds) execute { Unit => println("whoaaa") }
 */
object After {
  class TimedEvent(target: Int, function: => Unit) {
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
  private var timedEvents = new ArrayBuffer[TimedEvent]
  private var toRemove = new ArrayBuffer[TimedEvent]
  
  class Preliminary(amount:Int) {
    def execute(f: => Unit) = timedEvents += new TimedEvent(amount, f)
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