package net.resonious.sburb.game

object Using {
  class Executor[T](thing: T) {
    def execute(exp: T => Any) = {
      exp(thing)
    }
  }
  
	def apply[T](thing: T) = new Executor[T](thing)
}