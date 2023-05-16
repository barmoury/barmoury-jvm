package io.github.barmoury.cache.cacheinterface;

import io.github.barmoury.cache.Cache;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ListCacheImpl<T> implements Cache<T> {

    List<T> cached = new ArrayList<>();

    @Override
    public void cache(T data) {
        cached.add(data);
    }

    @Override
    public List<T> getCached() {
        List<T> resultCached = new ArrayList<>(cached);
        cached.clear();
        return resultCached;
    }

}
