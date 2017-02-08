package recipestore.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class UncheckedIterator<E> implements Iterator<E> {

    private final Iterator<E> originalIterator;

    public static final Logger LOGGER = LoggerFactory.getLogger(UncheckedIterator.class);

    public static final <E> Iterator<E> wrap(Iterator<E> original) {
        return new UncheckedIterator<E>(original);
    }

    private UncheckedIterator(Iterator<E> originalIterator) {
        this.originalIterator = originalIterator;
    }

    @Override
    public boolean hasNext() {
        try {
            return originalIterator.hasNext();
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public E next() {
        try {
            return originalIterator.next();
        } catch (Exception e) {
            LOGGER.warn("Exception thrown in the original iterator, will  return null ", e);
            return null;
        }
    }
}
