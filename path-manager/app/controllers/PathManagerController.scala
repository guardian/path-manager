package controllers

import model.PathRecord
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, Controller}
import services.{IdentifierSequence, PathStore}
import services.Metrics._


object PathManagerController extends Controller {

  def registerNewPath = Action { request =>
    val submission = request.body.asFormUrlEncoded.get
    val path = submission("path").head
    val system = submission("system").head

    PathStore.registerNew(path, system) match {
      case Left(error) => {
        PathOperationErrors.increment
        BadRequest(error)
      }
      case Right(records) => {
        PathRegistrations.increment
        argoOk(Json.toJson(records))
      }
    }
  }

  def registerExistingPath(id: Long) = Action { request =>
    request.body.asJson.map(_.as[PathRecord]).map { submission =>
      if (id != submission.identifier) {
        PathOperationErrors.increment
        Logger.warn("registerExistingPath failed, identifier in url and body do not match")
        BadRequest("identifier in url and body do not match")
      } else if (submission.`type` != "canonical") {
        PathOperationErrors.increment
        Logger.warn("registerExistingPath failed, only canonical paths can be updated at present")
        BadRequest("only canonical paths can be updated at present")
      } else {
        PathStore.register(submission) match {
          case Left(error) => BadRequest(error)
          case Right(records) => {
            PathMigrationRegistrations.increment
            argoOk(Json.toJson(records))
          }
        }
      }
    } getOrElse{
      PathOperationErrors.increment
      Logger.warn("registerExistingPath failed, unable to parse PathRecord from request body")
      BadRequest("unable to parse PathRecord from request body")
    }
  }

  def updateCanonicalPath(id: Long) = Action { request =>

    val submission = request.body.asFormUrlEncoded.get
    val newPath = submission("path").head

    PathStore.updateCanonical(newPath, id) match {
      case Left(error) => {
        PathOperationErrors.increment
        BadRequest(error)
      }
      case Right(record) => {
        PathUpdates.increment
        Ok(Json.toJson(record))
      }
    }
  }

  def deleteRecord(id: Long) = Action {
    PathStore.deleteRecord(id)
    PathDeletes.increment
    NoContent
  }

  def getPathDetails(path: String) = Action {
    Logger.debug(s"looking up path $path")
    val pathDetails = PathStore.getPathDetails(path)
    val pathsByType = pathDetails.groupBy(_.`type`)
    PathLookups.increment
    if(pathsByType.isEmpty) {
      Logger.debug(s"path $path not registered")
      NotFound
    } else {
      Logger.debug(s"path $path found")
      argoOk(Json.toJson(pathsByType))
    }
  }

  def getPathsById(id: Long) = Action {
    val paths = PathStore.getPathsById(id)
    PathLookups.increment
    if (paths.isEmpty) {
      NotFound
    } else {
      argoOk(Json.toJson(paths))
    }
  }

  // debug endpoints...

  def showIdSeq = Action {
    val currentId = IdentifierSequence.getCurrentId

    Ok(views.html.Application.updateIdSeq(currentId))
  }

  def updateIdSeq = Action { request =>
    val submission = request.body.asFormUrlEncoded.get
    val newSeqNo = submission("val").map(_.toLong).head

    IdentifierSequence.setCurrentId(newSeqNo)
    val currentId = IdentifierSequence.getCurrentId

    Ok(s"$currentId")
  }

  def showCurrentSequence = Action {
    val currentId = IdentifierSequence.getCurrentId
    Ok(s"$currentId")
  }

  def argoOk(json: JsValue) = Ok(Json.obj("data" -> json)).as("application/vnd.argo+json")

}
