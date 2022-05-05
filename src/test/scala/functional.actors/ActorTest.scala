package functional.actors

import functional.actors.ActorSystem
import functional.actors.ActorRef
import cats._
import cats.effect._
import cats.effect.syntax.concurrent._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.effect.{Deferred, Ref}
import cats.effect.std.Queue
import cats.syntax.all
import cats.data._
import cats.effect._
import cats.effect.implicits._
import cats.effect.unsafe.implicits.global
import cats.implicits._
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

class ActorTest extends AsyncFunSuite {

  trait Command
  case class Number(num: Int, actorRef: ActorRef[IO, Command]) extends Command
  case class DoSend(toSend: ActorRef[IO, Command]) extends Command
  case object Test extends Command
  case class Done(deferred: Deferred[IO, Boolean]) extends Command

  val Timeout: FiniteDuration = 10.seconds

  implicit val executor: ExecutionContextExecutor = ExecutionContext.global

  test("Messages from multiple actors are processed in the correct order") {

    def receiver(msgMap: Map[ActorRef[IO, Command], Int]): Behaviour[IO, Command] = Behaviour.receiveMessage[IO, Command] {
      case Number(num, sender) =>
        val prev = msgMap.getOrElse(sender, 0)
        receiver(msgMap + (sender -> (prev + 1)))
      case Done(d) =>
        d.complete(msgMap.foldLeft(false)((_, kv) => kv._2 == 10))
        Behaviour.empty
    }

    def sender(iter: Int): Behaviour[IO, Command] = Behaviour.receive[IO, Command] {(context, message) =>
      message match {
        case DoSend(toSend) => for {
          self <- context.self
          _ <- toSend ! Number(iter, self)
        } yield ()
          sender(iter+1)
      }
    }

    val test = for {
      actorSystem <- ActorSystem[IO, Command](receiver(Map.empty), "actor0")
      actor1 <- actorSystem.spawn[Command](sender(0), "actor1")
      actor2 <- actorSystem.spawn[Command](sender(0), "actor2")
      actor3 <- actorSystem.spawn[Command](sender(0), "actor3")
      deferred <- Deferred[IO, Boolean]
      _ <- (1 to 10).foldLeft(IO.unit)((_, _) => for {
        _ <- actor1 ! DoSend(actorSystem)
        _ <- actor2 ! DoSend(actorSystem)
        _ <- actor3 ! DoSend(actorSystem)
        _ <- actorSystem ! Done(deferred)
      } yield ())
      res <- deferred.get
    } yield res

    assert(test.unsafeRunSync())
  }


  test("Sending a message to a cancelled actor results in an exception") {
    val test = for {
      actorSystem <- ActorSystem[IO, Command](Behaviour.empty, "actor0")
      _ <- actorSystem.cancel
      _ <- actorSystem ! Test
    } yield ()

    assertThrows[Exception] {
      test.unsafeRunSync()
    }
  }

  test("Can search for actor using actor system and path name") {
    val test = for {
      actorSystem <- ActorSystem[IO, Command](Behaviour.empty, "actor0")
      actor1 <- actorSystem.spawn(Behaviour.empty[IO, Command], "actor1")
      actor11 <- actorSystem.spawn(Behaviour.empty[IO, Command], "actor1/actor11")
      res <- actorSystem.search[Command]("actor0/actor1/actor11")
    } yield (res, actor11)

    val result = test.unsafeRunSync

    assert(result._1 == result._2)
  }

  test("Cancelling an actor also cancels its children actors") {
    val test = for {
      actorSystem <- ActorSystem[IO, Command](Behaviour.empty, "actor0")
      actor1 <- actorSystem.spawn(Behaviour.empty[IO, Command], "actor1")
      _ <- actorSystem.spawn(Behaviour.empty[IO, Command], "actor1/actor11")
      _ <- actor1.cancel
      _ <- actorSystem.search[Command]("actor0/actor1/actor11")
    } yield ()

    assertThrows[Exception] {
      test.unsafeRunSync()
    }

  }

  test("Non existant actor searches result in an exception") {
    val test = for {
      actorSystem <- ActorSystem[IO, Command](Behaviour.empty, "actor0")
      res <- actorSystem.search[Command]("fake")
    } yield res

    assertThrows[Exception] {
      test.unsafeRunSync()
    }

  }

}
