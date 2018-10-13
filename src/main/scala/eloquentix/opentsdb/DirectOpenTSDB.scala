package eloquentix
package opentsdb

import monix.eval.Task
import eloquentix.http.HTTPClient
import eloquentix.opentsdb.data.{ Request, Result }

/**
 * A simplest OpenTSDB client — all requests are *directly* sent over the
 * network, hence its name — using our small [[HTTPClient]] layer to make
 * the actual HTTP calls.
 *
 * A more intelligent client would buffer requests and only execute them after
 * batching and deduplicating the queries in the requests. With this client,
 * all queries are sent as they are, with no prior transformations.
 */
final class DirectOpenTSDB(http: HTTPClient, config: DirectOpenTSDB.Config) extends OpenTSDB[Task] {
  override def fetchOne(req: Request): Task[Result] =
    // This method delagates to its more generic sibling and keeps the first
    // result found in the list of received results.
    fetchAll(req).map(_.head)

  override def fetchAll(req: Request): Task[List[Result]] = {
    // Ask OpenTSDB to send us back the datapoints as arrays, instead of
    // JSON objects, since that's what our Datapoint JSON decoder expects.
    val url = config.url("api/query?arrays")
    http.post[Request, List[Result]](url, req)
  }
}

object DirectOpenTSDB {
  case class Config(host: String, port: String) {
    def url(path: String): String = {
      val noLeadingSlashes = path.replaceAll("^/*", "")
      show"http://$host:$port/$noLeadingSlashes"
    }
  }

  object Config {
    val default: Config =
      Config("localhost", "4242")

    def fromEnvVars(hostKey: String, portKey: String): Option[Config] =
      for {
        host <- sys.env.get(hostKey)
        port <- sys.env.get(portKey)
      } yield {
        Config(host, port)
      }
  }

  def withDefaults(http: HTTPClient): DirectOpenTSDB = {
    val config =
      Config
        .fromEnvVars(hostKey = "OPENTSDB_HOST", portKey = "OPENTSDB_PORT")
        .getOrElse(Config.default)

    new DirectOpenTSDB(http, config)
  }
}
