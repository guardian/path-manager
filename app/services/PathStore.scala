package services

import play.api.Logger
import scala.collection.JavaConversions._
import com.amazonaws.services.dynamodbv2.document.{AttributeUpdate, KeyAttribute, RangeKeyCondition, Item}
import play.api.libs.json.Json

object PathStore {

  def registerNew(path: String, system: String) = {

    val existingPath = Option(Dynamo.pathsTable.getItem("path", path)).map(PathRecord(_))

    existingPath match {
      case Some(_) => Left("path already in use")
      case None => {
        val id = IdentifierSequence.getNextId
        val pathRecord = PathRecord(path, "canonical", id, system)
        val shortUrlPathRecord = PathRecord("simulatedShort/" + path, "short", id, system)

        Dynamo.pathsTable.putItem(pathRecord.asDynamoItem)

        Right(List(pathRecord, shortUrlPathRecord))
      }
    }
  }

  def register(path: String, id: Long, system: String) = {

    val existingPath = Option(Dynamo.pathsTable.getItem("path", path)).map(PathRecord(_))

    existingPath match {
      case Some(pr) if (pr.identifier != id) => Left("path already in use")
      case _ => {
        val pathRecord = PathRecord(path, "canonical", id, system)
        val shortUrlPathRecord = PathRecord("simulatedShort/" + path, "short", id, system)

        Dynamo.pathsTable.putItem(pathRecord.asDynamoItem)

        Right(List(pathRecord, shortUrlPathRecord))
      }
    }
  }

  def updateCanonical(newPath: String, existingPath: String, id: Long) = {

    val newPathRecord = Option(Dynamo.pathsTable.getItem("path", newPath)).map(PathRecord(_))
    val canonicalPathsForId = Dynamo.pathsTable.getIndex("id-index").query(new KeyAttribute("identifier", id), new RangeKeyCondition("type").eq("canonical"))
    val canonicalPathForId = canonicalPathsForId.map{ PathRecord(_) }.headOption

    if(newPathRecord.exists(_.identifier != id)) {
      Left("path already in use")
    } else if (canonicalPathForId.exists(_.path != existingPath)) {
      Left(s"$existingPath is not owned by $id")
    } else {
      canonicalPathForId.map { existingRecord: PathRecord =>
        if (existingPath != newPath) {
          val newRecord = existingRecord.copy(path = newPath)
          Dynamo.pathsTable.deleteItem("path", existingPath)
          Dynamo.pathsTable.putItem(newRecord.asDynamoItem)
          newRecord
        } else {
          existingRecord
        }
      }.toRight(s"unable to find canonical record for $id")
    }
  }

  def getPathDetails(path: String) = {
    Option(Dynamo.pathsTable.getItem("path", path)).map(PathRecord(_))
  }
}

object PathRecord {
  def apply(item: Item): PathRecord = PathRecord(
    path = item.getString("path"),
    `type` = item.getString("type"),
    identifier = item.getLong("identifier"),
    system = item.getString("system")
  )
}

case class PathRecord(path: String, `type`: String, identifier: Long, system: String) {
  def asJson = Json.obj(
    "path" -> path,
    "identifier" -> identifier,
    "type" -> `type`,
    "system" -> system
  )

  def asDynamoItem = new Item()
    .withString("path", path)
    .withLong("identifier", identifier)
    .withString("type", `type`)
    .withString("system", system)
}
