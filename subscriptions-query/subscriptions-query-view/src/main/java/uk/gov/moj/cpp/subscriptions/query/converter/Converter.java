package uk.gov.moj.cpp.subscriptions.query.converter;

public interface Converter<T, R> {
    R convert(T source);
}
