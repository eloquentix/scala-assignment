package eloquentix
package http

import monix.eval.Task
import play.api.libs.json.{ Json, JsValue, Reads, Writes }
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.JsonBodyWritables._
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.api.libs.ws.StandaloneWSResponse

/**
 * Small wrapper around Play Framework's HTTP client. This client exposes a
 * minimum of features needed when interacting with the OpenTSDB API, i.e.,
 * a way to send and read JSON payloads using POST requests.
 */
final class HTTPClient(ws: StandaloneAhcWSClient) extends Logging {
  def post[W: Writes, R: Reads](url: String, body: W): Task[R] =
    send(url, body).map { response =>
      if (response.status / 100 === 2) {
        val jsonRes = response.body[JsValue]
        log.debug(s"JSON response: ${Json.prettyPrint(jsonRes)}")
        jsonRes.as[R]
      } else {
        throw new RuntimeException(s"HTTP request failed: ${response.status} ${response.body}")
      }
    }

  private def send[W: Writes](url: String, body: W): Task[StandaloneWSResponse] =
    Task.deferFutureAction { implicit scheduler =>
      val jsonReq = Json.toJson(body)
      log.debug(s"JSON request: ${Json.prettyPrint(jsonReq)}")
      ws.url(url).post(jsonReq)
    }
}
