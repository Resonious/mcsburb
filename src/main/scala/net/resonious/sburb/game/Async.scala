package net.resonious.sburb.game

object Async {
	def apply(exp: => Unit) = {
	  val thread = new Thread() { override def run() = exp }
	  thread.start()
	}
	def execute(exp: => Unit) = apply(exp)
}