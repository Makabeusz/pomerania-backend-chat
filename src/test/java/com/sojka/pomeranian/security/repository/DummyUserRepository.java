package com.sojka.pomeranian.security.repository;

import com.sojka.pomeranian.security.model.User;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Primary
@Repository
public class DummyUserRepository implements UserRepository {

    private final static Map<String, User> db = new HashMap<>();

    @Override
    public boolean existsByUsername(String username) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean existsByEmail(String email) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends User> List<S> saveAll(Iterable<S> entities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<User> findAll() {
        return db.values().stream().toList();
    }

    @Override
    public List<User> findAllById(Iterable<String> strings) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends User> S save(S entity) {
        return (S) db.put(entity.getId(), entity);
    }

    @Override
    public Optional<User> findById(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean existsById(String s) {
        return db.containsKey(s);
    }

    @Override
    public long count() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteById(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(User entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAllById(Iterable<? extends String> strings) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAll(Iterable<? extends User> entities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAll() {
        db.clear();
    }

}
