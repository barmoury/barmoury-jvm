package io.github.barmoury.cache;

public interface Cache<T> {

    Object getCached();
    void cache(T data);

    default long maxBufferSize() {
        return 150;
    }

    default long intervalBeforeFlush() {
        return 20;
    }

}
