package io.github.barmoury.util;

import io.github.barmoury.cache.Cache;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
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

    public static <T> T[] addToArray(T[] entries, T entry) {
        T[] old = entries;
        entries = Arrays.copyOf(entries, entries.length + 1);
        System.arraycopy(old, 0, entries, 0, old.length);
        entries[old.length] = entry;
        return entries;
    }

}
