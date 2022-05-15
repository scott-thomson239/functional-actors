package functional.actors

import cats._
import cats.effect._
import cats.effect.syntax.concurrent._
import cats.effect.{Deferred, Ref}
import cats.effect.std.Queue
import cats.syntax.all
import cats.data._
import cats.effect.implicits._
import cats.implicits._

class Actor[F[_]: Concurrent, T](actorContext: ActorContext[F, T], inputQueue: Queue[F, T], receiveLoop: Fiber[F, Throwable, Unit], isCancelled: Deferred[F, Unit]) {

  def tell(msg: T): F[Unit] = for {
    option <- isCancelled.tryGet
    _ <- if (option.isDefined) Concurrent[F].raiseError(new Exception("Actor cancelled")) else Concurrent[F].unit
  } yield inputQueue.offer(msg)

  def cancel: F[Unit] = 
    for {
    _ <- receiveLoop.cancel
    _ <- isCancelled.complete()
    children <- actorContext.children
    _ <- children.traverse(_.cancel)
  } yield ()
}

object Actor {

  def apply[F[_]: Concurrent, T](actorContext: ActorContext[F, T], behaviour: Behaviour[F, T]): F[Actor[F, T]] = {
    for {
      inputQueue <- Queue.unbounded[F, T]
      behaviourRef <- Ref.of(behaviour)
      isCancelled <- Deferred[F, Unit]
      receiveLoop <- (for {
        message <- inputQueue.take
        behaviour <- behaviourRef.get
        newBehaviour <- behaviour.receive(actorContext, message)
        _ <- behaviourRef.set(newBehaviour)
      } yield ()).foreverM[Unit].start
    } yield new Actor[F, T](actorContext, inputQueue, receiveLoop, isCancelled)
  }
}
