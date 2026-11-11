package org.delcom.ifs25009springbootcashflow.framework.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import org.delcom.ifs25009springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25009springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25009springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25009springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25009springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25009springbootcashflow.domain.entity.User;
import org.delcom.ifs25009springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25009springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25009springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25009springbootcashflow.usecase.FinanceUseCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;

/** Dashboard transaksi keuangan pribadi: ringkasan saldo, pencarian, pengurutan, tambah, ubah, hapus. */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Cash Flow")
public class FinanceView extends VerticalLayout {

    private static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024;

    private final FinanceUseCase financeUseCase;
    private final FinancePresenter presenter;
    private final FileStorage fileStorage;
    private final IUserRepository userRepository;

    private final Grid<Transaction> grid = new Grid<>(Transaction.class, false);
    private final TextField searchField = new TextField();
    private final Select<SortOption> sortSelect = new Select<>();
    private final Span statusLabel = new Span();
    private final Span totalCount = new Span("0");
    private final Span incomeTotal = new Span("Rp 0.00");
    private final Span expenseTotal = new Span("Rp 0.00");
    private final Span balanceTotal = new Span("Rp 0.00");

    public FinanceView(FinanceUseCase financeUseCase, FinancePresenter presenter,
                       FileStorage fileStorage, IUserRepository userRepository) {
        this.financeUseCase = financeUseCase;
        this.presenter = presenter;
        this.fileStorage = fileStorage;
        this.userRepository = userRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        getStyle().set("background", "#F3F4F6");

        H1 title = new H1("Catatan Keuangan");
        title.getStyle().set("margin", "0");
        Paragraph subtitle = new Paragraph("Catat pemasukan dan pengeluaran, lalu pantau saldo kamu.");
        subtitle.getStyle().set("margin", "0").set("color", "#4B5563");

        configureGrid();
        Div card = PageCard.wrap(title, subtitle, buildSummary(), buildToolbar(), statusLabel, grid);
        add(card);
        expand(card);
        refreshAll();
    }

    private User requireUser() {
        return AuthSession.currentUser(userRepository)
                .orElseThrow(() -> new IllegalStateException("Sesi login tidak ditemukan"));
    }

    // ---------- Ringkasan ----------

    private Component buildSummary() {
        HorizontalLayout row = new HorizontalLayout(
                summaryCard("Total Transaksi", totalCount, "#EFF6FF", "#2563EB", "#1E3A8A"),
                summaryCard("Total Pemasukan", incomeTotal, "#ECFDF5", "#059669", "#065F46"),
                summaryCard("Total Pengeluaran", expenseTotal, "#FEF2F2", "#DC2626", "#991B1B"),
                summaryCard("Saldo", balanceTotal, "#FFFBEB", "#D97706", "#92400E"));
        row.setWidthFull();
        row.setPadding(false);
        row.setSpacing(true);
        row.getStyle().set("flex-wrap", "wrap");
        return row;
    }

    private Div summaryCard(String label, Span value, String background, String accent, String valueColor) {
        Span caption = new Span(label);
        caption.getStyle()
                .set("display", "block")
                .set("font-size", "0.875rem")
                .set("font-weight", "500")
                .set("color", accent);
        value.getStyle()
                .set("display", "block")
                .set("margin-top", "0.35rem")
                .set("font-size", "1.35rem")
                .set("font-weight", "700")
                .set("line-height", "1.2")
                .set("color", valueColor);
        Div card = new Div(caption, value);
        card.getStyle()
                .set("flex", "1 1 200px")
                .set("min-width", "0")
                .set("background", background)
                .set("border", "1px solid " + accent)
                .set("border-left", "6px solid " + accent)
                .set("border-radius", "12px")
                .set("padding", "1rem 1.1rem")
                .set("box-sizing", "border-box");
        return card;
    }

    // ---------- Toolbar ----------

    private Component buildToolbar() {
        searchField.setPlaceholder("Cari keterangan...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setValueChangeTimeout(300);
        searchField.setWidth("260px");
        searchField.addValueChangeListener(event -> refreshAll());

        sortSelect.setItems(SortOption.values());
        sortSelect.setItemLabelGenerator(SortOption::getLabel);
        sortSelect.setValue(SortOption.NEWEST);
        sortSelect.setWidth("200px");
        sortSelect.addValueChangeListener(event -> refreshAll());

        Button reloadButton = new Button("Muat Ulang", VaadinIcon.REFRESH.create(), event -> refreshAll());
        reloadButton.getStyle().set("cursor", "pointer");

        Button incomeButton = new Button("Tambah Pemasukan", VaadinIcon.PLUS.create(),
                event -> openAddDialog(TransactionType.INCOME));
        incomeButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
        incomeButton.getStyle().set("cursor", "pointer");

        Button expenseButton = new Button("Tambah Pengeluaran", VaadinIcon.MINUS.create(),
                event -> openAddDialog(TransactionType.EXPENSE));
        expenseButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        expenseButton.getStyle().set("cursor", "pointer");

        HorizontalLayout toolbar = new HorizontalLayout(searchField, sortSelect, reloadButton, incomeButton, expenseButton);
        toolbar.setWidthFull();
        toolbar.setPadding(false);
        toolbar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        toolbar.getStyle().set("flex-wrap", "wrap");
        return toolbar;
    }

    // ---------- Grid ----------

    private void configureGrid() {
        grid.addColumn(Transaction::getId).setHeader("ID").setKey("id").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(Transaction::getDescription).setHeader("Keterangan").setKey("description").setFlexGrow(1);
        grid.addColumn(t -> presenter.formatMoney(t.getAmount()))
                .setHeader("Jumlah").setKey("amount").setAutoWidth(true)
                .setComparator(Comparator.comparingLong(Transaction::getAmount));
        grid.addComponentColumn(this::createTypeBadge).setHeader("Tipe").setKey("type").setAutoWidth(true)
                .setComparator(Comparator.comparing(Transaction::getType));
        grid.addComponentColumn(this::createProofPreview).setHeader("Bukti").setKey("proof")
                .setAutoWidth(true).setFlexGrow(0).setSortable(false);
        grid.addColumn(t -> presenter.formatDateTime(t.getCreatedAt())).setHeader("Waktu").setKey("createdAt")
                .setAutoWidth(true).setComparator(Comparator.comparing(Transaction::getCreatedAt));
        grid.addComponentColumn(this::createActions).setHeader("Tindakan").setKey("actions")
                .setAutoWidth(true).setFlexGrow(0).setSortable(false);
        grid.addItemDoubleClickListener(event -> openEditDialog(event.getItem()));
        grid.setSizeFull();
        grid.setMinHeight("320px");
        GridStyles.apply(grid);
    }

    private Span createTypeBadge(Transaction t) {
        Span badge = new Span(t.getType().getLabel());
        boolean income = t.isIncome();
        badge.getStyle()
                .set("display", "inline-block")
                .set("padding", "0.15rem 0.6rem")
                .set("border-radius", "999px")
                .set("font-size", "0.8rem")
                .set("font-weight", "600")
                .set("background", income ? "#D1FAE5" : "#FEE2E2")
                .set("color", income ? "#065F46" : "#991B1B");
        return badge;
    }

    private Component createProofPreview(Transaction t) {
        if (t.getProofPath() == null || t.getProofPath().isBlank()) {
            return new Span("-");
        }
        return ImagePreviewDialog.clickableThumbnail("/uploads/" + t.getProofPath(), "Bukti transaksi #" + t.getId(), "40px");
    }

    private Component createActions(Transaction t) {
        Button edit = new Button("Ubah", VaadinIcon.EDIT.create(), event -> openEditDialog(t));
        edit.addThemeVariants(ButtonVariant.LUMO_SMALL);
        edit.getStyle().set("cursor", "pointer");
        Button delete = new Button("Hapus", VaadinIcon.TRASH.create(), event -> openDeleteDialog(t));
        delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        delete.getStyle().set("cursor", "pointer");
        HorizontalLayout actions = new HorizontalLayout(edit, delete);
        actions.setPadding(false);
        actions.setSpacing(true);
        return actions;
    }

    private void refreshAll() {
        User user = requireUser();
        var items = financeUseCase.list(user.getId(), searchField.getValue(), sortSelect.getValue());
        grid.setItems(items);

        FinanceUseCase.Summary summary = financeUseCase.getSummary(user.getId());
        totalCount.setText(String.valueOf(summary.count()));
        incomeTotal.setText(presenter.formatMoney(summary.income()));
        expenseTotal.setText(presenter.formatMoney(summary.expense()));
        balanceTotal.setText(presenter.formatMoney(summary.balance()));

        if (summary.count() == 0) {
            statusLabel.setText("Belum ada transaksi. Mulai dengan menambah pemasukan atau pengeluaran.");
        } else if (items.isEmpty()) {
            statusLabel.setText("Tidak ada transaksi yang cocok dengan pencarian.");
        } else {
            statusLabel.setText(presenter.formatBalance(summary.balance()) + "  \u2022  Menampilkan "
                    + items.size() + " dari " + summary.count() + " transaksi");
        }
        statusLabel.getStyle().set("color", "#4B5563").set("font-size", "0.9rem");
    }

    // ---------- Dialog tambah / ubah / hapus ----------

    private void openAddDialog(TransactionType type) {
        TextField descriptionField = new TextField("Keterangan");
        descriptionField.setWidthFull();
        descriptionField.setId("transaction-description");
        TextField amountField = new TextField("Jumlah (Rp)");
        amountField.setWidthFull();
        amountField.setId("transaction-amount");
        amountField.setHelperText("Angka bulat lebih dari 0, contoh: 1000000");

        AtomicReference<String> pendingName = new AtomicReference<>();
        AtomicReference<byte[]> pendingData = new AtomicReference<>();
        Upload upload = proofUpload(pendingName, pendingData);

        Dialog dialog = new Dialog();
        ProgressBar loadingBar = new ProgressBar();

        Button saveButton = new Button("Simpan", VaadinIcon.CHECK.create());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(event -> {
            loadingBar.setVisible(true);
            saveButton.setEnabled(false);
            Transaction created = null;
            try {
                User user = requireUser();
                created = financeUseCase.addTransaction(user.getId(), type,
                        descriptionField.getValue(), amountField.getValue(), null);
                if (pendingName.get() != null && pendingData.get() != null) {
                    String path = fileStorage.store("proofs", pendingName.get(),
                            new ByteArrayInputStream(pendingData.get()));
                    created = financeUseCase.updateTransaction(user.getId(), created.getId(),
                            created.getDescription(), String.valueOf(created.getAmount()), created.getType(), path);
                }
                showSuccess(presenter.showAddSuccess(created));
                dialog.close();
                refreshAll();
            } catch (DomainException | IOException ex) {
                rollbackCreated(created);
                loadingBar.setVisible(false);
                saveButton.setEnabled(true);
                showError(presenter.showValidationError(ex.getMessage()));
            }
        });
        Button cancelButton = new Button("Batal", VaadinIcon.CLOSE.create(), event -> dialog.close());

        VerticalLayout body = new VerticalLayout(descriptionField, amountField, new Span("Bukti transaksi (opsional)"), upload);
        body.setPadding(false);
        body.setSpacing(true);
        String title = type == TransactionType.INCOME ? "Menambah Pemasukan" : "Menambah Pengeluaran";
        ModalDialogs.build(dialog, title, ModalDialogs.Tone.PRIMARY, loadingBar, body, cancelButton, saveButton);
        dialog.open();
    }

    private void rollbackCreated(Transaction created) {
        if (created == null || created.getProofPath() != null) {
            return;
        }
        // Transaksi sudah dibuat tetapi bukti gagal disimpan: batalkan agar data tidak setengah jadi.
        try {
            financeUseCase.deleteTransaction(created.getUserId(), created.getId());
        } catch (DomainException ignored) {
            // abaikan: transaksi mungkin sudah tidak ada
        }
    }

    private void openEditDialog(Transaction transaction) {
        TextField descriptionField = new TextField("Keterangan");
        descriptionField.setWidthFull();
        descriptionField.setValue(transaction.getDescription());
        TextField amountField = new TextField("Jumlah (Rp)");
        amountField.setWidthFull();
        amountField.setValue(String.valueOf(transaction.getAmount()));
        Select<TransactionType> typeSelect = new Select<>();
        typeSelect.setLabel("Tipe");
        typeSelect.setItems(TransactionType.values());
        typeSelect.setItemLabelGenerator(TransactionType::getLabel);
        typeSelect.setValue(transaction.getType());
        typeSelect.setWidthFull();

        AtomicReference<String> pendingName = new AtomicReference<>();
        AtomicReference<byte[]> pendingData = new AtomicReference<>();
        Upload upload = proofUpload(pendingName, pendingData);

        Dialog dialog = new Dialog();
        ProgressBar loadingBar = new ProgressBar();

        Button saveButton = new Button("Simpan", VaadinIcon.CHECK.create());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(event -> {
            loadingBar.setVisible(true);
            saveButton.setEnabled(false);
            try {
                User user = requireUser();
                String newProof = null;
                if (pendingName.get() != null && pendingData.get() != null) {
                    newProof = fileStorage.store("proofs", pendingName.get(),
                            new ByteArrayInputStream(pendingData.get()));
                }
                String oldProof = transaction.getProofPath();
                Transaction updated;
                try {
                    updated = financeUseCase.updateTransaction(user.getId(), transaction.getId(),
                            descriptionField.getValue(), amountField.getValue(), typeSelect.getValue(), newProof);
                } catch (DomainException ex) {
                    fileStorage.delete(newProof);
                    throw ex;
                }
                if (newProof != null && oldProof != null) {
                    fileStorage.delete(oldProof);
                }
                showSuccess(presenter.showUpdateSuccess(updated));
                dialog.close();
                refreshAll();
            } catch (DomainException | IOException ex) {
                loadingBar.setVisible(false);
                saveButton.setEnabled(true);
                showError(presenter.showValidationError(ex.getMessage()));
            }
        });
        Button cancelButton = new Button("Batal", VaadinIcon.CLOSE.create(), event -> dialog.close());

        VerticalLayout body = new VerticalLayout(descriptionField, amountField, typeSelect,
                new Span("Ganti bukti transaksi (opsional)"), upload);
        body.setPadding(false);
        body.setSpacing(true);
        ModalDialogs.build(dialog, "Mengubah Transaksi #" + transaction.getId(), ModalDialogs.Tone.PRIMARY,
                loadingBar, body, cancelButton, saveButton);
        dialog.open();
    }

    private void openDeleteDialog(Transaction transaction) {
        Dialog dialog = new Dialog();
        ProgressBar loadingBar = new ProgressBar();

        Span message = new Span("Hapus transaksi berikut? Tindakan ini tidak dapat dibatalkan.");
        Span detail = new Span(presenter.formatLine(transaction));
        detail.getStyle().set("display", "block").set("margin-top", "0.5rem").set("font-weight", "600");
        Div body = new Div(message, detail);

        Button confirmButton = new Button("Hapus", VaadinIcon.TRASH.create());
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        confirmButton.addClickListener(event -> {
            loadingBar.setVisible(true);
            confirmButton.setEnabled(false);
            try {
                User user = requireUser();
                Transaction deleted = financeUseCase.deleteTransaction(user.getId(), transaction.getId());
                fileStorage.delete(deleted.getProofPath());
                showSuccess(presenter.showDeleteSuccess(deleted));
                dialog.close();
                refreshAll();
            } catch (DomainException | IOException ex) {
                loadingBar.setVisible(false);
                confirmButton.setEnabled(true);
                showError(presenter.showValidationError(ex.getMessage()));
            }
        });
        Button cancelButton = new Button("Batal", VaadinIcon.CLOSE.create(), event -> dialog.close());
        ModalDialogs.build(dialog, "Menghapus Transaksi", ModalDialogs.Tone.DANGER, loadingBar, body,
                cancelButton, confirmButton);
        dialog.open();
    }

    private Upload proofUpload(AtomicReference<String> pendingName, AtomicReference<byte[]> pendingData) {
        Upload upload = new Upload(UploadHandler.inMemory((metadata, data) -> {
            pendingName.set(metadata.fileName());
            pendingData.set(data);
        }));
        upload.setAcceptedMimeTypes("image/png", "image/jpeg", "image/webp");
        upload.setAcceptedFileExtensions(".png", ".jpg", ".jpeg", ".webp");
        upload.setMaxFiles(1);
        upload.setMaxFileSize((int) MAX_UPLOAD_BYTES);
        upload.setWidthFull();
        return upload;
    }

    private void showSuccess(String message) {
        Notification n = Notification.show(message, 3000, Notification.Position.BOTTOM_START);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showError(String message) {
        Notification n = Notification.show(message, 4000, Notification.Position.BOTTOM_START);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
