package recipestore.metrics

import org.scalatest.{FunSuite, Matchers}

class MetricsWrapperFactory$Test extends FunSuite with Matchers {


  test("Should create meter") {
    MetricsFactory.reporterStatus.get() should be equals (false)
    val metrics: MetricsWrapper = MetricsFactory.get("TestMeter", classOf[MeterWrapper])
    MetricsFactory.reporterStatus.get() should be equals (true)
    metrics.status should be equals (0)
    metrics.poke
    metrics.status should be equals (1)
    MetricsFactory.remove("TestMeter", classOf[MeterWrapper])
    MetricsFactory.reporterStatus.get() should be equals (false)
  }

  test("Should create counter") {
    MetricsFactory.reporterStatus.get() should be equals (false)
    val metrics: MetricsWrapper = MetricsFactory.get("TestCounter", classOf[CounterWrapper])
    MetricsFactory.reporterStatus.get() should be equals (true)
    metrics.status should be equals (0)
    metrics.poke
    metrics.status should be equals (1)
        MetricsFactory.remove("TestCounter", classOf[MeterWrapper])
    MetricsFactory.reporterStatus.get() should be equals (false)
  }


}
