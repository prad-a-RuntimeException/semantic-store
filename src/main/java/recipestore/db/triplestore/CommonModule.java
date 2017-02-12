package recipestore.db.triplestore;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;
import recipestore.metrics.AddMeter;
import recipestore.metrics.MeterDecorator;

public class CommonModule extends AbstractModule {

    @Override
    protected void configure() {
        final MeterDecorator meterDecorator = new MeterDecorator();
        requestInjection(meterDecorator);
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(AddMeter.class),
                meterDecorator);
    }
}
