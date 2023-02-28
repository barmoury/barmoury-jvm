package io.github.barmoury.api.model;

import io.github.barmoury.api.Timeo;
import io.github.barmoury.api.persistence.RequestParamFilter;
import io.github.barmoury.copier.Copier;
import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

import io.github.barmoury.copier.CopyProperty;

@Data
@MappedSuperclass
public class BarmouryModel {

    @GeneratedValue(strategy = GenerationType.IDENTITY) @Id
    long id;

    @RequestParamFilter(operator = RequestParamFilter.Operator.BETWEEN)
    @CopyProperty(ignore = true) @Temporal(TemporalType.TIMESTAMP)
    Date createdAt = new Date();

    @RequestParamFilter(operator = RequestParamFilter.Operator.BETWEEN)
    @CopyProperty(ignore = true) @Temporal(TemporalType.TIMESTAMP)
    Date updatedAt = new Date();

    public BarmouryModel resolve(Request baseRequest) {
        Copier.copy(this, baseRequest);
        Timeo.resolve(this);
        return this;
    }

    public interface Request {

    }

}
