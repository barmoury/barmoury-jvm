package io.github.barmoury.eloquent;

public abstract class SqlInterface {

    public abstract String database();

    public String limit(long l) {
        return String.format("LIMIT %d", l);
    }

    public String offset(long l) {
        return String.format("OFFSET %d", l);
    }

}
