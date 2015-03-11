package controllers

import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import services.{PathStore, KeyService}


object UrlManagerController extends Controller {

  def registerNewPath = Action { request =>
    val submission = request.body.asFormUrlEncoded.get
    val path = submission("path").head
    val system = submission("system").head
    val key = submission("key").head

    if(KeyService.validRequest(system, key)) {

      PathStore.registerNew(path, system) match {
        case Left(error) => BadRequest(error)
        case Right(records) => {
          val cannonicalJson = records.find(_.`type` == "canonical").map(_.asJson)
          val shortJson = records.find(_.`type` == "short").map(_.asJson)
          Ok(Json.obj("canonical" -> cannonicalJson, "short" -> shortJson))
        }
      }
    } else {
      Unauthorized("system and key do not match")
    }
  }

  def registerPath = Action { request =>
    val submission = request.body.asFormUrlEncoded.get
    val path = submission("path").head
    val id = submission("identifier").head.toLong
    val system = submission("system").head
    val key = submission("key").head

    if(KeyService.validRequest(system, key)) {

      PathStore.register(path, id, system) match {
        case Left(error) => BadRequest(error)
        case Right(records) => {
          val cannonicalJson = records.find(_.`type` == "canonical").map(_.asJson)
          val shortJson = records.find(_.`type` == "short").map(_.asJson)
          Ok(Json.obj("canonical" -> cannonicalJson, "short" -> shortJson))
        }
      }
    } else {
      Unauthorized("system and key do not match")
    }
  }

  def updateCanonicalPath = Action { request =>

    val submission = request.body.asFormUrlEncoded.get
    val newPath = submission("newPath").head
    val existingPath = submission("existingPath").head
    val id = submission("identifier").head.toLong
    val system = submission("system").head
    val key = submission("key").head

    if(KeyService.validRequest(system, key)) {

      PathStore.updateCanonical(newPath, existingPath, id) match {
        case Left(error) => BadRequest(error)
        case Right(records) => {
          val cannonicalJson = records.find(_.`type` == "canonical").map(_.asJson)
          Ok(Json.obj("canonical" -> cannonicalJson))
        }
      }
    } else {
      Unauthorized("system and key do not match")
    }
  }

  def getPathDetails(path: String, system: String, key: String) = Action {
    if(KeyService.validRequest(system, key)) {
      val pathDetails = PathStore.getPathDetails(path)
      pathDetails map{ p => Ok(p.asJson) } getOrElse( NotFound )
    } else {
      Unauthorized("system and key do not match")
    }
  }

  def showIdSeq = Action {
    val currentId = PathStore.identifierSeq.get

    Ok(views.html.Application.updateIdSeq(currentId))
  }

  def updateIdSeq = Action { request =>
    val submission = request.body.asFormUrlEncoded.get
    val newSeqNo = submission("val").map(_.toLong).head

    PathStore.identifierSeq.set(newSeqNo)

    Redirect("/debug")
  }

  def showDebug = Action {
    val paths = PathStore.pathRepository.get
    val currentId = PathStore.identifierSeq.get

    val pathsData = paths.sortBy(_.identifier).map{ p => s"${p.path}\t${p.identifier}\t${p.`type`}\t${p.system}\t"}.mkString("\n")

    Ok(s"current id = $currentId \n\n\n$pathsData")

  }

}
