package recipestore.metrics

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

import com.codahale.metrics.{ConsoleReporter, MetricRegistry}
import com.twitter.storehaus.cache.MapCache.empty
import com.twitter.storehaus.cache.{Memoize, MutableCache}
import org.slf4j.{Logger, LoggerFactory}

sealed trait MetricsWrapper {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def getCount: Long

  def poke = {
    this match {
      case _: TimerWrapper => this.asInstanceOf[TimerWrapper].timer.time()
      case _: MeterWrapper => this.asInstanceOf[MeterWrapper].meter.mark()
      case _: CounterWrapper => this.asInstanceOf[CounterWrapper].counter.inc()
    }
  }

  def status: Long = {
    this.getCount
  }


  def close = {
    this match {
      case _: TimerWrapper => this.asInstanceOf[TimerWrapper].timer.time().stop()
      case _ => logger.warn(s"Close method not applicable for ${this.getClass}")
    }
  }
}

final case class TimerWrapper(timer: com.codahale.metrics.Timer) extends MetricsWrapper {
  override def getCount: Long = timer.getCount
}

final case class MeterWrapper(meter: com.codahale.metrics.Meter) extends MetricsWrapper {
  override def getCount: Long = meter.getCount
}

final case class CounterWrapper(counter: com.codahale.metrics.Counter) extends MetricsWrapper {
  override def getCount: Long = counter.getCount
}

object MetricsFactory {

  private val metricsCache: MutableCache[(String, Class[_]), MetricsWrapper] =
    empty[(String, Class[_]), MetricsWrapper].toMutable()

  val get = Memoize(metricsCache)(_get)

  val reporterStatus: AtomicBoolean = new AtomicBoolean(false)

  val metricRegistry = new MetricRegistry

  var reporter: ConsoleReporter = null


  private def createReporter: ConsoleReporter = {
    ConsoleReporter.forRegistry(metricRegistry).
      convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).build
  }

  def initReporter(duration: Int = 10): Unit = {
    reporter = createReporter
  }

  def stopReporter() = {
    if (reporter != null) {
      reporter.stop()
      reporter = null
    }
  }

  def _get[T <: MetricsWrapper](name: String, c: Class[_]): MetricsWrapper = {
    val metrics: MetricsWrapper = if (classOf[MeterWrapper] isAssignableFrom c) new MeterWrapper(metricRegistry.meter(name))
    else if (classOf[CounterWrapper] isAssignableFrom c) new CounterWrapper(metricRegistry.counter(name))
    else if (classOf[TimerWrapper] isAssignableFrom c) new TimerWrapper(metricRegistry.timer(name))
    else throw new IllegalArgumentException("Cannot initiate metrics for class " + c)
    if (!reporterStatus.get()) {
      initReporter()
    }
    metrics
  }

  def remove[T <: MetricsWrapper](name: String, c: Class[_]): Unit = {
    metricsCache.evict(name, c)
    if (!metricsCache.iterator.hasNext) {
      stopReporter()
    }
  }


}
