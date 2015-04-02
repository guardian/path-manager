package model

import com.amazonaws.services.dynamodbv2.document.Item
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Format}

object PathRecord {

  implicit val pathRecordReads: Format[PathRecord] = (
    (JsPath \ "path").format[String] and
      (JsPath \ "identifier").format[Long] and
      (JsPath \ "type").format[String] and
      (JsPath \ "system").format[String]
    )(PathRecord.apply, unlift(PathRecord.unapply))


  def apply(item: Item): PathRecord = PathRecord(
    path = item.getString("path"),
    `type` = item.getString("type"),
    identifier = item.getLong("identifier"),
    system = item.getString("system")
  )
}

case class PathRecord(path: String, identifier: Long, `type`: String, system: String) {

  def asDynamoItem = new Item()
    .withString("path", path)
    .withLong("identifier", identifier)
    .withString("type", `type`)
    .withString("system", system)
}
