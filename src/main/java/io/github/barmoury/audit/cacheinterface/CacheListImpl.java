package io.github.barmoury.audit.cacheinterface;

import io.github.barmoury.audit.Audit;
import io.github.barmoury.audit.Auditor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CacheListImpl<T>  implements Auditor.Cache<T> {

    List<Audit<T>> cachedAudits = new ArrayList<>();

    @Override
    public void cache(Audit<T> audit) {
        cachedAudits.add(audit);
    }

    @Override
    public List<Audit<T>> getCachedAudits() {
        List<Audit<T>> resultCachedAudits = new ArrayList<>(cachedAudits);
        cachedAudits.clear();
        return resultCachedAudits;
    }

}
