# Functional Actors

This project aims to implement actors from the actor model using pure functional programming with Cats-Effect.

The interface for creating and interacting with actors is heavily inspired by Akka actors, particularly with `Behaviours` being used as the main way to define message handling functionality.

## Example

```scala
sealed trait Command
case class Ping[F[_]](sender: ActorRef[F, Command]) extends Command
case object Pong extends Command

def msgHandler[F[_]: Console]: Behaviour[F, Command].receive { (context, message) =>
  message match {
    case Pong =>
      for {
        _ <- Console[F].println("Pong")
      } yield ()
    case Ping(sender) =>
      for {
        _ <- Console[F].println("Ping")
        self <- context.self
        _ <- sender ! Pong(self)
      } yield ()
  }
  msgHandler
}

val program = for {
  actorSystem <- ActorSystem[IO, Command](Behaviour.empty, "system")
  pinger <- actorSystem.spawn[Command](msgHandler, "pinger")
  ponger <- actorSystem.spawn[Command](msgHandler, "ponger")
  pinger ! Ping(ponger)
  _ <- actorSystem.cancel
} yield ()

```

## TODO

1. Implement the ask pattern found in Akka.
2. Implement support for communication between actors on different JVMs.
3. Implement additional failure recovery of actors.
