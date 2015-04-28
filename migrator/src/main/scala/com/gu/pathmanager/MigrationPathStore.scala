package com.gu.pathmanager

import com.amazonaws.services.dynamodbv2.document.{Item, RangeKeyCondition, KeyAttribute}
import scala.collection.JavaConversions._

object MigrationPathStore {

  def register(proposedPathRecord: PathRecord) = {

    val id = proposedPathRecord.identifier


    val existingPath = Option(Dynamo.pathsTable.getItem("path", proposedPathRecord.path)).map(PathRecord(_))
    val canonicalPathsForId = Dynamo.pathsTable.getIndex("id-index").query(new KeyAttribute("identifier", id), new RangeKeyCondition("type").eq("canonical"))
    val existingCanonicalPathForId = canonicalPathsForId.map {
      PathRecord(_)
    }.headOption

    existingPath match {
      case Some(pr) if (pr.identifier != id) => {
        println(s"Failed to register existing path [${proposedPathRecord.path}], already claimed by id [${pr.identifier}], submitting id [$id]")
        throw new PathInUseException(s"Failed to register existing path [${proposedPathRecord.path}], already claimed by id [${pr.identifier}], submitting id [$id]")
      }
      case _ => {
        val shortUrlPathRecord = PathRecord(ShortUrlEncoder.generateShortUrlPath(id), id, "short", proposedPathRecord.system)

        existingCanonicalPathForId match {
          case Some(oldCanonical) => {
            if (oldCanonical.path != proposedPathRecord.path) {
              Dynamo.pathsTable.deleteItem("path", oldCanonical.path)
            }
          }
          case None => {
            Dynamo.pathsTable.putItem(shortUrlPathRecord.asDynamoItem)
          }
        }

        Dynamo.pathsTable.putItem(proposedPathRecord.asDynamoItem)

      }
    }
  }
}

class PathInUseException(m: String) extends Exception(m)

