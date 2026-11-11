package org.delcom.ifs25009springbootcashflow.adapter.repository;

import org.delcom.ifs25009springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25009springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25009springbootcashflow.domain.repository.ITransactionRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Repository transaksi berbasis JDBC (PostgreSQL / H2). */
public class JdbcTransactionRepository implements ITransactionRepository {
    private static final String COLUMNS = "id, user_id, description, amount, type, proof_path, created_at";

    private final DataSource dataSource;

    public JdbcTransactionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Transaction save(Transaction t) {
        return t.getId() == null ? insert(t) : update(t);
    }

    @Override
    public Transaction save(int userId, String description, long amount, TransactionType type, String proofPath) {
        return insert(new Transaction(null, userId, description, amount, type, proofPath, LocalDateTime.now()));
    }

    private Transaction insert(Transaction t) {
        String sql = "INSERT INTO transactions (user_id, description, amount, type, proof_path, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (t.getCreatedAt() == null) {
                t.setCreatedAt(LocalDateTime.now());
            }
            ps.setInt(1, t.getUserId());
            ps.setString(2, t.getDescription());
            ps.setLong(3, t.getAmount());
            ps.setString(4, t.getType().name());
            ps.setString(5, t.getProofPath());
            ps.setTimestamp(6, Timestamp.valueOf(t.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    t.setId(keys.getInt("id"));
                }
            }
            return t;
        } catch (SQLException ex) {
            throw new RepositoryException("Gagal menyimpan transaksi", ex);
        }
    }

    private Transaction update(Transaction t) {
        String sql = "UPDATE transactions SET user_id = ?, description = ?, amount = ?, type = ?, proof_path = ? WHERE id = ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, t.getUserId());
            ps.setString(2, t.getDescription());
            ps.setLong(3, t.getAmount());
            ps.setString(4, t.getType().name());
            ps.setString(5, t.getProofPath());
            ps.setInt(6, t.getId());
            ps.executeUpdate();
            return t;
        } catch (SQLException ex) {
            throw new RepositoryException("Gagal memperbarui transaksi", ex);
        }
    }

    @Override
    public Optional<Transaction> findById(Integer id) {
        String sql = "SELECT " + COLUMNS + " FROM transactions WHERE id = ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new RepositoryException("Gagal membaca transaksi", ex);
        }
    }

    @Override
    public List<Transaction> findAll() {
        return query("SELECT " + COLUMNS + " FROM transactions ORDER BY id", null);
    }

    @Override
    public List<Transaction> findByUserId(int userId) {
        return query("SELECT " + COLUMNS + " FROM transactions WHERE user_id = ? ORDER BY id", userId);
    }

    @Override
    public List<Transaction> findBy(Predicate<Transaction> predicate) {
        return findAll().stream().filter(predicate).toList();
    }

    @Override
    public boolean deleteById(Integer id) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM transactions WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RepositoryException("Gagal menghapus transaksi", ex);
        }
    }

    private List<Transaction> query(String sql, Integer userId) {
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (userId != null) {
                ps.setInt(1, userId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Transaction> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(map(rs));
                }
                return result;
            }
        } catch (SQLException ex) {
            throw new RepositoryException("Gagal membaca transaksi", ex);
        }
    }

    private static Transaction map(ResultSet rs) throws SQLException {
        return new Transaction(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("description"),
                rs.getLong("amount"),
                TransactionType.valueOf(rs.getString("type")),
                rs.getString("proof_path"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }
}
