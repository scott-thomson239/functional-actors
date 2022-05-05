package functional.actors

import cats._
import cats.effect._
import cats.syntax.all
import cats.data._
import cats.implicits._
import cats.effect.implicits._

trait ActorRef[F[_], T] {

  def tell(msg: T): F[Unit]

  def !(msg: T): F[Unit]

  def cancel: F[Unit]
}

class ActorRefImpl[F[_]: Monad, T](actor: Actor[F, T], removeFromActorMap: () => F[Unit]) extends ActorRef[F, T] {

  def tell(msg: T): F[Unit] = actor.tell(msg)

  def !(msg: T): F[Unit] = tell(msg)

  def cancel: F[Unit] = for {
    _ <- removeFromActorMap()
    _ <- actor.cancel
  } yield ()
}

object ActorRef {

  def apply[F[_]: Monad, T](actor: Actor[F, T], removeFromActorMap: () => F[Unit]): F[ActorRef[F, T]] =
    Applicative[F].pure(new ActorRefImpl(actor, removeFromActorMap))

}
