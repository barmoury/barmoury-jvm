package io.github.barmoury.eloquent;

import io.github.barmoury.eloquent.sqlinterface.MySqlInterface;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class QueryArmouryTest {

    @Autowired
    @Qualifier("mySqlEloquentInterface")
    QueryArmoury queryArmoury;

    @Test
    void initializeQueryArmoury() {
        QueryArmoury queryArmoury = new QueryArmoury(new MySqlInterface());
        System.out.println(queryArmoury.test());
    }

}
