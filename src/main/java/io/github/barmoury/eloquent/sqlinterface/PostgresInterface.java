package io.github.barmoury.eloquent.sqlinterface;

import io.github.barmoury.eloquent.SqlInterface;

public class PostgresInterface extends SqlInterface {

    @Override
    public String database() {
        return "postgres";
    }

}
