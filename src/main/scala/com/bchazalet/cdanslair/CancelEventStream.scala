package com.bchazalet.cdanslair

import scala.concurrent._

/** a stream of cancel events */
trait CancelEventStream {
  
  type CancelEvent = Unit
  
  def next: Future[CancelEvent]
  
  def stop(): Unit
  
}

/** stream of cancel events generated by EOF (Ctrl+D) events from console input */
class ConsoleEOFEventStream(implicit ec: ExecutionContext) extends CancelEventStream {
  
  var current = Promise.successful[Unit]()
      
  private def spawn() = {
    val p: Promise[Unit] = Promise()
    p.completeWith(
      Future( 
        for(_ <- io.Source.stdin.getLines){
          // does nothing, just waits for EOF
        }
      )
    )
  }
  
  /** here we don't spawn a new future, until someone ask for it */
  override def next = synchronized {
    if(current.isCompleted){
      current = spawn()
    }
    current.future
  }
  
  override def stop() = ()
  
}