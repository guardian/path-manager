package model

import com.amazonaws.services.dynamodbv2.document.Item
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Format}

object PathRecord {

  implicit val pathRecordReads: Format[PathRecord] = (
    (JsPath \ "path").format[String] and
      (JsPath \ "identifier").format[Long] and
      (JsPath \ "type").format[String] and
      (JsPath \ "system").format[String] and
      (JsPath \ "lastModified").formatNullable[Long] and
      (JsPath \ "isRemoved").formatNullable[Boolean]
    )(PathRecord.apply, unlift(PathRecord.unapply))


  def apply(item: Item): PathRecord = PathRecord(
    path = item.getString("path"),
    identifier = item.getLong("identifier"),
    `type` = item.getString("type"),
    system = item.getString("system"),
    lastModified = if(item.hasAttribute("lastModified")) Some(item.getLong("lastModified")) else None,
    isRemoved = if(item.hasAttribute("isRemoved")) Some(item.getBoolean("isRemoved")) else None
  )
}

case class PathRecord(
  path: String,
  identifier: Long,
  `type`: String,
  system: String,
  lastModified: Option[Long] = None,
  isRemoved: Option[Boolean] = None
) {

  def asDynamoItem = {
    val baseItem = new Item()
      .withString("path", path)
      .withLong("identifier", identifier)
      .withString("type", `type`)
      .withString("system", system)

    val intermediateItem = lastModified.fold(
      baseItem
    )(
      baseItem.withLong("lastModified", _)
    )

    isRemoved.fold(
      intermediateItem
    )(
      intermediateItem.withBoolean("isRemoved", _)
    )
  }

}
