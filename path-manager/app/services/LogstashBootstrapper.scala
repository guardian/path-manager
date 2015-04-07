package services

import play.api.{Logger, LoggerLike}
import play.api.Play.current
import ch.qos.logback.classic.{Logger => LogbackLogger, LoggerContext}
import net.logstash.logback.encoder.LogstashEncoder
import net.logstash.logback.appender.LogstashTcpSocketAppender

object LogstashBootstrapper extends AwsInstanceTags {

  val config = play.api.Play.configuration

  def bootstrap {
    Logger.debug("bootstrapping logstash appender if configured correctly")
    for (
      stack <- readTag("Stack");
      app <- readTag("App");
      stage <- readTag("Stage");
      l <- asLogBack(Logger);
      logstashHost <- config.getString("logging.logstash.host");
      logstashPort <- config.getInt("logging.logstash.port")
    ) {
      Logger.debug(s"bootstrapping logstash appender with $stack -> $app -> $stage")
      val context = l.getLoggerContext

      val encoder = new LogstashEncoder()
      encoder.setContext(context)
      encoder.setCustomFields(s"""{"stack":"$stack","app":"$app","stage":"$stage"}""")
      encoder.start()

      val appender = new LogstashTcpSocketAppender()
      appender.setContext(context)
      appender.setEncoder(encoder)
      appender.setRemoteHost(logstashHost)
      appender.setPort(logstashPort)
      appender.start()


      l.addAppender(appender)
    }
  }

  def asLogBack(l: LoggerLike): Option[LogbackLogger] = l.logger match {
    case l: LogbackLogger => Some(l)
    case _ => None
  }

}