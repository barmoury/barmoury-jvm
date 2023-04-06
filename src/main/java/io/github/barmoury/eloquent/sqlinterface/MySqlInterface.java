package io.github.barmoury.eloquent.sqlinterface;

import io.github.barmoury.eloquent.SqlInterface;

public class MySqlInterface implements SqlInterface {

    @Override
    public String database() {
        return "mysql";
    }

}
