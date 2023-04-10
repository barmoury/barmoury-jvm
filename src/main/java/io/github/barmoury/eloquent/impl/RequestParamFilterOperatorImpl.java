package io.github.barmoury.eloquent.impl;

import io.github.barmoury.eloquent.RequestParamFilter;

import java.lang.annotation.Annotation;

public class RequestParamFilterOperatorImpl implements RequestParamFilter {

    Operator operator;
    RequestParamFilter requestParamFilter;

    public RequestParamFilterOperatorImpl(RequestParamFilter requestParamFilter, Operator operator) {
        this.operator = operator;
        this.requestParamFilter = requestParamFilter;
    }

    @Override
    public String column() {
        return requestParamFilter.column();
    }

    @Override
    public String[] aliases() {
        return requestParamFilter.aliases();
    }

    @Override
    public boolean booleanToInt() {
        return requestParamFilter.booleanToInt();
    }

    @Override
    public boolean alwaysQuery() {
        return requestParamFilter.alwaysQuery();
    }

    @Override
    public boolean acceptSnakeCase() {
        return requestParamFilter.acceptSnakeCase();
    }

    @Override
    public Operator operator() {
        return operator;
    }

    @Override
    public boolean columnIsSnakeCase() {
        return requestParamFilter.columnIsSnakeCase();
    }

    @Override
    public String multiFilterSeparator() {
        return requestParamFilter.multiFilterSeparator();
    }

    @Override
    public boolean columnObjectFieldsIsSnakeCase() {
        return requestParamFilter.columnObjectFieldsIsSnakeCase();
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return requestParamFilter.annotationType();
    }
}
