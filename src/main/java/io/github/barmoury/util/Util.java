package io.github.barmoury.util;

import io.github.barmoury.cache.Cache;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.function.Function;

public class Util {

    public static <T> boolean cacheWriteAlong(long bufferSize, Date dateLastFlushed,
                             Cache<T> cache, T entry) {
        cache.cache(entry);
        bufferSize++;
        long diff = ChronoUnit.MINUTES.between(dateLastFlushed.toInstant(), new Date().toInstant());
        return bufferSize >= cache.maxBufferSize() || diff >= cache.intervalBeforeFlush();
    }

}
