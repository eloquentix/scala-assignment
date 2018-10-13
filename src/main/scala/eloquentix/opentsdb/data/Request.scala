package eloquentix
package opentsdb
package data

import java.time.Instant
import play.api.libs.json.{ Json, JsString, Writes }

case class Request(
  start: Instant,
  end: Instant,
  queries: List[Query],
) {
  def interval: Interval = start -> end
}

object Request {
  implicit val instantWrites: Writes[Instant] =
    Writes(instant => JsString(instant.getEpochSecond.toString))

  implicit val writes: Writes[Request] = Json.writes[Request]
}
