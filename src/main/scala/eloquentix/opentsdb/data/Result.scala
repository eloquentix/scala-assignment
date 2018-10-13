package eloquentix
package opentsdb
package data

import play.api.libs.json.{ Format, Json }

case class Result(
  metric: String,
  tags: Map[String, String],
  aggregateTags: Set[String],
  dps: List[Datapoint],
)

object Result {
  implicit val format: Format[Result] = Json.format[Result]

  def empty(metric: String): Result =
    Result(metric, Map.empty, Set.empty, List.empty)
}
