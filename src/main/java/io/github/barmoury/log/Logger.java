package io.github.barmoury.log;

import io.github.barmoury.cache.Cache;
import io.github.barmoury.util.Util;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public abstract class Logger {

    long bufferSize = 0;
    Date dateLastFlushed = new Date();

    @Autowired
    Tracer tracer;

    public abstract void flush();
    public abstract void preLog(Log log);
    public abstract Cache<Log> getCache();

    public void log(Log log) {
        Span span = tracer.currentSpan();
        if (span != null) {
            log.setSpanId(span.context().spanId());
            log.setTraceId(span.context().traceId());
        }
        this.preLog(log);
        bufferSize++;
        if (Util.cacheWriteAlong(bufferSize, dateLastFlushed, getCache(), log)) {
            bufferSize = 0;
            dateLastFlushed = new Date();
            new Thread(this::flush).start();
        }
    }

    String formatContent(String format, Object ...args) {
        return String.format(format, args);
    }

    public void verbose(String format, Object ...args) {
        this.log(Log.builder().level(Log.Level.VERBOSE)
                .content(formatContent(format, args)).build());
    }

    public void info(String format, Object ...args) {
        this.log(Log.builder().level(Log.Level.INFO)
                .content(formatContent(format, args)).build());
    }

    public void warn(String format, Object ...args) {
        this.log(Log.builder().level(Log.Level.WARN)
                .content(formatContent(format, args)).build());
    }

    public void error(String format, Object ...args) {
        this.log(Log.builder().level(Log.Level.ERROR)
                .content(formatContent(format, args)).build());
    }

    public void error(Exception exception) {
        String content = exception.getMessage() + "\n" + ExceptionUtils.getStackTrace(exception);
        this.log(Log.builder().level(Log.Level.ERROR).content(content).build());
    }

    public void error(String content, Exception exception) {
        content += "\n" + ExceptionUtils.getStackTrace(exception);
        this.log(Log.builder().level(Log.Level.ERROR).content(content).build());
    }

    public void trace(String format, Object ...args) {
        this.log(Log.builder().level(Log.Level.TRACE)
                .content(formatContent(format, args)).build());
    }

    public void fatal(String format, Object ...args) {
        this.log(Log.builder().level(Log.Level.FATAL)
                .content(formatContent(format, args)).build());
        System.exit(-1199810);
    }

    public void panic(String format, Object ...args) {
        this.log(Log.builder().level(Log.Level.PANIC)
                .content(formatContent(format, args)).build());
        System.exit(-1199811);
    }

}
