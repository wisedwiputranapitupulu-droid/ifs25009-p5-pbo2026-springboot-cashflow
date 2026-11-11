package org.delcom.ifs25009springbootcashflow.usecase;

import org.delcom.ifs25009springbootcashflow.adapter.repository.TransactionRepository;
import org.delcom.ifs25009springbootcashflow.adapter.repository.UserRepository;
import org.delcom.ifs25009springbootcashflow.domain.entity.Role;
import org.delcom.ifs25009springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25009springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25009springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25009springbootcashflow.domain.entity.User;
import org.delcom.ifs25009springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25009springbootcashflow.domain.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UseCaseTest {

    private UserRepository userRepository;
    private TransactionRepository transactionRepository;
    private AuthUseCase auth;
    private UserUseCase users;
    private FinanceUseCase finance;

    @BeforeEach
    void setUp() {
        userRepository = new UserRepository();
        transactionRepository = new TransactionRepository();
        auth = new AuthUseCase(userRepository);
        users = new UserUseCase(userRepository);
        finance = new FinanceUseCase(transactionRepository);
    }

    // ---------- Auth ----------

    @Test
    void registerAndLoginHappyPath() throws Exception {
        User user = auth.register("  budi_1 ", "rahasia", " Budi Santoso ");
        assertEquals("budi_1", user.getUsername());
        assertEquals("Budi Santoso", user.getFullName());
        assertEquals(Role.USER, user.getRole());
        assertEquals(user.getId(), auth.login("budi_1", "rahasia").getId());
        assertEquals(user.getId(), auth.login("BUDI_1", "rahasia").getId());
    }

    @Test
    void registerValidatesInput() throws Exception {
        auth.register("budi", "rahasia", "Budi");
        assertThrows(ValidationException.class, () -> auth.register("budi2", "rahasia", " "));
        assertThrows(ValidationException.class, () -> auth.register("budi2", "rahasia", null));
        assertThrows(ValidationException.class, () -> auth.register("ab", "rahasia", "Budi"));
        assertThrows(ValidationException.class, () -> auth.register(null, "rahasia", "Budi"));
        assertThrows(ValidationException.class, () -> auth.register("bud i", "rahasia", "Budi"));
        assertThrows(ValidationException.class, () -> auth.register("budi3", "abc", "Budi"));
        assertThrows(ValidationException.class, () -> auth.register("budi3", null, "Budi"));
        assertThrows(ValidationException.class, () -> auth.register("BUDI", "rahasia", "Budi"));
    }

    @Test
    void loginRejectsWrongCredentials() throws Exception {
        auth.register("budi", "rahasia", "Budi");
        assertThrows(ValidationException.class, () -> auth.login("budi", "salah"));
        assertThrows(ValidationException.class, () -> auth.login("tidak-ada", "rahasia"));
        assertThrows(ValidationException.class, () -> auth.login(" ", "rahasia"));
        assertThrows(ValidationException.class, () -> auth.login(null, "rahasia"));
        assertThrows(ValidationException.class, () -> auth.login("budi", ""));
        assertThrows(ValidationException.class, () -> auth.login("budi", null));
    }

    @Test
    void ensureRootAdminSeedsOnceAndValidates() throws Exception {
        User first = auth.ensureRootAdmin("admin", "admin");
        User second = auth.ensureRootAdmin("admin", "lain");
        assertEquals(Role.ADMIN, first.getRole());
        assertEquals(first.getId(), second.getId());
        assertEquals(1, users.listUsers().size());
        assertThrows(ValidationException.class, () -> auth.ensureRootAdmin(" ", "x"));
        assertThrows(ValidationException.class, () -> auth.ensureRootAdmin(null, "x"));
        assertThrows(ValidationException.class, () -> auth.ensureRootAdmin("a", " "));
        assertThrows(ValidationException.class, () -> auth.ensureRootAdmin("a", null));
    }

    // ---------- User ----------

    @Test
    void userUseCaseProfileAndPhoto() throws Exception {
        User user = auth.register("budi", "rahasia", "Budi");
        auth.register("sari", "rahasia", "Sari");
        assertEquals(List.of("budi", "sari"), users.listUsers().stream().map(User::getUsername).toList());
        assertEquals("Budi", users.getUser(user.getId()).getFullName());

        User updated = users.updateProfile(user.getId(), " Budi Baru ", "");
        assertEquals("Budi Baru", updated.getFullName());
        assertEquals(user.getId(), auth.login("budi", "rahasia").getId());

        users.updateProfile(user.getId(), "Budi Baru", null);
        assertEquals(user.getId(), auth.login("budi", "rahasia").getId());

        users.updateProfile(user.getId(), "Budi Baru", "passbaru");
        assertEquals(user.getId(), auth.login("budi", "passbaru").getId());

        assertEquals("profiles/a.png", users.updatePhoto(user.getId(), "profiles/a.png").getPhotoPath());
    }

    @Test
    void userUseCaseErrors() throws Exception {
        User user = auth.register("budi", "rahasia", "Budi");
        assertThrows(EntityNotFoundException.class, () -> users.getUser(99));
        assertThrows(EntityNotFoundException.class, () -> users.getUser(null));
        assertThrows(EntityNotFoundException.class, () -> users.updateProfile(99, "x", null));
        assertThrows(EntityNotFoundException.class, () -> users.updatePhoto(99, "x"));
        assertThrows(ValidationException.class, () -> users.updateProfile(user.getId(), " ", null));
        assertThrows(ValidationException.class, () -> users.updateProfile(user.getId(), null, null));
        assertThrows(ValidationException.class, () -> users.updateProfile(user.getId(), "Budi", "abc"));
    }

    // ---------- Finance ----------

    @Test
    void addTransactionsAndComputeBalance() throws Exception {
        Transaction income = finance.addIncome(1, " Gaji ", "5000000", "proofs/a.png");
        Transaction expense = finance.addExpense(1, "Makan", " 25000 ", " ");
        assertEquals("Gaji", income.getDescription());
        assertEquals(TransactionType.INCOME, income.getType());
        assertEquals("proofs/a.png", income.getProofPath());
        assertNull(expense.getProofPath());

        FinanceUseCase.Summary summary = finance.getSummary(1);
        assertEquals(new FinanceUseCase.Summary(2, 5_000_000, 25_000, 4_975_000), summary);
        assertEquals(4_975_000, finance.getBalance(1));
        assertEquals(0, finance.getBalance(2));
    }

    @Test
    void validationErrorsOnAdd() {
        assertThrows(ValidationException.class, () -> finance.addIncome(1, " ", "100", null));
        assertThrows(ValidationException.class, () -> finance.addIncome(1, null, "100", null));
        assertThrows(ValidationException.class, () -> finance.addIncome(1, "x".repeat(501), "100", null));
        assertThrows(ValidationException.class, () -> finance.addIncome(1, "ok", null, null));
        assertThrows(ValidationException.class, () -> finance.addIncome(1, "ok", " ", null));
        assertThrows(ValidationException.class, () -> finance.addIncome(1, "ok", "abc", null));
        assertThrows(ValidationException.class, () -> finance.addIncome(1, "ok", "12.5", null));
        assertThrows(ValidationException.class, () -> finance.addIncome(1, "ok", "0", null));
        assertThrows(ValidationException.class, () -> finance.addIncome(1, "ok", "-5", null));
        assertThrows(ValidationException.class, () -> finance.addIncome(1, "ok", "99999999999999999999", null));
        assertThrows(ValidationException.class, () -> finance.addIncome(1, "ok", "1000000000001", null));
        assertThrows(ValidationException.class, () -> finance.addTransaction(1, null, "ok", "100", null));
        assertTrue(finance.list(1, null, null).isEmpty());
    }

    @Test
    void listSearchesCaseInsensitiveAndSorts() throws Exception {
        finance.addIncome(1, "Gaji November", "5000000", null);
        finance.addExpense(1, "Makan siang", "25000", null);
        finance.addExpense(1, "makan malam", "40000", null);
        finance.addIncome(2, "Gaji orang lain", "1", null);

        assertEquals(3, finance.list(1, null, null).size());
        assertEquals(3, finance.list(1, "  ", SortOption.NEWEST).size());
        assertEquals(2, finance.list(1, "MAKAN", SortOption.AMOUNT_DESC).size());
        assertEquals("makan malam", finance.list(1, "MAKAN", SortOption.AMOUNT_DESC).get(0).getDescription());
        assertEquals("Makan siang", finance.list(1, "makan", SortOption.AMOUNT_ASC).get(0).getDescription());
        assertEquals(TransactionType.INCOME, finance.list(1, null, SortOption.INCOME_FIRST).get(0).getType());
        assertEquals(TransactionType.EXPENSE, finance.list(1, null, SortOption.EXPENSE_FIRST).get(0).getType());
        assertTrue(finance.list(1, "tidak ada", null).isEmpty());

        Transaction noDescription = new Transaction(null, 1, null, 5, TransactionType.INCOME, null, null);
        transactionRepository.save(noDescription);
        assertEquals(0, finance.list(1, "zzz", null).stream().filter(t -> t.getDescription() == null).count());
    }

    @Test
    void updateTransactionKeepsOrReplacesProof() throws Exception {
        Transaction t = finance.addIncome(1, "Gaji", "100", "proofs/a.png");

        Transaction kept = finance.updateTransaction(1, t.getId(), "Gaji Baru", "200", TransactionType.EXPENSE, null);
        assertEquals("Gaji Baru", kept.getDescription());
        assertEquals(200, kept.getAmount());
        assertEquals(TransactionType.EXPENSE, kept.getType());
        assertEquals("proofs/a.png", kept.getProofPath());

        assertEquals("proofs/b.png", finance.updateTransaction(1, t.getId(), "G", "1", TransactionType.INCOME, "proofs/b.png").getProofPath());
        assertNull(finance.updateTransaction(1, t.getId(), "G", "1", TransactionType.INCOME, " ").getProofPath());
    }

    @Test
    void updateAndDeleteAreScopedToOwner() throws Exception {
        Transaction t = finance.addIncome(1, "Gaji", "100", null);
        assertThrows(EntityNotFoundException.class, () -> finance.updateTransaction(2, t.getId(), "x", "1", TransactionType.INCOME, null));
        assertThrows(EntityNotFoundException.class, () -> finance.deleteTransaction(2, t.getId()));
        assertThrows(EntityNotFoundException.class, () -> finance.getTransaction(1, 999));
        assertThrows(ValidationException.class, () -> finance.deleteTransaction(1, null));
        assertThrows(ValidationException.class, () -> finance.deleteTransaction(1, 0));
        assertThrows(ValidationException.class, () -> finance.deleteTransaction(1, -3));
        assertThrows(ValidationException.class, () -> finance.updateTransaction(1, t.getId(), " ", "1", TransactionType.INCOME, null));
        assertThrows(ValidationException.class, () -> finance.updateTransaction(1, t.getId(), "ok", "abc", TransactionType.INCOME, null));
        assertThrows(ValidationException.class, () -> finance.updateTransaction(1, t.getId(), "ok", "1", null, null));

        assertEquals(t.getId(), finance.getTransaction(1, t.getId()).getId());
        assertEquals(t.getId(), finance.deleteTransaction(1, t.getId()).getId());
        assertEquals(0, finance.getBalance(1));
        assertThrows(EntityNotFoundException.class, () -> finance.deleteTransaction(1, t.getId()));
    }

    @Test
    void adminCanListOtherUsersButUserCannot() throws Exception {
        User admin = auth.ensureRootAdmin("admin", "admin");
        User budi = auth.register("budi", "rahasia", "Budi");
        finance.addIncome(budi.getId(), "Gaji", "100", null);

        assertEquals(1, finance.listForAdmin(admin, budi.getId(), null, null).size());
        assertThrows(ValidationException.class, () -> finance.listForAdmin(budi, admin.getId(), null, null));
        assertThrows(ValidationException.class, () -> finance.listForAdmin(null, budi.getId(), null, null));
    }

    @Test
    void parseAmountAcceptsExplicitPlusSign() throws Exception {
        assertEquals(10, FinanceUseCase.parseAmount("+10"));
        assertEquals(FinanceUseCase.MAX_AMOUNT, FinanceUseCase.parseAmount(String.valueOf(FinanceUseCase.MAX_AMOUNT)));
    }
}
