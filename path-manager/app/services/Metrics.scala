package services

import java.util.Date
import java.util.concurrent.atomic.{AtomicLong, AtomicReference}

import org.apache.pekko.actor.{Actor, ActorSystem, Props}
import com.amazonaws.services.cloudwatch.model._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

class Metrics(actorSystem: ActorSystem) {

  val PathRegistrations = new CountMetric("pathRegistrations")
  val PathMigrationRegistrations = new CountMetric("pathMigrationRegistrations")
  val PathUpdates = new CountMetric("pathUpdates")
  val PathDeletes = new CountMetric("pathDeletes")
  val PathLookups = new CountMetric("pathLookups")
  val PathOperationErrors = new CountMetric("pathOperationErrors")

  val all = List(
    PathRegistrations,
    PathMigrationRegistrations,
    PathUpdates,
    PathDeletes,
    PathLookups,
    PathOperationErrors
  )

  private val reporter = new CloudWatchReporter(all, actorSystem)
  reporter.start

}

class StopWatch {
  val startTime = System.currentTimeMillis
  def elapsed = System.currentTimeMillis - startTime
}

trait CloudWatchMetric {
  def flush(dimensions: Dimension*) :Seq[MetricDatum]
}

class TimingMetric(metricName: String) extends CloudWatchMetric {

  private val _times = new AtomicReference[List[Long]](Nil)

  def recordTimeSpent(durationInMillis: Long) = {
    _times.set(durationInMillis :: _times.get())
  }

  def measure[T](block: => T) = {
    val s = new StopWatch
    val result = block
    recordTimeSpent(s.elapsed)
    result
  }

  override def flush(dimensions: Dimension*) = {
    val timeData = _times.getAndSet(Nil)
    if (timeData == Nil) {
      Nil
    } else {
      val stats = new StatisticSet()
        .withMaximum(timeData.max.toDouble)
        .withMinimum(timeData.min.toDouble)
        .withSum(timeData.sum.toDouble)
        .withSampleCount(timeData.size.toDouble)
      Seq(new MetricDatum()
        .withMetricName(metricName)
        .withStatisticValues(stats)
        .withUnit(StandardUnit.Milliseconds)
        .withTimestamp(new Date())
        .withDimensions(dimensions: _*))
    }
  }
}

class CountMetric(metricName: String) extends CloudWatchMetric {
  val _count = new AtomicLong()

  def recordCount(c: Long) = { _count.addAndGet(c) }
  def increment = { _count.incrementAndGet }

  override def flush(dimensions: Dimension*) = Seq(
    new MetricDatum()
      .withMetricName(metricName)
      .withDimensions(dimensions: _*)
      .withValue(_count.getAndSet(0).toDouble)
      .withUnit(StandardUnit.Count)
      .withTimestamp(new Date())
  )
}

class CloudWatchReporter(metrics: Seq[CloudWatchMetric], actorSystem: ActorSystem) extends AwsInstanceTags {

  lazy val stageOpt = readTag("Stage")
  lazy val appOpt = readTag("App")

  def start = {

    for (
      app <- appOpt;
      stage <- stageOpt
    ) {
      actorSystem.scheduler.scheduleOnce(
        delay = 1 minute,
        receiver = actorSystem.actorOf(Props(new CloudWatchReportActor(app, stage))),
        message = ReportMetrics
      )
    }
  }

  case object ReportMetrics

  class CloudWatchReportActor(app: String, stage: String) extends Actor {

    val appDimension = new Dimension().withName("App").withValue(app)
    val stageDimension = new Dimension().withName("Stage").withValue(stage)

    override def receive = {
      case ReportMetrics => {

        val data = metrics.flatMap(_.flush(appDimension, stageDimension))

        val metricData = new PutMetricDataRequest().withNamespace("AppMetrics").withMetricData(data: _*)
        AWS.CloudWatch.putMetricDataAsync(metricData)
        reschedule()
      }
    }

    private def reschedule() = {
      context.system.scheduler.scheduleOnce(1 minute, self, ReportMetrics)
    }

    override def postRestart(reason: Throwable) = { reschedule() }
  }
}
