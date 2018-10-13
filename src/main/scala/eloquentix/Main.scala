package eloquentix

import scala.concurrent.Await
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import eloquentix.http.HTTPClient
import eloquentix.opentsdb.DirectOpenTSDB
import eloquentix.rollup.{ RollupAB, RollupAC }
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import play.api.libs.ws.ahc.StandaloneAhcWSClient

object Main {
  /**
   * Run in the unoptimized mode:
   * ```
   * sbt:opentsdb-client> runMain eloquentix.Main unoptimized
   * ```
   *
   * Run in the optimized mode:
   * ```
   * sbt:opentsdb-client> runMain eloquentix.Main optimized
   * ```
   */
  def main(args: Array[String]): Unit = {
    val mode = args.headOption.getOrElse("unoptimized")
    val task = if (mode == "optimized") optimized else unoptimized
    run(task)
  }

  def unoptimized: Task[Unit] = {
    val directTSDBClient = DirectOpenTSDB.withDefaults(new HTTPClient(ws))
    val rollupAB = new RollupAB(directTSDBClient)
    val rollupAC = new RollupAC(directTSDBClient)

    val taskAB: Task[Unit] =
      rollupAB.run.map(total => println(s"Roll-Up AB: $total."))

    val taskAC: Task[Unit] =
      rollupAC.run.map(total => println(s"Roll-Up AC: $total."))

    val tasks: List[Task[Unit]] =
      List(taskAB, taskAC)

    tasks.sequence_
  }

  def optimized: Task[Unit] =
    // TODO: Exercise 2
    //
    // Reimplement `unoptimized` here, such that it uses `BatchingOpenTSDB`
    // and verify that instead of sending four separate HTTP requests to the
    // OpenTSDB server, it only sends one request containing three queries.
    ???


  // You shouldn't have to modify any of the code that follows. It's part of
  // the running infrastructure.
  private lazy val system = ActorSystem()
  private lazy val materializer = ActorMaterializer()(system)
  private lazy val ws = StandaloneAhcWSClient()(materializer)

  private def run(task: Task[Unit]): Unit =
    try {
      task.runSyncUnsafe(timeout = 5.seconds)
    } catch {
      case e: Exception => e.printStackTrace()
    } finally {
      shutdown()
    }

  private def shutdown(): Unit = {
    ws.close()
    materializer.shutdown()
    Await.result(system.terminate(), Duration.Inf)
  }
}
