package services

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap
import com.amazonaws.services.dynamodbv2.document.{AttributeUpdate, Index, KeyAttribute, RangeKeyCondition}
import com.amazonaws.services.dynamodbv2.model.{ConditionalCheckFailedException, ReturnValue}
import model.PathRecord
import play.api.Logging

import scala.jdk.CollectionConverters._
import scala.util.matching.Regex

object PathValidator extends Logging {
  val validPathRegex = new Regex("^[a-z0-9][a-z0-9-]*(/[a-z0-9][a-z0-9-]*)*$")

  def isValid(path: String): Boolean = {
    val matches = validPathRegex.pattern.matcher(path).matches()
    if (!matches) logger.warn(s"path fails validation [$path]")
    matches
  }
  def isInvalid(path: String):Boolean = !isValid(path)
}

object PathStore extends Logging {

  private val CANONICAL_PATH_TYPE = "canonical"
  private val ALIAS_PATH_TYPE = "alias"

  def registerNew(path: String, system: String) = {

    logger.debug(s"Registering new path [$path]")

    if (PathValidator.isInvalid(path)) {
      Left(s"invalid path [$path]")
    } else {
      val existingPath = Option(Dynamo.pathsTable.getItem("path", path)).map(PathRecord(_))

      existingPath match {
        case Some(pr) => {
          logger.warn(s"Failed to register new path [$path], already claimed by id [${pr.identifier}]")
          Left("path already in use")
        }
        case None => {
          val id = IdentifierSequence.getNextId
          logger.debug(s"generated id [$id] path [$path]")
          val pathRecord = PathRecord(path, id, CANONICAL_PATH_TYPE, system)
          val shortUrlPathRecord = PathRecord(ShortUrlEncoder.generateShortUrlPath(id), id, "short", system)

          putPathItemAndAwaitIndexUpdate(pathRecord)
          logger.debug(s"Adding new short url record for [$id]. short path[${shortUrlPathRecord.path }]")
          Dynamo.pathsTable.putItem(shortUrlPathRecord.asDynamoItem)

          logger.debug(s"Registered path [$path}] for id [$id] successfully")
          Right(List(pathRecord, shortUrlPathRecord).groupBy(_.`type`))
        }
      }
    }
  }

  def registerCanonical(proposedPathRecord: PathRecord) = {

    val id = proposedPathRecord.identifier

    logger.debug(s"Registering path [${proposedPathRecord.path }] for [$id]")

    if (PathValidator.isInvalid(proposedPathRecord.path)) {
      Left(s"invalid path [${proposedPathRecord.path}]")
    } else {
      val existingPath = Option(Dynamo.pathsTable.getItem("path", proposedPathRecord.path)).map(PathRecord(_))
      val index: Index = Dynamo.pathsTable.getIndex("id-index")

      //noinspection Duplicates
      val canonicalPathsForId = index.query(new KeyAttribute("identifier", id), RangeKeyMatches.rangeKeyMatches("type", CANONICAL_PATH_TYPE)).asScala
      val existingCanonicalPathForId = canonicalPathsForId.map{ PathRecord(_) }.headOption

      existingPath match {
        case Some(pr) if (pr.identifier != id) => {
          logger.warn(s"Failed to register existing path [${proposedPathRecord.path}], already claimed by id [${pr.identifier}], submitting id [$id]")
          Left("path already in use")
        }
        case _ => {
          val shortUrlPathRecord = PathRecord(ShortUrlEncoder.generateShortUrlPath(id), id, "short", proposedPathRecord.system)

          existingCanonicalPathForId match {
            case Some(oldCanonical) => {
              if (oldCanonical.path != proposedPathRecord.path) {
                logger.debug(s"Removing old path for item [$id]. old path[${oldCanonical.path}] new path [${proposedPathRecord.path}]")
                Dynamo.pathsTable.deleteItem("path", oldCanonical.path)
              }
            }
            case None => {
              logger.debug(s"Adding new short url record for [$id]. short path[${shortUrlPathRecord.path}]")
              Dynamo.pathsTable.putItem(shortUrlPathRecord.asDynamoItem)
            }
          }

          putPathItemAndAwaitIndexUpdate(proposedPathRecord)

          logger.debug(s"Registered path [${proposedPathRecord.path}] for id [$id] successfully")
          Right(List(proposedPathRecord, shortUrlPathRecord).groupBy(_.`type`))
        }
      }
    }
  }

  def registerAlias(proposedAliasPathRecord: PathRecord) = {

    val id = proposedAliasPathRecord.identifier

    logger.debug(s"Registering path [${proposedAliasPathRecord.path }] for [$id]")

    if (PathValidator.isInvalid(proposedAliasPathRecord.path)) {
      Left(s"invalid path [${proposedAliasPathRecord.path}]")
    } else {
      val existingPath = Option(Dynamo.pathsTable.getItem("path", proposedAliasPathRecord.path)).map(PathRecord(_))

      existingPath match {
        case Some(pr) if (pr.identifier != id) => {
          logger.warn(s"Failed to register existing path [${proposedAliasPathRecord.path}], already claimed by id [${pr.identifier}], submitting id [$id]")
          Left("path already in use")
        }
        case _ => {
          putPathItemAndAwaitIndexUpdate(proposedAliasPathRecord)

          logger.debug(s"Registered new $ALIAS_PATH_TYPE path [${proposedAliasPathRecord.path}] for id [$id] successfully")
          Right(List(proposedAliasPathRecord).groupBy(_.`type`))
        }
      }
    }
  }

  def updateCanonicalWithAlias(newPath: String, id: Long): Either[String, Map[String, List[PathRecord]]] = {
    //This takes the canonical path and makes it an alias. And then adds a new canonical path for newPath.
    
    logger.debug(s"Updating $CANONICAL_PATH_TYPE path [$newPath}] for [$id] and creating $ALIAS_PATH_TYPE to old path.")

    if (PathValidator.isInvalid(newPath)) {
      Left(s"invalid path [$newPath]")
    } else {
      val newPathRecord = Option(Dynamo.pathsTable.getItem("path", newPath)).map(PathRecord(_))
      val pathsForId = Dynamo.pathsTable.getIndex("id-index").query(new KeyAttribute("identifier", id)).asScala.map{ PathRecord(_) }
      val canonicalPathForId = pathsForId.find(_.`type`==CANONICAL_PATH_TYPE)

      if(newPathRecord.exists(_.identifier != id)) {
        logger.warn(s"Failed to update path [$newPath], already claimed by id [${newPathRecord.map{_.identifier}.get}], submitting id [$id]")
        Left("path already in use")
      } else {
        canonicalPathForId.map { existingRecord: PathRecord =>

          val existingPath = existingRecord.path
          val existingAliases = pathsForId.filter(_.`type` == ALIAS_PATH_TYPE)

          val updatedRecords = if (existingPath != newPath) {

            val newCanonicalRecord = existingRecord.copy(path = newPath, lastModified = None)

            logger.debug(s"Aliasing old path for item [$id]. old path[$existingPath] new path [$newPath]")
            val resultingAliasRecord = PathRecord(
              Dynamo.pathsTable.updateItem(new UpdateItemSpec()
                .withPrimaryKey("path", existingPath)
                .withAttributeUpdate(
                  new AttributeUpdate("type").put(ALIAS_PATH_TYPE),
                  new AttributeUpdate("lastModified").put(System.currentTimeMillis) // so we can keep the aliases order
                )
                .withReturnValues(ReturnValue.ALL_NEW) // this means we can call getItem below
              ).getItem
            )
            putPathItemAndAwaitIndexUpdate(newCanonicalRecord) //TODO this and the update above should be a transaction (in case the latter fails)
            List(newCanonicalRecord, resultingAliasRecord)
          } else {
            List(existingRecord)
          }
          logger.debug(s"updated $CANONICAL_PATH_TYPE path [$newPath}] and added $ALIAS_PATH_TYPE to [$existingPath] id [$id] successfully")
          (updatedRecords ++ existingAliases).groupBy(_.`type`)
        }.toRight{
          logger.warn(s"Failed to update path [$newPath], no existing path found for id [$id]")
          s"unable to find $CANONICAL_PATH_TYPE record for $id"
        }
      }
    }
  }

  def setAliasPathIsRemovedFlag(path: String, isRemoved: Boolean): Option[Iterable[PathRecord]] = {
    try {
      val updatedRecord = PathRecord(
        Dynamo.pathsTable.updateItem(new UpdateItemSpec()
          .withPrimaryKey("path", path)
          .withConditionExpression(s"#type = :pathType")
          .withUpdateExpression("SET isRemoved = :isRemoved")
          .withNameMap(Map("#type" -> "type").asJava)
          .withValueMap(new ValueMap()
            .withString(":pathType", ALIAS_PATH_TYPE)
            .withBoolean(":isRemoved", isRemoved)
          )
          .withReturnValues(ReturnValue.ALL_NEW) // this means we can call getItem below
        ).getItem
      )

      Some(
        Dynamo.pathsTable.getIndex("id-index")
        .query(new KeyAttribute("identifier", updatedRecord.identifier)).asScala.map{ PathRecord(_) }
        .filter(_.`type` == ALIAS_PATH_TYPE)
      )
    } catch {
      case _: ConditionalCheckFailedException => None
    }

  }


  def updateCanonical(newPath: String, id: Long): Either[String, Map[String, List[PathRecord]]] = {

    logger.debug(s"Updating $CANONICAL_PATH_TYPE path [$newPath}] for [$id]")

    if (PathValidator.isInvalid(newPath)) {
      Left(s"invalid path [$newPath]")
    } else {
      val newPathRecord = Option(Dynamo.pathsTable.getItem("path", newPath)).map(PathRecord(_))
      val canonicalPathsForId = Dynamo.pathsTable.getIndex("id-index").query(new KeyAttribute("identifier", id), RangeKeyMatches.rangeKeyMatches("type", CANONICAL_PATH_TYPE)).asScala
      val canonicalPathForId = canonicalPathsForId.map{ PathRecord(_) }.headOption

      if(newPathRecord.exists(_.identifier != id)) {
        logger.warn(s"Failed to update path [$newPath], already claimed by id [${newPathRecord.map{_.identifier}.get}], submitting id [$id]")
        Left("path already in use")
      } else {
        canonicalPathForId.map { existingRecord: PathRecord =>

          val existingPath = existingRecord.path
          val updatedRecord = if (existingPath != newPath) {
              val newRecord = existingRecord.copy(path = newPath)
              logger.debug(s"Removing old path for item [$id]. old path[$existingPath] new path [$newPath]")
              Dynamo.pathsTable.deleteItem("path", existingPath)
              putPathItemAndAwaitIndexUpdate(newRecord)
              newRecord
            } else {
              existingRecord
            }
          logger.debug(s"updated $CANONICAL_PATH_TYPE path [$newPath}] for id [$id] successfully")
          List(updatedRecord).groupBy(_.`type`)
        }.toRight{
          logger.warn(s"Failed to update path [$newPath], no existing path found for id [$id]")
          s"unable to find $CANONICAL_PATH_TYPE record for $id"
        }
      }
    }
  }

  def deleteRecord(id: Long) = {
    logger.debug(s"deleting path records for id [$id]")
    val pathItems = Dynamo.pathsTable.getIndex("id-index").query(new KeyAttribute("identifier", id)).asScala
    pathItems foreach { item =>
      val path = item.getString("path")
      logger.debug(s"deleting path [$path] for id [$id]")
      Dynamo.pathsTable.deleteItem("path", path)
    }
    logger.debug(s"deleted path records for id [$id] successfully")
  }

  def getPathDetails(path: String) = {
    Option(Dynamo.pathsTable.getItem("path", path)).map(PathRecord(_))
  }

  def getPathsById(id: Long) = {
    val pathItems = Dynamo.pathsTable.getIndex("id-index").query(new KeyAttribute("identifier", id)).asScala
    val paths = pathItems.map{ PathRecord(_) }
    paths.groupBy(_.`type`)
  }

  def putPathItemAndAwaitIndexUpdate(record: PathRecord): Unit = {

    Dynamo.pathsTable.putItem(record.asDynamoItem)

    def waitForIndexUpdate(attempts: Int = 0): Boolean = {

      if (attempts > 20) {
        logger.warn(s"update to path [${record.path}] for id [${record.identifier}] has not propagated to secondary index after 20 checks")
        false
      } else {
        val pathRecordsFromIndex = Dynamo.pathsTable.getIndex("id-index").query(new KeyAttribute("identifier", record.identifier), RangeKeyMatches.rangeKeyMatches("type", record.`type`)).asScala

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
