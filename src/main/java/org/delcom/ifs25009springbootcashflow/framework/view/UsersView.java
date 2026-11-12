package org.delcom.ifs25009springbootcashflow.framework.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.delcom.ifs25009springbootcashflow.domain.entity.User;
import org.delcom.ifs25009springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25009springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25009springbootcashflow.usecase.UserUseCase;

/** Daftar seluruh pengguna. Semua user login boleh melihat; tombol transaksi hanya untuk ADMIN. */
@Route(value = "users", layout = MainLayout.class)
@PageTitle("Daftar Pengguna | Cash Flow")
public class UsersView extends VerticalLayout implements BeforeEnterObserver {

    private final IUserRepository userRepository;

    public UsersView(UserUseCase userUseCase, IUserRepository userRepository) {
        this.userRepository = userRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        getStyle().set("background", "#F3F4F6");

        Grid<User> grid = new Grid<>(User.class, false);
        grid.addComponentColumn(user -> {
            if (user.getPhotoPath() == null || user.getPhotoPath().isBlank()) {
                return new Span("-");
            }
            return ImagePreviewDialog.clickableThumbnail("/uploads/" + user.getPhotoPath(), "Foto", "40px", "50%");
        }).setHeader("Foto").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(User::getId).setHeader("ID").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(User::getUsername).setHeader("Username").setFlexGrow(1);
        grid.addColumn(User::getFullName).setHeader("Nama").setFlexGrow(1);
        grid.addColumn(user -> user.getRole().name()).setHeader("Role").setAutoWidth(true);
        boolean viewerIsAdmin = AuthSession.currentUser(userRepository).map(User::isAdmin).orElse(false);
        grid.addComponentColumn(user -> {
            if (!viewerIsAdmin) {
                return new Span("-");
            }
            Button open = new Button("Lihat Transaksi", event ->
                    UI.getCurrent().navigate(UserTransactionsView.class, user.getId()));
            open.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            open.getStyle().set("cursor", "pointer");
            return open;
        }).setHeader("Tindakan").setAutoWidth(true).setFlexGrow(0);
        grid.setItems(userUseCase.listUsers());
        grid.setSizeFull();
        GridStyles.apply(grid);

        H1 title = new H1("Daftar Pengguna");
        title.getStyle().set("margin", "0");
        Paragraph subtitle = new Paragraph("Daftar pengguna terdaftar. Admin dapat melihat transaksi pengguna lain (hanya baca).");
        subtitle.getStyle().set("margin", "0").set("color", "#4B5563");

        Div card = PageCard.wrap(title, subtitle, grid);
        add(card);
        expand(card);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        User current = AuthSession.currentUser(userRepository).orElse(null);
        if (current == null) {
            event.forwardTo(LoginView.class);
        }
    }
}
