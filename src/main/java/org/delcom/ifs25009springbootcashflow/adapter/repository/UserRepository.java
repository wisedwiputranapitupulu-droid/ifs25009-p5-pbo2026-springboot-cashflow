package org.delcom.ifs25009springbootcashflow.adapter.repository;

import org.delcom.ifs25009springbootcashflow.domain.entity.Role;
import org.delcom.ifs25009springbootcashflow.domain.entity.User;
import org.delcom.ifs25009springbootcashflow.domain.repository.IUserRepository;

import java.util.Optional;

/** Repository pengguna in-memory. */
public class UserRepository extends InMemoryRepository<User> implements IUserRepository {

    @Override
    protected Integer idOf(User entity) {
        return entity.getId();
    }

    @Override
    protected void assignId(User entity, int id) {
        entity.setId(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return findAll().stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    @Override
    public User save(String username, String passwordHash, String fullName, Role role) {
        return save(new User(null, username, passwordHash, fullName, role, null));
    }
}
