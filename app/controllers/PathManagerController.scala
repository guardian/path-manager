package controllers

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import services.{IdentifierSequence, PathStore}


object PathManagerController extends Controller {

  def registerNewPath = Action { request =>
    val submission = request.body.asFormUrlEncoded.get
    val path = submission("path").head
    val system = submission("system").head

    PathStore.registerNew(path, system) match {
      case Left(error) => BadRequest(error)
      case Right(records) => {
        val cannonicalJson = records.find(_.`type` == "canonical").map(_.asJson)
        val shortJson = records.find(_.`type` == "short").map(_.asJson)
        Ok(Json.obj("canonical" -> cannonicalJson, "short" -> shortJson))
      }
    }
  }

  def registerPath = Action { request =>
    val submission = request.body.asFormUrlEncoded.get
    val path = submission("path").head
    val id = submission("identifier").head.toLong
    val system = submission("system").head


    PathStore.register(path, id, system) match {
      case Left(error) => BadRequest(error)
      case Right(records) => {
        val cannonicalJson = records.find(_.`type` == "canonical").map(_.asJson)
        val shortJson = records.find(_.`type` == "short").map(_.asJson)
        Ok(Json.obj("canonical" -> cannonicalJson, "short" -> shortJson))
      }
    }
  }

  def updateCanonicalPath = Action { request =>

    val submission = request.body.asFormUrlEncoded.get
    val newPath = submission("newPath").head
    val existingPath = submission("existingPath").head
    val id = submission("identifier").head.toLong


    PathStore.updateCanonical(newPath, existingPath, id) match {
      case Left(error) => BadRequest(error)
      case Right(record) => Ok(Json.obj("canonical" -> record.asJson))
    }
  }

  def getPathDetails(path: String) = Action {
    val pathDetails = PathStore.getPathDetails(path)
    pathDetails map{ p => Ok(p.asJson) } getOrElse( NotFound )
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

    Redirect("/debug")
  }

  def showDebug = Action {
    val currentId = IdentifierSequence.getCurrentId
    Ok(s"current id = $currentId")
  }

}
