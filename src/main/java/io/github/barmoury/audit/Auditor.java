package io.github.barmoury.audit;

import io.github.barmoury.copier.Copier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public abstract class Auditor<T2> {

    long bufferSize = 0;
    public abstract void flush();
    public abstract Cache<T2> getCache();

    public void audit(Audit<T2> audit) {
        Cache<T2> cache = getCache();
        cache.cache(audit);
        bufferSize++;
        if (bufferSize >= cache.maxBufferSize()) {
            bufferSize = 0;
            new Thread(this::flush).start();
        }
    }

    public interface Cache<T> {

        Object getCachedAudits();
        void cache(Audit<T> audit);
        default long maxBufferSize() {
            return 50;
        }

    }

}
