package recipestore.metrics;

import com.codahale.metrics.Meter;
import lombok.Getter;
import lombok.Setter;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import javax.inject.Inject;

public class MeterDecorator implements MethodInterceptor {

    @Getter
    @Setter
    @Inject
    private MetricsFactory metricsFactory;

    public MeterDecorator() {
    }

    public Object invoke(MethodInvocation invocation) throws Throwable {

        final Meter meter = metricsFactory.initializeOrGetMeter(invocation.getMethod().getName());
        meter.mark();
        return invocation.proceed();
    }
}