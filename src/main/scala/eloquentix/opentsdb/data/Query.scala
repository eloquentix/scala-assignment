package eloquentix
package opentsdb
package data

import play.api.libs.json.{ Format, Json }

case class Query(
  metric: String,
  aggregator: String = "none",
  tags: Map[String, String] = Map.empty,
)

object Query {
  implicit val format: Format[Query] = Json.format[Query]
}
