package io.github.barmoury.api.model;

import io.github.barmoury.api.Timeo;
import io.github.barmoury.eloquent.RequestParamFilter;
import io.github.barmoury.eloquent.StatQuery;
import io.github.barmoury.copier.Copier;
import io.github.barmoury.eloquent.QueryArmoury;
import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

import io.github.barmoury.copier.CopyProperty;

@Data
@MappedSuperclass
public class Model {

    @StatQuery.PercentageChangeQuery
    @GeneratedValue(strategy = GenerationType.IDENTITY) @Id
    long id;

    @CopyProperty(ignore = true) @Temporal(TemporalType.TIMESTAMP)
    @RequestParamFilter(operator = RequestParamFilter.Operator.RANGE)
    Date createdAt = new Date();

    @CopyProperty(ignore = true) @Temporal(TemporalType.TIMESTAMP)
    @RequestParamFilter(operator = RequestParamFilter.Operator.RANGE)
    Date updatedAt = new Date();

    public Model resolve(Request baseRequest) {
        Copier.copy(this, baseRequest);
        Timeo.resolve(this);
        return this;
    }

    public Model resolve(Request baseRequest,
                         QueryArmoury queryArmoury,
                         UserDetails<?> userDetails) {
        return resolve(baseRequest);
    }

    public static class Request {
        public long updateEntityId;
    }

}
