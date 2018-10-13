package eloquentix
package tool

import java.time.Instant
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import play.api.libs.json.{ Json, JsValue }
import play.api.libs.ws.{ JsonBodyWritables, StandaloneWSResponse }
import play.api.libs.ws.ahc.StandaloneAhcWSClient

/**
 * This class will generate random values between 100 and 110, for metrics
 * "metric-a", "metric-b", "metric-c", for the time interval:
 *
 *    2018-05-01T00:10:00.00Z - 2018-05-01T00:20:00.00Z
 *
 * The chosen time interval and metric names are meant to test the roll-up
 * implementations found in `RollupAB` and `RollupAC`. So, you'll want to
 * use this little helper before trying to test your solution with a running
 * OpenTSDB database instance.
 *
 * Use the following link to verify in the UI that it generated data:
 *
 * http://localhost:4242/#start=2018/05/01-00:09:00&end=2018/05/01-00:21:00&m=sum:metric-a&o=&m=sum:metric-b&o=&m=sum:metric-c&o=&yrange=%5B90:120%5D&wxh=1420x645&style=linespoint
 */
object MetricGenerator extends JsonBodyWritables {
  private val OPENTSDB_HOST = "localhost"
  private val OPENTSDB_PORT = 4242
  private val system = ActorSystem()
  private val materializer = ActorMaterializer()(system)
  private val WS = StandaloneAhcWSClient()(materializer)

  def main(args: Array[String]): Unit = {
    val start = Instant.parse("2018-05-01T00:10:00.00Z")
    val end = start.plusSeconds(10 * 60)

    println(s"start: ${start.getEpochSecond}")
    println(s"end: ${end.getEpochSecond}")

    val task = insert(start, end).map { responses =>
      responses.zipWithIndex.foreach { case (response, i) =>
        println(s"response $i: ${response.status} ${response.body}")
      }
    }

    Await.result(task.runAsync, 3.hours)

    shutdown()
  }

  private def shutdown(): Unit = {
    WS.close()
    materializer.shutdown()
    Await.result(system.terminate().map(_ => ()), Duration.Inf)
  }

  private def insert(start: Instant, end: Instant): Task[List[StandaloneWSResponse]] = {
    val records =
      makeRecords(start, end, "metric-a") ++
      makeRecords(start, end, "metric-b") ++
      makeRecords(start, end, "metric-c")

    Task.sequence {
      records.grouped(50).map(insertChunk).toList
    }
  }

  private def insertChunk(chunk: List[JsValue]): Task[StandaloneWSResponse] =
    Task.deferFutureAction { implicit scheduler =>
      val json = Json.toJson(chunk)
      WS.url(s"http://$OPENTSDB_HOST:$OPENTSDB_PORT/api/put?details").post(json)
    }

  private def makeRecords(start: Instant, end: Instant, metric: String): List[JsValue] =
    Iterator.iterate(start)(_.plusSeconds(1))
      .takeWhile(_.isBefore(end))
      .map(makeRecord(metric))
      .toList

  private def makeRecord(metric: String)(instant: Instant): JsValue =
    Json.obj(
      "metric" -> metric,
      "timestamp" -> instant.getEpochSecond,
      "value" -> (100 + Random.nextInt(10)),
      "tags" -> Json.obj(
        "tag-a" -> "val-a",
        "tag-b" -> "val-b",
      )
    )
}
