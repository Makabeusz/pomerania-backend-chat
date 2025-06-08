package com.sojka.pomeranian.chat.repository;

/**
 * Common methods for AstraDB repositories.
 *
 * @param <T>
 */
public interface AstraRepository<T> {

    T save(T t);

    void delete(T t);
}
