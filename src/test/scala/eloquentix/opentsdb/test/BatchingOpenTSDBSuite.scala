package eloquentix
package opentsdb
package test

import java.time.Instant
import cats.data.Writer
import eloquentix.opentsdb.data.{ Query, Request, Result }
import org.scalatest.FunSuite

final class BatchingOpenTSDBSuite extends FunSuite {
  private val `00:00` = Instant.parse("2018-10-13T00:00:00Z")
  private val `00:15` = Instant.parse("2018-10-13T00:15:00Z")
  private val `00:30` = Instant.parse("2018-10-13T00:15:00Z")

  test("batch different requests with identical intervals") {
    val reqA = Request(`00:00`, `00:15`, List(Query(metric = "metric-a")))
    val reqB = Request(`00:00`, `00:15`, List(Query(metric = "metric-b")))

    val executionPlans = List(
      BatchingOpenTSDB.fetchOne(reqA),
      BatchingOpenTSDB.fetchOne(reqB),
    )

    val executionPlan =
      executionPlans.sequence[ExecutionPlan, Result]

    val results = List(
      Result.empty("metric-a"),
      Result.empty("metric-b"),
    )

    val loggingOpenTSDB = LoggingOpenTSDB.withResults(results)
    val batchedRequests = executionPlan.executeWith(loggingOpenTSDB).written

    // Two requests have been optimized to one with two queries.
    assert(batchedRequests.size == 1)
    assert(batchedRequests.head.start == `00:00`)
    assert(batchedRequests.head.end == `00:15`)
    assert(batchedRequests.head.queries.toSet == Set(
      Query(metric = "metric-a"),
      Query(metric = "metric-b"),
    ))
  }

  test("separate requests with different intervals") {
    val reqA = Request(`00:00`, `00:15`, List(Query(metric = "metric-a")))
    val reqB = Request(`00:15`, `00:30`, List(Query(metric = "metric-b")))

    val executionPlans = List(
      BatchingOpenTSDB.fetchOne(reqA),
      BatchingOpenTSDB.fetchOne(reqB),
    )

    val executionPlan =
      executionPlans.sequence[ExecutionPlan, Result]

    val results = List(
      Result.empty("metric-a"),
      Result.empty("metric-b"),
    )

    val loggingOpenTSDB = LoggingOpenTSDB.withResults(results)
    val batchedRequests = executionPlan.executeWith(loggingOpenTSDB).written

    // No optimizations performed.
    assert(batchedRequests.size == 2)
    assert(batchedRequests.toSet == Set(reqA, reqB))
  }

  test("batch and separate at the same time") {
    val reqA = Request(`00:00`, `00:15`, List(Query(metric = "metric-a")))
    val reqB = Request(`00:00`, `00:15`, List(Query(metric = "metric-b")))
    val reqC = Request(`00:15`, `00:30`, List(Query(metric = "metric-c")))

    val executionPlans = List(
      BatchingOpenTSDB.fetchOne(reqA),
      BatchingOpenTSDB.fetchOne(reqB),
      BatchingOpenTSDB.fetchOne(reqC),
    )

    val executionPlan =
      executionPlans.sequence[ExecutionPlan, Result]

    val results = List(
      Result.empty("metric-a"),
      Result.empty("metric-b"),
      Result.empty("metric-c"),
    )

    val loggingClient = LoggingOpenTSDB.withResults(results)
    val batchedRequests = executionPlan.executeWith(loggingClient).written

    // Three requests have been optimized to two.
    assert(batchedRequests.size == 2)

    // First request contains two queries.
    assert(batchedRequests(0).start == `00:00`)
    assert(batchedRequests(0).end == `00:15`)
    assert(batchedRequests(0).queries.toSet == Set(
      Query(metric = "metric-a"),
      Query(metric = "metric-b"),
    ))

    // Second request contains one query.
    assert(batchedRequests(1).start == `00:15`)
    assert(batchedRequests(1).end == `00:30`)
    assert(batchedRequests(1).queries == List(
      Query(metric = "metric-c")
    ))
  }

  // TODO: Exercise 4
  //
  // Write tests for `fetchAll`, similar to those for `fetchOne`.

  object LoggingOpenTSDB {
    type Logged[A] = Writer[List[Request], A]

    /**
     * Create an [[OpenTSDB]] instance that only logs the given requests
     * and produces the responses given as arguments to this method.
     */
    def withResults(responses: List[Result]): OpenTSDB[Logged] =
      new OpenTSDB[Writer[List[Request], ?]] {
        override def fetchOne(req: Request): Logged[Result] =
          fetchAll(req).map(_.head)

        override def fetchAll(req: Request): Logged[List[Result]] =
          Writer(List(req), responses)
      }
  }
}
