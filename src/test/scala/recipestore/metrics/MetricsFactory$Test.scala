package recipestore.metrics


import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers}


class MetricsFactory$Test extends FunSuite with Matchers {


  test("Should create meter") {
    MetricsFactory.reporterStatus.get() should be(false)
    val metrics: MetricsWrapper = MetricsFactory.get("TestMeter", classOf[MeterWrapper])
    MetricsFactory.reporterStatus.get() should be(true)
    metrics.status should be equals (0)
    metrics.poke
    metrics.status should be equals (1)
    MetricsFactory.remove("TestMeter", classOf[MeterWrapper])
    MetricsFactory.reporterStatus.get() should be(false)
  }

  test("Should create counter") {
    MetricsFactory.reporterStatus.get() should be(false)
    val metrics: MetricsWrapper = MetricsFactory.get("TestCounter", classOf[CounterWrapper])
    MetricsFactory.reporterStatus.get() should be(true)
    metrics.status should be equals (0)
    metrics.poke
    metrics.status should be equals (1)
    MetricsFactory.remove("TestCounter", classOf[CounterWrapper])
    MetricsFactory.reporterStatus.get() should be(false)
  }


}
