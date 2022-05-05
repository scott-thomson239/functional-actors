# Functional Actors

This project aims to implement actors from the actor model using pure functional programming with Cats-Effect.

The interface for creating and interacting with actors is heavily inspired by Akka actors, particularly with `Behaviours` being used as the main way to define message handling functionality.

## TODO

1. Implement the ask pattern found in Akka.
2. Implement support for communication between actors on different JVMs.
3. Implement additional failure recovery of actors.
