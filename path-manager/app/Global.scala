import play.api.{Application, GlobalSettings}
import services.LogstashBootstrapper

object Global extends GlobalSettings {

  override def beforeStart(app: Application) {
    LogstashBootstrapper.bootstrap
  }
}
