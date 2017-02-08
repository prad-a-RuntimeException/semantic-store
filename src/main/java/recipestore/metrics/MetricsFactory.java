package recipestore.metrics;

import com.codahale.metrics.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

/**
 * TODO: Make the handling of metric artifact generic, using suppliers
 */
public class MetricsFactory {

    private static MetricsFactory metricFactory = null;
    public static final Logger LOGGER = LoggerFactory.getLogger(MetricsFactory.class);

    public static MetricsFactory getMetricFactory() {
        if (metricFactory == null) {
            metricFactory = new MetricsFactory();
        }
        return metricFactory;
    }

    final Map<String, Meter> meters = new WeakHashMap<>();
    final Map<String, Counter> counters = new WeakHashMap<>();
    final Map<String, Timer> timers = new WeakHashMap<>();

    private final MetricRegistry metricRegistry = new MetricRegistry();
    private final ConsoleReporter reporter;

    @Inject
    public MetricsFactory() {
        reporter = ConsoleReporter.forRegistry(metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(1, TimeUnit.SECONDS);
    }

    public Meter initializeMeter(final String meterName) {
        if (!meters.containsKey(meterName)) {
            meters.put(meterName, metricRegistry.meter(meterName));
        }
        return meters.get(meterName);
    }

    public void stopMeter(final String meterName) {
        Meter meter = meters.get(meterName);
        LOGGER.info("Closing meter {} with count {}", meterName, meter.getCount());
        meters.remove(meterName);
    }

    public Counter initializeCounter(final String counterName) {
        if (!meters.containsKey(counterName)) {
            counters.put(counterName, metricRegistry.counter(counterName));
        }
        return counters.get(counterName);
    }

    public void stopCounter(final String counterName) {
        Counter counter = counters.get(counterName);
        counters.remove(counterName);
    }


    public void stop() {
        reporter.stop();
    }

}
