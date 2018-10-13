package eloquentix
package rollup

import java.time.Instant
import cats.Applicative
import eloquentix.opentsdb.OpenTSDB
import eloquentix.opentsdb.data.{ Query, Request, Result }

/**
 * A roll-up that needs two metrics: A and C, hence the class name suffix.
 */
final class RollupAC[F[_]: Applicative](tsdb: OpenTSDB[F]) {
  def run: F[Double] = {
    val metricA = Request(
      start = Instant.parse("2018-05-01T00:10:00Z"),
      end = Instant.parse("2018-05-01T00:10:05Z"),
      queries = List(
        Query(metric = "metric-a")
      )
    )

    val metricC = Request(
      start = Instant.parse("2018-05-01T00:10:00Z"),
      end = Instant.parse("2018-05-01T00:10:05Z"),
      queries = List(
        Query(metric = "metric-c")
      )
    )

    (tsdb.fetchOne(metricA), tsdb.fetchOne(metricC)).mapN(rollup)
  }

  private def rollup(resultA: Result, resultC: Result): Double =
    resultA
      .dps
      .zip(resultC.dps)
      .map { case (a, c) => a.value + c.value }
      .sum
}
