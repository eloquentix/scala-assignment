package eloquentix
package rollup

import java.time.Instant
import cats.Applicative
import eloquentix.opentsdb.OpenTSDB
import eloquentix.opentsdb.data.{ Query, Request, Result }

/**
 * A roll-up that needs two metrics: A and C, hence the class name suffix.
 */
final class RollupAB[F[_]: Applicative](tsdb: OpenTSDB[F]) {
  def run: F[Double] = {
    val metricA = Request(
      start = Instant.parse("2018-05-01T00:10:00Z"),
      end = Instant.parse("2018-05-01T00:10:05Z"),
      queries = List(
        Query(metric = "metric-a")
      )
    )

    val metricB = Request(
      start = Instant.parse("2018-05-01T00:10:00Z"),
      end = Instant.parse("2018-05-01T00:10:05Z"),
      queries = List(
        Query(metric = "metric-b")
      )
    )

    val resultA = tsdb.fetchOne(metricA)
    val resultB = tsdb.fetchOne(metricB)

    // Because we require `tsdb`'s methods to return an `F` that has an
    // `Applicative` instance, we can call `map` or `mapN` over the results
    // returned by any of its methods.
    (resultA, resultB).mapN { (resultA, resultB) =>
      rollup(resultA, resultB)
    }
  }

  // Simple, pure function. Computes an aggregation based on the given results.
  private def rollup(resultA: Result, resultB: Result): Double =
    resultA
      .dps
      .zip(resultB.dps)
      .map { case (a, b) => a.value + b.value }
      .sum
}
