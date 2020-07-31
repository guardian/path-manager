package controllers

import play.api.mvc.{BaseController, ControllerComponents}

class ManagementController(override val controllerComponents: ControllerComponents) extends BaseController {

  def healthCheck = Action {
    Ok("OK")
  }

}
