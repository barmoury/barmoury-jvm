package io.github.barmoury.cache;

import io.github.barmoury.audit.Audit;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.function.Function;

public interface Cache<T> {

    Object getCached();
    void cache(T data);

    default long maxBufferSize() {
        return 50;
    }

    default long intervalBeforeFlush() {
        return 20;
    }

}
