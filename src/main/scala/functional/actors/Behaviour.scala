package functional.actors

import cats.MonadError
import cats.effect._
import cats.syntax.all
import cats.data._
import cats.implicits._
import cats.effect.implicits._

trait Behaviour[F[_], T] {

  def receive(context: ActorContext[F, T], message: T): F[Behaviour[F, T]]

}

object Behaviour {

  def receive[F[_]: Sync, T](f: (ActorContext[F, T], T) => Behaviour[F, T]): Behaviour[F, T] =
    (context: ActorContext[F, T], message: T) => Sync[F].delay(f(context, message))

  def receiveMessage[F[_]: Sync, T](f: T => Behaviour[F, T]): Behaviour[F, T] =
    (_, message: T) => Sync[F].delay(f(message))

  def empty[F[_]: Sync, T]: Behaviour[F, T] =
    (_, message: T) => Sync[F].delay {empty}

}
