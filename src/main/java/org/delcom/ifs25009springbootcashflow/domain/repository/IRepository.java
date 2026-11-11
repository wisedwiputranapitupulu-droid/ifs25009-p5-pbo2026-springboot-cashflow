package org.delcom.ifs25009springbootcashflow.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Port generik CRUD. Implementasi: InMemory (unit test) dan JDBC (runtime).
 *
 * @param <T>  tipe entitas
 * @param <ID> tipe identitas
 */
public interface IRepository<T, ID> {

    /** Menyimpan entitas baru (id null) atau memperbarui entitas yang sudah ada. */
    T save(T entity);

    Optional<T> findById(ID id);

    List<T> findAll();

    List<T> findBy(Predicate<T> predicate);

    boolean deleteById(ID id);
}
