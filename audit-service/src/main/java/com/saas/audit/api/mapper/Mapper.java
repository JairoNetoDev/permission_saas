package com.saas.audit.api.mapper;

public interface Mapper<I, O> {
    O map(I input);
}
