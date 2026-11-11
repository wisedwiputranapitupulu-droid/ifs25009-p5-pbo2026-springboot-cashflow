package org.delcom.ifs25009springbootcashflow.domain.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Transaksi keuangan milik satu pengguna. Jumlah disimpan dalam satuan rupiah (long). */
public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private int userId;
    private String description;
    private long amount;
    private TransactionType type;
    private String proofPath;
    private LocalDateTime createdAt;

    public Transaction() {
    }

    public Transaction(Integer id, int userId, String description, long amount,
                       TransactionType type, String proofPath, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.proofPath = proofPath;
        this.createdAt = createdAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getProofPath() {
        return proofPath;
    }

    public void setProofPath(String proofPath) {
        this.proofPath = proofPath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isIncome() {
        return type == TransactionType.INCOME;
    }

    /** Pengaruh transaksi terhadap saldo: pemasukan positif, pengeluaran negatif. */
    public long signedAmount() {
        return isIncome() ? amount : -amount;
    }
}
