package org.delcom.ifs25009springbootcashflow.adapter;

import org.delcom.ifs25009springbootcashflow.adapter.repository.JdbcTransactionRepository;
import org.delcom.ifs25009springbootcashflow.adapter.repository.JdbcUserRepository;
import org.delcom.ifs25009springbootcashflow.adapter.repository.RepositoryException;
import org.delcom.ifs25009springbootcashflow.adapter.repository.SchemaInitializer;
import org.delcom.ifs25009springbootcashflow.adapter.repository.TransactionRepository;
import org.delcom.ifs25009springbootcashflow.adapter.repository.UserRepository;
import org.delcom.ifs25009springbootcashflow.domain.entity.Role;
import org.delcom.ifs25009springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25009springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25009springbootcashflow.domain.entity.User;
import org.delcom.ifs25009springbootcashflow.domain.repository.ITransactionRepository;
import org.delcom.ifs25009springbootcashflow.domain.repository.IUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryTest {

    private static DataSource h2() {
        DriverManagerDataSource ds = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "sa", "");
        SchemaInitializer.initialize(ds);
        return ds;
    }

    private static void exerciseUsers(IUserRepository repo) {
        User saved = repo.save("Budi", "hash", "Budi Santoso", Role.USER);
        assertNotNull(saved.getId());
        assertEquals("Budi", repo.findByUsername("budi").orElseThrow().getUsername());
        assertTrue(repo.findByUsername("tidak-ada").isEmpty());

        saved.setFullName("Budi S");
        saved.setPhotoPath("profiles/a.png");
        repo.save(saved);
        User reloaded = repo.findById(saved.getId()).orElseThrow();
        assertEquals("Budi S", reloaded.getFullName());
        assertEquals("profiles/a.png", reloaded.getPhotoPath());

        repo.save("Admin", "hash", "Admin", Role.ADMIN);
        assertEquals(2, repo.findAll().size());
        assertEquals(1, repo.findBy(User::isAdmin).size());
        assertTrue(repo.findById(999).isEmpty());
        assertTrue(repo.deleteById(saved.getId()));
        assertFalse(repo.deleteById(saved.getId()));
    }

    private static void exerciseTransactions(ITransactionRepository repo, int userId, int otherUserId) {
        Transaction a = repo.save(userId, "Gaji", 5_000_000, TransactionType.INCOME, null);
        repo.save(userId, "Makan", 25_000, TransactionType.EXPENSE, "proofs/x.png");
        repo.save(otherUserId, "Orang lain", 1, TransactionType.INCOME, null);

        assertNotNull(a.getId());
        assertNotNull(a.getCreatedAt());
        assertEquals(2, repo.findByUserId(userId).size());
        assertEquals(3, repo.findAll().size());
        assertEquals(1, repo.findBy(Transaction::isIncome).stream().filter(t -> t.getUserId() == userId).count());

        a.setDescription("Gaji November");
        a.setAmount(6_000_000);
        repo.save(a);
        Transaction reloaded = repo.findById(a.getId()).orElseThrow();
        assertEquals("Gaji November", reloaded.getDescription());
        assertEquals(6_000_000, reloaded.getAmount());
        assertEquals(TransactionType.INCOME, reloaded.getType());

        assertTrue(repo.findById(999).isEmpty());
        assertTrue(repo.deleteById(a.getId()));
        assertFalse(repo.deleteById(a.getId()));
    }

    @Test
    void inMemoryRepositoriesWork() {
        UserRepository users = new UserRepository();
        exerciseUsers(users);
        User u = users.save("u1", "h", "U Satu", Role.USER);
        User other = users.save("u2", "h", "U Dua", Role.USER);
        exerciseTransactions(new TransactionRepository(), u.getId(), other.getId());
    }

    @Test
    void jdbcRepositoriesWorkOnH2() {
        DataSource ds = h2();
        exerciseUsers(new JdbcUserRepository(ds));
        JdbcUserRepository users = new JdbcUserRepository(ds);
        User u = users.save("u1", "h", "U Satu", Role.USER);
        User other = users.save("u2", "h", "U Dua", Role.USER);
        exerciseTransactions(new JdbcTransactionRepository(ds), u.getId(), other.getId());
    }

    @Test
    void schemaInitializerIsIdempotent() {
        DataSource ds = h2();
        SchemaInitializer.initialize(ds);
    }

    @Test
    void failuresAreWrappedInRepositoryException() throws Exception {
        DataSource broken = new DriverManagerDataSource("jdbc:h2:mem:kosong-" + UUID.randomUUID(), "sa", "") {
            @Override
            public Connection getConnection() throws SQLException {
                throw new SQLException("koneksi putus");
            }
        };
        assertThrows(RepositoryException.class, () -> SchemaInitializer.initialize(broken));

        JdbcUserRepository users = new JdbcUserRepository(broken);
        User probe = new User(1, "a", "h", "A", Role.USER, null);
        assertThrows(RepositoryException.class, () -> users.save("a", "h", "A", Role.USER));
        assertThrows(RepositoryException.class, () -> users.save(probe));
        assertThrows(RepositoryException.class, () -> users.findById(1));
        assertThrows(RepositoryException.class, () -> users.findByUsername("a"));
        assertThrows(RepositoryException.class, users::findAll);
        assertThrows(RepositoryException.class, () -> users.deleteById(1));

        JdbcTransactionRepository txs = new JdbcTransactionRepository(broken);
        Transaction t = new Transaction(1, 1, "x", 1, TransactionType.INCOME, null, null);
        assertThrows(RepositoryException.class, () -> txs.save(1, "x", 1, TransactionType.INCOME, null));
        assertThrows(RepositoryException.class, () -> txs.save(t));
        assertThrows(RepositoryException.class, () -> txs.findById(1));
        assertThrows(RepositoryException.class, txs::findAll);
        assertThrows(RepositoryException.class, () -> txs.findByUserId(1));
        assertThrows(RepositoryException.class, () -> txs.deleteById(1));

        Constructor<SchemaInitializer> c = SchemaInitializer.class.getDeclaredConstructor();
        c.setAccessible(true);
        c.newInstance();
    }
}
