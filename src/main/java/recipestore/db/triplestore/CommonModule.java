package recipestore.db.triplestore;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;
import recipestore.metrics.AddMeter;
import recipestore.metrics.AddTimer;
import recipestore.metrics.MeterDecorator;
import recipestore.metrics.TimerDecorator;

public class CommonModule extends AbstractModule {

    @Override
    protected void configure() {
        final MeterDecorator meterDecorator = new MeterDecorator();
        requestInjection(meterDecorator);
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(AddMeter.class),
                meterDecorator);
        final TimerDecorator timerDecorator = new TimerDecorator();
        requestInjection(timerDecorator);
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(AddTimer.class),
                timerDecorator);
    }
}
