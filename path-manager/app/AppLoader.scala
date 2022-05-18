import play.api.ApplicationLoader.Context
import play.api._

class AppLoader extends ApplicationLoader {
  def load(context: Context) = {

    //TODO do we still need this?
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }

    new AppComponents(context).application

  }
}

