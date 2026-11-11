package org.delcom.ifs25009springbootcashflow.adapter;

import org.delcom.ifs25009springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25009springbootcashflow.domain.entity.Role;
import org.delcom.ifs25009springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25009springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25009springbootcashflow.domain.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinancePresenterTest {

    private final FinancePresenter presenter = new FinancePresenter();
    private final Transaction salary = new Transaction(1, 1, "Gaji", 1_000_000,
            TransactionType.INCOME, null, LocalDateTime.of(2026, 11, 10, 8, 5));

    @Test
    void formatsMoneyWithThousandsSeparatorAndTwoDecimals() {
        assertEquals("Rp 1,000,000.00", presenter.formatMoney(1_000_000));
        assertEquals("Rp 0.00", presenter.formatMoney(0));
        assertEquals("-Rp 25,000.00", presenter.formatMoney(-25_000));
    }

    @Test
    void formatsLineAndBalance() {
        assertEquals("[1] Gaji | Rp 1,000,000.00 | [Pemasukan]", presenter.formatLine(salary));
        assertEquals("Saldo: Rp 4,975,000.00", presenter.formatBalance(4_975_000));
    }

    @Test
    void formatsDateAndUser() {
        assertEquals("10-11-2026 08:05", presenter.formatDateTime(salary.getCreatedAt()));
        assertEquals("-", presenter.formatDateTime(null));
        assertEquals("Budi (budi)", presenter.formatUserName(new User(1, "budi", "h", "Budi", Role.USER, null)));
    }

    @Test
    void buildsMessages() {
        assertEquals("Pemasukan berhasil dicatat: [1] Gaji | Rp 1,000,000.00 | [Pemasukan]", presenter.showAddSuccess(salary));
        assertEquals("Transaksi berhasil diubah: [1] Gaji | Rp 1,000,000.00 | [Pemasukan]", presenter.showUpdateSuccess(salary));
        assertEquals("Transaksi berhasil dihapus: [1] Gaji | Rp 1,000,000.00 | [Pemasukan]", presenter.showDeleteSuccess(salary));
        assertEquals("Pesan", presenter.showValidationError("Pesan"));
        assertEquals("Terjadi kesalahan", presenter.showValidationError(null));
        assertEquals("Terjadi kesalahan", presenter.showValidationError(" "));
    }
}
