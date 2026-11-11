package org.delcom.ifs25009springbootcashflow.adapter.repository;

import org.delcom.ifs25009springbootcashflow.domain.repository.IRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/** Implementasi generik penyimpanan in-memory (untuk unit test). */
public abstract class InMemoryRepository<T> implements IRepository<T, Integer> {
    private final Map<Integer, T> store = new LinkedHashMap<>();
    private int nextId = 1;

    protected abstract Integer idOf(T entity);

    protected abstract void assignId(T entity, int id);

    @Override
    public synchronized T save(T entity) {
        Integer id = idOf(entity);
        if (id == null) {
            id = nextId++;
            assignId(entity, id);
        }
        store.put(id, entity);
        return entity;
    }

    @Override
    public synchronized Optional<T> findById(Integer id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public synchronized List<T> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public synchronized List<T> findBy(Predicate<T> predicate) {
        return store.values().stream().filter(predicate).toList();
    }

    @Override
    public synchronized boolean deleteById(Integer id) {
        return store.remove(id) != null;
    }
}
