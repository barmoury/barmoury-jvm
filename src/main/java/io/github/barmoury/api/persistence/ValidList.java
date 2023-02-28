package io.github.barmoury.api.persistence;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public abstract class ValidList<E> {

    List<E> list;

}
