package recipestore.metrics;

import com.codahale.metrics.Timer;
import lombok.Getter;
import lombok.Setter;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import javax.inject.Inject;

public class TimerDecorator implements MethodInterceptor {

    @Getter
    @Setter
    @Inject
    private MetricsFactory metricsFactory;

    public TimerDecorator() {
    }

    public Object invoke(MethodInvocation invocation) throws Throwable {

        final Timer timer = metricsFactory.initializeOrGetTimer(invocation.getMethod().getName());
        final Timer.Context time = timer.time();
        final Object proceed = invocation.proceed();
        time.stop();
        return proceed;
    }
}