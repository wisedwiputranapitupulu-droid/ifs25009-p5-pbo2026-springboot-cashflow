package org.delcom.ifs25009springbootcashflow.usecase;

import org.delcom.ifs25009springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25009springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25009springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25009springbootcashflow.domain.entity.User;
import org.delcom.ifs25009springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25009springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25009springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25009springbootcashflow.domain.repository.ITransactionRepository;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Proses bisnis keuangan: tambah, ubah, cari, urut, hapus, dan hitung saldo. Selalu per pengguna. */
public class FinanceUseCase {
    public static final long MAX_AMOUNT = 1_000_000_000_000L;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final Pattern DIGITS = Pattern.compile("[+-]?\\d+");

    /** Ringkasan keuangan seorang pengguna. */
    public record Summary(int count, long income, long expense, long balance) {
    }

    private final ITransactionRepository transactionRepository;

    public FinanceUseCase(ITransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Transaction addIncome(int userId, String description, String amountText, String proofPath)
            throws ValidationException {
        return addTransaction(userId, TransactionType.INCOME, description, amountText, proofPath);
    }

    public Transaction addExpense(int userId, String description, String amountText, String proofPath)
            throws ValidationException {
        return addTransaction(userId, TransactionType.EXPENSE, description, amountText, proofPath);
    }

    public Transaction addTransaction(int userId, TransactionType type, String description,
                                      String amountText, String proofPath) throws ValidationException {
        validateType(type);
        String cleanDescription = validateDescription(description);
        long amount = parseAmount(amountText);
        return transactionRepository.save(userId, cleanDescription, amount, type, blankToNull(proofPath));
    }

    /** Mengubah transaksi milik pengguna. proofPath null berarti bukti lama dipertahankan. */
    public Transaction updateTransaction(int userId, Integer transactionId, String description, String amountText,
                                         TransactionType type, String proofPath) throws DomainException {
        Transaction existing = findOwned(userId, transactionId);
        validateType(type);
        String cleanDescription = validateDescription(description);
        long amount = parseAmount(amountText);
        existing.setDescription(cleanDescription);
        existing.setAmount(amount);
        existing.setType(type);
        if (proofPath != null) {
            existing.setProofPath(blankToNull(proofPath));
        }
        return transactionRepository.save(existing);
    }

    public Transaction deleteTransaction(int userId, Integer transactionId) throws DomainException {
        Transaction existing = findOwned(userId, transactionId);
        transactionRepository.deleteById(existing.getId());
        return existing;
    }

    public Transaction getTransaction(int userId, Integer transactionId) throws DomainException {
        return findOwned(userId, transactionId);
    }

    /** Daftar transaksi pengguna, difilter keterangan (case-insensitive) lalu diurutkan. */
    public List<Transaction> list(int userId, String keyword, SortOption sortOption) {
        String needle = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        SortOption sort = sortOption == null ? SortOption.NEWEST : sortOption;
        return transactionRepository.findByUserId(userId).stream()
                .filter(t -> needle.isEmpty()
                        || (t.getDescription() != null && t.getDescription().toLowerCase(Locale.ROOT).contains(needle)))
                .sorted(sort.getComparator())
                .toList();
    }

    /** Admin melihat transaksi pengguna lain (read-only). */
    public List<Transaction> listForAdmin(User admin, int targetUserId, String keyword, SortOption sortOption)
            throws ValidationException {
        if (admin == null || !admin.isAdmin()) {
            throw new ValidationException("Hanya admin yang dapat melihat transaksi pengguna lain");
        }
        return list(targetUserId, keyword, sortOption);
    }

    public Summary summarize(List<Transaction> transactions) {
        long income = 0;
        long expense = 0;
        for (Transaction t : transactions) {
            if (t.isIncome()) {
                income += t.getAmount();
            } else {
                expense += t.getAmount();
            }
        }
        return new Summary(transactions.size(), income, expense, income - expense);
    }

    public Summary getSummary(int userId) {
        return summarize(transactionRepository.findByUserId(userId));
    }

    public long getBalance(int userId) {
        return getSummary(userId).balance();
    }

    private Transaction findOwned(int userId, Integer transactionId) throws DomainException {
        if (transactionId == null || transactionId <= 0) {
            throw new ValidationException("ID transaksi tidak valid");
        }
        return transactionRepository.findById(transactionId)
                .filter(t -> t.getUserId() == userId)
                .orElseThrow(() -> new EntityNotFoundException("Transaksi", transactionId));
    }

    private static void validateType(TransactionType type) throws ValidationException {
        if (type == null) {
            throw new ValidationException("Tipe transaksi wajib dipilih");
        }
    }

    private static String validateDescription(String description) throws ValidationException {
        String clean = description == null ? "" : description.trim();
        if (clean.isEmpty()) {
            throw new ValidationException("Keterangan tidak boleh kosong");
        }
        if (clean.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ValidationException("Keterangan maksimal " + MAX_DESCRIPTION_LENGTH + " karakter");
        }
        return clean;
    }

    static long parseAmount(String amountText) throws ValidationException {
        String clean = amountText == null ? "" : amountText.trim();
        if (clean.isEmpty()) {
            throw new ValidationException("Jumlah wajib diisi");
        }
        if (!DIGITS.matcher(clean).matches()) {
            throw new ValidationException("Jumlah harus berupa angka bulat");
        }
        long amount;
        try {
            amount = Long.parseLong(clean);
        } catch (NumberFormatException ex) {
            throw new ValidationException("Jumlah terlalu besar");
        }
        if (amount <= 0) {
            throw new ValidationException("Jumlah harus lebih dari 0");
        }
        if (amount > MAX_AMOUNT) {
            throw new ValidationException("Jumlah terlalu besar");
        }
        return amount;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
