package io.github.barmoury.api.exception;

import lombok.Getter;

public class SubModelResolveException extends RuntimeException {

    @Getter
    String entity = "entity";

    public SubModelResolveException(Exception e) {
        super(e);
    }

    public SubModelResolveException(String entity, Exception e) {
        super(e);
        this.entity = entity;
    }

}
