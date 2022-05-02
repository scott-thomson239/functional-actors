package functional.actors

import cats._
import cats.effect._
import cats.effect.syntax.concurrent._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.effect.{Deferred, Ref}
import cats.effect.std.Queue
import cats.syntax.all
import cats.data._
import cats.effect.implicits._
import cats.implicits._

class Actor[F[_]: Concurrent, T](actorContext: ActorContext[F, T], inputQueue: Queue[F, T], receiveLoop: Fiber[F, Throwable, Nothing]) {

  def tell(msg: T): F[Unit] = inputQueue.offer(msg)

  def cancel: F[Unit] = for {
    _ <- receiveLoop.cancel
    children <- actorContext.children
    _ <- children.traverse(_.cancel)
  } yield ()
}

object Actor {

  def apply[F[_]: Concurrent: Spawn, T](actorContext: ActorContext[F, T], behaviour: Behaviour[F, T]): F[Actor[F, T]] = for {
      inputQueue <- Queue.unbounded[F, T]
      behaviourRef <- Ref.of(behaviour)
      receiveLoop <- (for { ///maybe monadcancel
        message <- inputQueue.take
        behaviour <- behaviourRef.get
        newBehaviour <- behaviour.receive(actorContext, message)
        _ <- behaviourRef.set(newBehaviour)
      } yield ()).foreverM.start
    } yield new Actor[F, T](actorContext, inputQueue, receiveLoop)
}
