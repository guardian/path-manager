package services

import model.PathRecord
import play.api.Logger
import scala.collection.JavaConversions._
import com.amazonaws.services.dynamodbv2.document.{AttributeUpdate, KeyAttribute, RangeKeyCondition, Item}

object PathStore {

  def registerNew(path: String, system: String) = {

    Logger.debug(s"Registering new path [$path]")

    val existingPath = Option(Dynamo.pathsTable.getItem("path", path)).map(PathRecord(_))

    existingPath match {
      case Some(pr) => {
        Logger.warn(s"Failed to register new path [$path], already claimed by id [${pr.identifier}]")
        Left("path already in use")
      }
      case None => {
        val id = IdentifierSequence.getNextId
        Logger.debug(s"generated id [$id] path [$path]")
        val pathRecord = PathRecord(path, id, "canonical", system)
        val shortUrlPathRecord = PathRecord(ShortUrlEncoder.generateShortUrlPath(id), id, "short", system)

        putPathItemAndAwaitIndexUpdate(pathRecord)
        Logger.debug(s"Adding new short url record for [$id]. short path[${shortUrlPathRecord.path }]")
        Dynamo.pathsTable.putItem(shortUrlPathRecord.asDynamoItem)

        Logger.debug(s"Registered path [$path}] for id [$id] successfully")
        Right(List(pathRecord, shortUrlPathRecord).groupBy(_.`type`))
      }
    }
  }

  def register(proposedPathRecord: PathRecord) = {

    val id = proposedPathRecord.identifier

    Logger.debug(s"Registering path [${proposedPathRecord.path }] for [$id]")

    val existingPath = Option(Dynamo.pathsTable.getItem("path", proposedPathRecord.path)).map(PathRecord(_))
    val canonicalPathsForId = Dynamo.pathsTable.getIndex("id-index").query(new KeyAttribute("identifier", id), new RangeKeyCondition("type").eq("canonical"))
    val existingCanonicalPathForId = canonicalPathsForId.map{ PathRecord(_) }.headOption

    existingPath match {
      case Some(pr) if (pr.identifier != id) => {
        Logger.warn(s"Failed to register existing path [${proposedPathRecord.path}], already claimed by id [${pr.identifier}], submitting id [$id]")
        Left("path already in use")
      }
      case _ => {
        val shortUrlPathRecord = PathRecord(ShortUrlEncoder.generateShortUrlPath(id), id, "short", proposedPathRecord.system)

        existingCanonicalPathForId match {
          case Some(oldCanonical) => {
            if (oldCanonical.path != proposedPathRecord.path) {
              Logger.debug(s"Removing old path for item [$id]. old path[${oldCanonical.path}] new path [${proposedPathRecord.path}]")
              Dynamo.pathsTable.deleteItem("path", oldCanonical.path)
            }
          }
          case None => {
            Logger.debug(s"Adding new short url record for [$id]. short path[${shortUrlPathRecord.path}]")
            Dynamo.pathsTable.putItem(shortUrlPathRecord.asDynamoItem)
          }
        }

        putPathItemAndAwaitIndexUpdate(proposedPathRecord)

        Logger.debug(s"Registered path [${proposedPathRecord.path}] for id [$id] successfully")
        Right(List(proposedPathRecord, shortUrlPathRecord).groupBy(_.`type`))
      }
    }
  }

  def updateCanonical(newPath: String, id: Long) = {

    Logger.debug(s"Updating canonical path [$newPath}] for [$id]")
    val newPathRecord = Option(Dynamo.pathsTable.getItem("path", newPath)).map(PathRecord(_))
    val canonicalPathsForId = Dynamo.pathsTable.getIndex("id-index").query(new KeyAttribute("identifier", id), new RangeKeyCondition("type").eq("canonical"))
    val canonicalPathForId = canonicalPathsForId.map{ PathRecord(_) }.headOption

    if(newPathRecord.exists(_.identifier != id)) {
      Logger.warn(s"Failed to update path [$newPath], already claimed by id [${newPathRecord.map{_.identifier}}], submitting id [$id]")
      Left("path already in use")
    } else {
      canonicalPathForId.map { existingRecord: PathRecord =>

        val existingPath = existingRecord.path
        val updatedRecord = if (existingPath != newPath) {
            val newRecord = existingRecord.copy(path = newPath)
            Logger.debug(s"Removing old path for item [$id]. old path[$existingPath] new path [$newPath]")
            Dynamo.pathsTable.deleteItem("path", existingPath)
            putPathItemAndAwaitIndexUpdate(newRecord)
            newRecord
          } else {
            existingRecord
          }
        Logger.debug(s"updated canonical path [$newPath}] for id [$id] successfully")
        List(updatedRecord).groupBy(_.`type`)
      }.toRight{
        Logger.warn(s"Failed to update path [$newPath], no existing path found for id [$id]")
        s"unable to find canonical record for $id"
      }
    }
  }

  def deleteRecord(id: Long) = {
    Logger.debug(s"deleting path records for id [$id]")
    val pathItems = Dynamo.pathsTable.getIndex("id-index").query(new KeyAttribute("identifier", id))
    pathItems foreach { item =>
      val path = item.getString("path")
      Logger.debug(s"deleting path [$path] for id [$id]")
      Dynamo.pathsTable.deleteItem("path", path)
    }
    Logger.debug(s"deleted path records for id [$id] successfully")
  }

  def getPathDetails(path: String) = {
    Option(Dynamo.pathsTable.getItem("path", path)).map(PathRecord(_))
  }

  def getPathsById(id: Long) = {
    val pathItems = Dynamo.pathsTable.getIndex("id-index").query(new KeyAttribute("identifier", id))
    val paths = pathItems.map{ PathRecord(_) }
    paths.groupBy(_.`type`)
  }

  def putPathItemAndAwaitIndexUpdate(record: PathRecord) {

    Dynamo.pathsTable.putItem(record.asDynamoItem)

    def waitForIndexUpdate(attempts: Int = 0): Boolean = {

      if (attempts > 20) {
        false
      } else {
        val pathRecordsFromIndex = Dynamo.pathsTable.getIndex("id-index").query(new KeyAttribute("identifier", record.identifier), new RangeKeyCondition("type").eq(record.`type`))

        val pathRecordFromIndex = pathRecordsFromIndex.map {
          PathRecord(_)
        }.headOption

        pathRecordFromIndex match {
          case Some(i) if (i.path == record.path) => true
          case _ => {
            Thread.sleep(50)
            waitForIndexUpdate(attempts + 1)
          }
        }
      }
    }



  }
}