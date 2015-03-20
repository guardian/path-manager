package services

import model.PathRecord
import play.api.Logger
import scala.collection.JavaConversions._
import com.amazonaws.services.dynamodbv2.document.{AttributeUpdate, KeyAttribute, RangeKeyCondition, Item}

object PathStore {

  def registerNew(path: String, system: String) = {

    val existingPath = Option(Dynamo.pathsTable.getItem("path", path)).map(PathRecord(_))

    existingPath match {
      case Some(_) => Left("path already in use")
      case None => {
        val id = IdentifierSequence.getNextId
        val pathRecord = PathRecord(path, id, "canonical", system)
        val shortUrlPathRecord = PathRecord("simulatedShort/" + path, id, "short", system)

        Dynamo.pathsTable.putItem(pathRecord.asDynamoItem)

        Right(List(pathRecord, shortUrlPathRecord).groupBy(_.`type`))
      }
    }
  }

  def register(proposedPathRecord: PathRecord) = {

    val id = proposedPathRecord.identifier
    val existingPath = Option(Dynamo.pathsTable.getItem("path", proposedPathRecord.path)).map(PathRecord(_))
    val canonicalPathsForId = Dynamo.pathsTable.getIndex("id-index").query(new KeyAttribute("identifier", id), new RangeKeyCondition("type").eq("canonical"))
    val existingCanonicalPathForId = canonicalPathsForId.map{ PathRecord(_) }.headOption

    existingPath match {
      case Some(pr) if (pr.identifier != id) => Left("path already in use")
      case _ => {
        val shortUrlPathRecord = PathRecord("simulatedShort/" + proposedPathRecord.path, id, "short", proposedPathRecord.system)

        for (oldCanonical <- existingCanonicalPathForId) {
          if (oldCanonical.path != proposedPathRecord.path) {
            Dynamo.pathsTable.deleteItem("path", oldCanonical.path)
          }
        }

        Dynamo.pathsTable.putItem(proposedPathRecord.asDynamoItem)

        Right(List(proposedPathRecord, shortUrlPathRecord).groupBy(_.`type`))
      }
    }
  }

  def updateCanonical(newPath: String, id: Long) = {

    val newPathRecord = Option(Dynamo.pathsTable.getItem("path", newPath)).map(PathRecord(_))
    val canonicalPathsForId = Dynamo.pathsTable.getIndex("id-index").query(new KeyAttribute("identifier", id), new RangeKeyCondition("type").eq("canonical"))
    val canonicalPathForId = canonicalPathsForId.map{ PathRecord(_) }.headOption

    if(newPathRecord.exists(_.identifier != id)) {
      Left("path already in use")
    } else {
      canonicalPathForId.map { existingRecord: PathRecord =>

        val existingPath = existingRecord.path
        val updatedRecord = if (existingPath != newPath) {
            val newRecord = existingRecord.copy(path = newPath)
            Dynamo.pathsTable.deleteItem("path", existingPath)
            Dynamo.pathsTable.putItem(newRecord.asDynamoItem)
            newRecord
          } else {
            existingRecord
          }
        List(updatedRecord).groupBy(_.`type`)
      }.toRight(s"unable to find canonical record for $id")
    }
  }

  def getPathDetails(path: String) = {
    Option(Dynamo.pathsTable.getItem("path", path)).map(PathRecord(_))
  }

  def getPathsById(id: Long) = {
    val pathItems = Dynamo.pathsTable.getIndex("id-index").query(new KeyAttribute("identifier", id))
    val paths = pathItems.map{ PathRecord(_) }
    paths.groupBy(_.`type`)
  }
}