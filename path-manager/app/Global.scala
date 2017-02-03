import play.api.{Application, GlobalSettings}
import services.KinesisLogging

object Global extends GlobalSettings {

  override def beforeStart(app: Application) {
    KinesisLogging.init()
  }
}
