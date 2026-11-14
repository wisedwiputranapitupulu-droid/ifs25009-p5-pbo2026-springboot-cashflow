package org.delcom.ifs25009springbootcashflow.domain;

import org.delcom.ifs25009springbootcashflow.domain.entity.Role;
import org.delcom.ifs25009springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25009springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25009springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25009springbootcashflow.domain.entity.User;
import org.delcom.ifs25009springbootcashflow.domain.exception.EntityNotFoundException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainTest {

    private static Transaction tx(Integer id, long amount, TransactionType type, int minute) {
        return new Transaction(id, 1, "t" + id, amount, type, null, LocalDateTime.of(2026, 11, 1, 10, minute));
    }

    @Test
    void passwordHasherProducesSha256Hex() throws Exception {
        assertEquals("5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8", PasswordHasher.hash("password"));
        assertEquals(PasswordHasher.hash(""), PasswordHasher.hash(null));
        assertTrue(PasswordHasher.matches("password", PasswordHasher.hash("password")));
        assertFalse(PasswordHasher.matches("salah", PasswordHasher.hash("password")));
        assertThrows(IllegalStateException.class, () -> PasswordHasher.hashWith("ALGORITMA-TIDAK-ADA", "x"));
        assertFalse(PasswordHasher.matches("password", null));
        Constructor<PasswordHasher> c = PasswordHasher.class.getDeclaredConstructor();
        c.setAccessible(true);
        c.newInstance();
    }

    @Test
    void userAndTransactionAccessors() {
        User user = new User();
        user.setId(5);
        user.setUsername("budi");
        user.setPasswordHash("h");
        user.setFullName("Budi");
        user.setRole(Role.ADMIN);
        user.setPhotoPath("profiles/a.png");
        assertEquals(5, user.getId());
        assertEquals("budi", user.getUsername());
        assertEquals("h", user.getPasswordHash());
        assertEquals("Budi", user.getFullName());
        assertEquals("profiles/a.png", user.getPhotoPath());
        assertTrue(user.isAdmin());
        user.setRole(Role.USER);
        assertFalse(user.isAdmin());

        Transaction t = new Transaction();
        t.setId(1);
        t.setUserId(5);
        t.setDescription("Gaji");
        t.setAmount(100);
        t.setType(TransactionType.INCOME);
        t.setProofPath("proofs/x.png");
        t.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        assertEquals(1, t.getId());
        assertEquals(5, t.getUserId());
        assertEquals("Gaji", t.getDescription());
        assertEquals("proofs/x.png", t.getProofPath());
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), t.getCreatedAt());
        assertEquals(100, t.signedAmount());
        t.setType(TransactionType.EXPENSE);
        assertFalse(t.isIncome());
        assertEquals(-100, t.signedAmount());
        assertEquals("Pemasukan", TransactionType.INCOME.getLabel());
        assertEquals("Pengeluaran", TransactionType.EXPENSE.getLabel());
    }

    @Test
    void sortOptionsOrderTransactions() {
        Transaction a = tx(1, 500, TransactionType.INCOME, 1);
        Transaction b = tx(2, 100, TransactionType.EXPENSE, 2);
        Transaction c = tx(3, 900, TransactionType.EXPENSE, 3);
        Transaction d = tx(4, 300, TransactionType.INCOME, 4);

        assertEquals(List.of(4, 3, 2, 1), ids(SortOption.NEWEST, a, b, c, d));
        assertEquals(List.of(1, 2, 3, 4), ids(SortOption.OLDEST, a, b, c, d));
        assertEquals(List.of(3, 1, 4, 2), ids(SortOption.AMOUNT_DESC, a, b, c, d));
        assertEquals(List.of(2, 4, 1, 3), ids(SortOption.AMOUNT_ASC, a, b, c, d));
        assertEquals(List.of(1, 4, 3, 2), ids(SortOption.INCOME_FIRST, a, b, c, d));
        assertEquals(List.of(3, 2, 1, 4), ids(SortOption.EXPENSE_FIRST, a, b, c, d));
        for (SortOption option : SortOption.values()) {
            assertFalse(option.getLabel().isBlank());
        }
        // tanggal null diletakkan paling akhir dan tidak membuat comparator error
        Transaction noDate = new Transaction(9, 1, "x", 1, TransactionType.INCOME, null, null);
        assertEquals(9, ids(SortOption.NEWEST, noDate, a).get(1));
    }

    private static List<Integer> ids(SortOption option, Transaction... items) {
        List<Transaction> list = new ArrayList<>(List.of(items));
        list.sort(option.getComparator());
        return list.stream().map(Transaction::getId).toList();
    }

    @Test
    void entityNotFoundExceptionMessageContainsEntityAndId() {
        EntityNotFoundException ex = new EntityNotFoundException("Transaksi", 7);
        assertEquals("Transaksi dengan ID 7 tidak ditemukan", ex.getMessage());
        assertNull(new Transaction().getId());
    }
}
