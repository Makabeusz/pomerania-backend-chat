package com.sojka.pomeranian.chat.repository;

/**
 * Common CRUD methods for AstraDB repositories.
 *
 * @param <T> Model entity
 */
public interface AstraCrudRepository<T> {

    T save(T t);

    void delete(T t);
}
