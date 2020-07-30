import play.api.ApplicationLoader.Context
import play.api._
import services.KinesisLogging

class AppLoader extends ApplicationLoader {
  def load(context: Context) = {

    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }

    new KinesisLogging(context.initialConfiguration)

    new AppComponents(context).application

  }
}

