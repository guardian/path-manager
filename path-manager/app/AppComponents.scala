import controllers.{ManagementController, PathManagerController}
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.routing.Router
import play.filters.HttpFiltersComponents
import router.Routes
import services.Metrics

class AppComponents(context: Context) extends BuiltInComponentsFromContext(context) with HttpFiltersComponents {

  lazy val metrics = new Metrics(actorSystem)

  lazy val router: Router = new Routes(
    httpErrorHandler,
    new PathManagerController(controllerComponents, metrics),
    new ManagementController(controllerComponents)
  )

}
