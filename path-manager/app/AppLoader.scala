import controllers._
import play.api._
import play.api.ApplicationLoader.Context
import play.api.routing.Router
import play.filters.HttpFiltersComponents
import router.Routes
import services.{KinesisLogging, Metrics}

class AppLoader extends ApplicationLoader {
  def load(context: Context) = {

    new KinesisLogging(context.initialConfiguration)

    new AppComponents(context).application

  }
}

class AppComponents(context: Context) extends BuiltInComponentsFromContext(context) with HttpFiltersComponents {

  lazy val metrics = new Metrics(actorSystem)

  lazy val router: Router = new Routes(
    httpErrorHandler,
    new PathManagerController(controllerComponents, metrics),
    new ManagementController(controllerComponents)
  )

}