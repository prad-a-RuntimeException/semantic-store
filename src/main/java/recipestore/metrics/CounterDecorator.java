package recipestore.metrics;

import com.codahale.metrics.Counter;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class CounterDecorator implements MethodInterceptor {

    private final MetricsFactory metricsFactory;

    public CounterDecorator(final MetricsFactory metricsFactory) {
        this.metricsFactory = metricsFactory;
    }

    public Object invoke(MethodInvocation invocation) throws Throwable {

        final Counter counter = metricsFactory.initializeOrGetCounter(invocation.getMethod().getName());
        counter.inc();
        return invocation.proceed();
    }
}