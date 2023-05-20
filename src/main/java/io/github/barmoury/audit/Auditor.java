package io.github.barmoury.audit;

import io.github.barmoury.cache.Cache;
import io.github.barmoury.util.Util;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public abstract class Auditor<T> {

    long bufferSize = 0;
    Date dateLastFlushed = new Date();

    public abstract void flush();
    public abstract Cache<Audit<T>> getCache();
    public abstract void preAudit(Audit<T> audit);

    public void audit(Audit<T> audit) {
        this.preAudit(audit);
        bufferSize++;
        if (Util.cacheWriteAlong(bufferSize, dateLastFlushed, getCache(), audit)) {
            bufferSize = 0;
            dateLastFlushed = new Date();
            new Thread(this::flush).start();
        }
    }

}
