package io.github.barmoury.audit;

import io.github.barmoury.cache.Cache;
import io.github.barmoury.copier.Copier;
import io.github.barmoury.util.Util;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public abstract class Auditor<T> {

    long bufferSize = 0;
    Date dateLastFlushed = new Date();

    public abstract void flush();
    public abstract Cache<Audit<T>> getCache();

    public void audit(Audit<T> audit) {
        if (Util.cacheWriteAlong(bufferSize, dateLastFlushed, getCache(), audit)) {
            bufferSize = 0;
            dateLastFlushed = new Date();
            new Thread(this::flush).start();
        }
    }

}
