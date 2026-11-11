package org.delcom.ifs25009springbootcashflow.adapter.presenter;

import org.delcom.ifs25009springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25009springbootcashflow.domain.entity.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Format tampilan uang, baris transaksi, tanggal, dan pesan notifikasi. */
public class FinancePresenter {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    /** Contoh: 1000000 -> "Rp 1,000,000.00". */
    public String formatMoney(long amount) {
        String sign = amount < 0 ? "-" : "";
        return String.format(Locale.US, "%sRp %,d.00", sign, Math.abs(amount));
    }

    /** Contoh: "[1] Gaji | Rp 1,000,000.00 | [Pemasukan]". */
    public String formatLine(Transaction t) {
        return "[" + t.getId() + "] " + t.getDescription() + " | " + formatMoney(t.getAmount())
                + " | [" + t.getType().getLabel() + "]";
    }

    /** Contoh: "Saldo: Rp 4,975,000.00". */
    public String formatBalance(long balance) {
        return "Saldo: " + formatMoney(balance);
    }

    public String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : DATE_TIME.format(value);
    }

    public String formatUserName(User user) {
        return user.getFullName() + " (" + user.getUsername() + ")";
    }

    public String showAddSuccess(Transaction t) {
        return t.getType().getLabel() + " berhasil dicatat: " + formatLine(t);
    }

    public String showUpdateSuccess(Transaction t) {
        return "Transaksi berhasil diubah: " + formatLine(t);
    }

    public String showDeleteSuccess(Transaction t) {
        return "Transaksi berhasil dihapus: " + formatLine(t);
    }

    public String showValidationError(String message) {
        return message == null || message.isBlank() ? "Terjadi kesalahan" : message;
    }
}
