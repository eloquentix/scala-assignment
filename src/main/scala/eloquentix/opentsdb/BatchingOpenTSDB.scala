package eloquentix
package opentsdb

import cats.data.{ Kleisli, Reader, State }
import cats.Applicative
import eloquentix.opentsdb.ExecutionPlan.Distributor
import eloquentix.opentsdb.data.{ Request, Result }

/**
 * An [[OpenTSDB]] client that stores seen requests inside an [[ExecutionPlan]].
 *
 * The actual optimizations — batching, query de-duplication, etc. — happen
 * inside [[ExecutionPlan.executeWith]].
 */
object BatchingOpenTSDB extends OpenTSDB[ExecutionPlan] {
  override def fetchOne(req: Request): ExecutionPlan[Result] =
    ExecutionPlan.fetchOne(req)

  override def fetchAll(req: Request): ExecutionPlan[List[Result]] =
    // TODO: Exercise 3
    //
    // So far, the implementation supports just requests which are guaranteed
    // to return a single result, but we need to allow client code to issue
    // queries for which that invariant doesn't hold. Your task here is to
    // extend `ExecutionPlan` so that this is possible.
    ???
}

/**
 * An execution plan is a small wrapper around a state, implemented using the
 * `State` monad provided by the Cats library — it, thus, keeps state in a
 * functional style.
 *
 * It's main responsibility is to keep track of this state and optimize seen
 * requests before delegating their execution to an external [[OpenTSDB]]
 * client.
 */
final case class ExecutionPlan[A](state: State[List[Request], Distributor[A]]) {
  /**
   * Optimize the requests accumulated in this execution plan's state and
   * just then use the underlying [[OpenTSDB]] instance to execute them.
   *
   * The implementation is responsible for mapping back the results to the
   * originating queries.
   */
  def executeWith[F[_]: Applicative](tsdb: OpenTSDB[F]): F[A] = {
    val (
      // This  contains all the requests issued by clients. Your task is to
      // take this list, remove duplicate queries and batch together those
      // that are requesting data for identical time intervals.
      seenRequests: List[Request],

      // This should be used to give back the results, once they're fetched
      // using the passed-in `tsdb` instance.
      resultsDistributor: Distributor[A],
    ) = state.runEmpty.value

    // TODO: Exercise 1
    //
    // Implement the logic of batching and de-duplication, so that the tests
    // in `BatchingOpenTSDBSuite` pass.
    //
    // HINT: you may reach a point where you'll have a `List[F[...]]` and want
    // to transform it to a `F[List[...]]`. One can use `sequence` to achieve
    // that. There are a few examples inside `BatchingOpenTSDBSuite` already.
    ???
  }

  // TODO: Exercise 5
  //
  // The optimizations applied above are of two kinds: de-duplication and
  // batching of queries. Can you think of other possible optimizations?
  // What if the requests send by `BatchingOpenTSDB` are sometimes slower
  // than using the `DirectOpenTSDB` client — because it batches too many
  // queries? What if we can't decide on a best strategy while writing the
  // code, but while running the code? Can you think of an yet another type
  // of client that's smarter than any of the existing two? Leave your answer
  // below.
  //
  // ???

  def ap[B](func: ExecutionPlan[A => B]): ExecutionPlan[B] =
    ExecutionPlan {
      for {
        f <- func.state
        a <- this.state
      } yield {
        a.ap(f)
      }
    }
}

object ExecutionPlan { self =>
  /**
   * A distributor is a [[Reader]] that given a mapping of OpenTSDB requests
   * to results, will **distribute** them back to they requests' issuers.
   *
   * A `Reader` is just a small wrapper around a single-argument function, but
   * comes with some extra goodies, e.g., `Monad` and `Applicative` type-class
   * instances. So, look at it as a function.
   */
  type Distributor[A] = Reader[Results, A]

  /**
   * A map from requests to results is what a distributor requires in order to
   * map back the results to the appropriate request issuers.
   */
  type Results = Map[Request, Result]

  def fetchOne(req: Request): ExecutionPlan[Result] =
    ExecutionPlan {
      for {
        // Prepend the passed-in request to the list of already seen requests.
        seenRequests <- State.get
        _            <- State.set(req :: seenRequests)
      } yield {
        // Finally, we produce a distributor which, when called with the final
        // result map, extracts the result associated with the given request.
        Reader { results: Results =>
          results(req)
        }
      }
    }

  def pure[A](a: A): ExecutionPlan[A] =
    ExecutionPlan(State.pure(Kleisli.pure(a)))

  // You shouldn't have to modify this. It's needed so that client code can
  // `map` (and a couple other things) over the query results.
  implicit object ApplicativeExecutionPlan extends Applicative[ExecutionPlan] {
    override def pure[A](a: A): ExecutionPlan[A] =
      self.pure(a)

    override def ap[A, B](ff: ExecutionPlan[A => B])(fa: ExecutionPlan[A]): ExecutionPlan[B] =
      fa.ap(ff)
  }
}
