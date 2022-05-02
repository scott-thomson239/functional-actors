package functional.actors

import cats._
import cats.effect._
import cats.syntax.all
import cats.data._
import cats.implicits._
import cats.effect.implicits._

trait ActorContextAlgebra[F[_], T] {

  def self: F[ActorRef[F, T]]

  def spawn[U](behaviour: Behaviour[F, U], name: String): F[ActorRef[F, U]]

  def children: F[List[ActorRef[F, Nothing]]]

  def child[U](name: String): F[ActorRef[F, U]]

}

class ActorContext[F[_]: Concurrent, T](path: String, actorSystem: ActorSystem[F, _], childrenList: Ref[F, List[ActorRef[F, Nothing]]]) extends ActorContextAlgebra[F, T] {

  def self: F[ActorRef[F, T]] = actorSystem.search(path)

  def spawn[U](behaviour: Behaviour[F, U], name: String): F[ActorRef[F, U]] =
    actorSystem.spawn(behaviour, path + "/" + name)

  def children: F[List[ActorRef[F, Nothing]]] = childrenList.get

  def child[U](name: String): F[ActorRef[F, U]] = actorSystem.search(path + "/" + name)
}

object ActorContext {

  def apply[F[_]: Concurrent, T](path: String, actorSystem: ActorSystem[F, _]): F[ActorContext[F, T]] = for {
    childSet <- Ref.of[F, List[ActorRef[F, Nothing]]](List.empty)
  } yield new ActorContext(path, actorSystem, childSet)
}
