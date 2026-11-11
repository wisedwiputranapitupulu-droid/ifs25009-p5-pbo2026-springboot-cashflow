package org.delcom.ifs25009springbootcashflow.adapter.repository;

import org.delcom.ifs25009springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25009springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25009springbootcashflow.domain.repository.ITransactionRepository;

import java.time.LocalDateTime;
import java.util.List;

/** Repository transaksi in-memory. */
public class TransactionRepository extends InMemoryRepository<Transaction> implements ITransactionRepository {

    @Override
    protected Integer idOf(Transaction entity) {
        return entity.getId();
    }

    @Override
    protected void assignId(Transaction entity, int id) {
        entity.setId(id);
    }

    @Override
    public Transaction save(int userId, String description, long amount, TransactionType type, String proofPath) {
        return save(new Transaction(null, userId, description, amount, type, proofPath, LocalDateTime.now()));
    }

    @Override
    public List<Transaction> findByUserId(int userId) {
        return findBy(t -> t.getUserId() == userId);
    }
}
