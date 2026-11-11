package org.delcom.ifs25009springbootcashflow.framework.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.delcom.ifs25009springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25009springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25009springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25009springbootcashflow.domain.entity.User;
import org.delcom.ifs25009springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25009springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25009springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25009springbootcashflow.usecase.FinanceUseCase;
import org.delcom.ifs25009springbootcashflow.usecase.UserUseCase;

import java.util.Comparator;
import java.util.List;

/** Transaksi pengguna lain, hanya baca (khusus ADMIN). */
@Route(value = "user-transactions", layout = MainLayout.class)
@PageTitle("Transaksi Pengguna | Cash Flow")
public class UserTransactionsView extends VerticalLayout implements HasUrlParameter<Integer> {

    private final FinanceUseCase financeUseCase;
    private final UserUseCase userUseCase;
    private final FinancePresenter presenter;
    private final IUserRepository userRepository;

    private final Grid<Transaction> grid = new Grid<>(Transaction.class, false);
    private final H1 title = new H1("Transaksi Pengguna");
    private final Paragraph subtitle = new Paragraph();

    public UserTransactionsView(FinanceUseCase financeUseCase, UserUseCase userUseCase,
                                FinancePresenter presenter, IUserRepository userRepository) {
        this.financeUseCase = financeUseCase;
        this.userUseCase = userUseCase;
        this.presenter = presenter;
        this.userRepository = userRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        getStyle().set("background", "#F3F4F6");

        grid.addColumn(Transaction::getId).setHeader("ID").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(Transaction::getDescription).setHeader("Keterangan").setFlexGrow(1);
        grid.addColumn(t -> presenter.formatMoney(t.getAmount())).setHeader("Jumlah").setAutoWidth(true)
                .setComparator(Comparator.comparingLong(Transaction::getAmount));
        grid.addColumn(t -> t.getType().getLabel()).setHeader("Tipe").setAutoWidth(true);
        grid.addComponentColumn(t -> {
            if (t.getProofPath() == null || t.getProofPath().isBlank()) {
                return new Span("-");
            }
            return ImagePreviewDialog.clickableThumbnail("/uploads/" + t.getProofPath(),
                    "Bukti transaksi #" + t.getId(), "40px");
        }).setHeader("Bukti").setAutoWidth(true).setFlexGrow(0).setSortable(false);
        grid.addColumn(t -> presenter.formatDateTime(t.getCreatedAt())).setHeader("Waktu").setAutoWidth(true)
                .setComparator(Comparator.comparing(Transaction::getCreatedAt));
        grid.setSizeFull();
        GridStyles.apply(grid);

        title.getStyle().set("margin", "0");
        subtitle.getStyle().set("margin", "0").set("color", "#4B5563");

        Button back = new Button("Kembali", VaadinIcon.ARROW_LEFT.create(),
                event -> UI.getCurrent().navigate(UsersView.class));
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        back.getStyle().set("cursor", "pointer");

        Div card = PageCard.wrap(new HorizontalLayout(back), title, subtitle, grid);
        add(card);
        expand(card);
    }

    @Override
    public void setParameter(BeforeEvent event, Integer userId) {
        User current = AuthSession.currentUser(userRepository).orElse(null);
        if (current == null || !current.isAdmin()) {
            event.forwardTo(FinanceView.class);
            return;
        }
        try {
            User target = userUseCase.getUser(userId);
            List<Transaction> items = financeUseCase.listForAdmin(current, target.getId(), null, SortOption.NEWEST);
            FinanceUseCase.Summary summary = financeUseCase.summarize(items);
            title.setText("Transaksi " + presenter.formatUserName(target));
            subtitle.setText("Hanya baca  \u2022  " + summary.count() + " transaksi  \u2022  "
                    + presenter.formatBalance(summary.balance()));
            grid.setItems(items);
        } catch (DomainException ex) {
            event.forwardTo(UsersView.class);
        }
    }
}
