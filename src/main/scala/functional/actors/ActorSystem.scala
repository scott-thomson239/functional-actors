package functional.actors

import cats.{Applicative, MonadError}
import cats.effect._
import cats.syntax.all
import cats.data._
import cats.implicits._
import cats.effect.implicits._

trait ActorSystemAlgebra[F[_], T] extends ActorRefAlgebra[F, T] {

  def spawn[U](behaviour: Behaviour[F, U], name: String): F[ActorRef[F, U]]

  def search[U](path: String): F[ActorRef[F, U]]

}

class ActorSystem[F[_]: Async, T](rootBehaviour: Behaviour[F, T], name: String, actorRefMap: Ref[F, Map[String, ActorRef[F, Any]]]) extends ActorSystemAlgebra[F, T] {

  val rootActor: F[ActorRef[F, T]] = for {
    actorContext <- ActorContext(name, this)
    rootActor <- Actor(actorContext, rootBehaviour)
    rootActorRef <- ActorRef(rootActor, removeFromActorMap(name))
    _ <- actorRefMap.update(_ + (name -> rootActorRef.asInstanceOf[ActorRef[F, Any]]))
  } yield rootActorRef

  def cancel: F[Unit] = rootActor.flatMap(_.cancel)

  def tell(msg: T): F[Unit] = rootActor.flatMap(_.tell(msg))

  def !(msg: T): F[Unit] = tell(msg)

  def spawn[U](behaviour: Behaviour[F, U], path: String): F[ActorRef[F, U]] = for {
    map <- actorRefMap.get
    _ <- if (map.contains(path)) Async[F].raiseError(new Exception(s"Actor $path already exists")) else Async[F].unit
    childPath <- Applicative[F].pure(path)
    childContext <- ActorContext(childPath, this)
    childActor <- Actor(childContext, behaviour)
    actorRef <- ActorRef(childActor, removeFromActorMap(childPath))
    _ <- actorRefMap.update(_ + (path -> actorRef.asInstanceOf[ActorRef[F, Any]]))
  } yield actorRef

  def search[U](path: String): F[ActorRef[F, U]] = for {
    map <- actorRefMap.get
    _ <- if (!map.contains(path)) Async[F].raiseError(new Exception(s"Actor $path does not exist")) else Async[F].unit
  } yield map(path).asInstanceOf[ActorRef[F, U]]

  def removeFromActorMap(path: String): () => F[Unit] = () => for {
    _ <- actorRefMap.update(_ - path)
  } yield ()
}

object ActorSystem {

  def apply[F[_]: Async, T](rootBehaviour: Behaviour[F, T], name: String): F[ActorSystem[F[_], T]] = for {
    actorRefMap <- Ref[F].of[Map[String, ActorRef[F, Any]]](Map.empty)
    actorSystem <- Sync[F].delay(new ActorSystem(rootBehaviour, name, actorRefMap))
  } yield actorSystem

}
