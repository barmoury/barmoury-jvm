package io.github.barmoury.api.model.modelling;

import io.github.barmoury.api.model.Model;
import io.github.barmoury.eloquent.StatQuery;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.GenericGenerator;

@Data
@EqualsAndHashCode(callSuper = true)
public class IdModel<T> extends Model {

    @Id
    @StatQuery.PercentageChangeQuery
    @GeneratedValue(generator = "barmoury_id")
    @GenericGenerator(name = "barmoury_id", strategy = "io.github.barmoury.api.generator.BarmouryIdGenerator")
    T id;

}
