package org.delcom.ifs25009springbootcashflow.framework.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import org.delcom.ifs25009springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25009springbootcashflow.domain.entity.User;
import org.delcom.ifs25009springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25009springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25009springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25009springbootcashflow.usecase.UserUseCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/** Pengaturan akun: nama, password, dan foto profil. */
@Route(value = "profile", layout = MainLayout.class)
@PageTitle("Profil | Cash Flow")
public class ProfileView extends VerticalLayout {

    private final Div photoFrame = new Div();
    private final Image photoPreview = new Image();
    private final Span photoInitial = new Span();
    private final Span photoPlaceholder = new Span("Belum ada foto profil. Unggah foto di bawah.");

    public ProfileView(UserUseCase userUseCase, FileStorage fileStorage, IUserRepository userRepository) {
        setSizeFull();
        setPadding(true);
        setSpacing(false);
        getStyle().set("background", "#F3F4F6");

        User user = AuthSession.currentUser(userRepository).orElse(null);
        if (user == null) {
            return; // MainLayout sudah mengarahkan ke /login
        }

        TextField fullName = new TextField("Nama Lengkap");
        fullName.setWidthFull();
        fullName.setValue(user.getFullName());
        TextField username = new TextField("Username");
        username.setWidthFull();
        username.setValue(user.getUsername());
        username.setReadOnly(true);
        PasswordField password = new PasswordField("Password Baru (opsional)");
        password.setWidthFull();

        configurePhotoFrame();
        refreshPhoto(user);

        AtomicReference<String> pendingName = new AtomicReference<>();
        AtomicReference<byte[]> pendingData = new AtomicReference<>();
        Upload upload = new Upload(UploadHandler.inMemory((metadata, data) -> {
            pendingName.set(metadata.fileName());
            pendingData.set(data);
        }));
        upload.setAcceptedMimeTypes("image/png", "image/jpeg", "image/webp");
        upload.setAcceptedFileExtensions(".png", ".jpg", ".jpeg", ".webp");
        upload.setMaxFiles(1);
        upload.setMaxFileSize(5 * 1024 * 1024);
        upload.setWidthFull();

        ProgressBar loadingBar = new ProgressBar();
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidthFull();

        Button savePhoto = new Button("Simpan Foto");
        savePhoto.getStyle().set("cursor", "pointer");
        Button saveProfile = new Button("Simpan Profil");
        saveProfile.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveProfile.getStyle().set("margin-top", "0.5rem").set("cursor", "pointer");

        savePhoto.addClickListener(event -> {
            loadingBar.setVisible(true);
            savePhoto.setEnabled(false);
            try {
                if (pendingName.get() == null || pendingData.get() == null) {
                    throw new DomainException("Pilih file foto terlebih dahulu");
                }
                String oldPhoto = userUseCase.getUser(user.getId()).getPhotoPath();
                String path = fileStorage.store("profiles", pendingName.get(),
                        new ByteArrayInputStream(pendingData.get()));
                User updated = userUseCase.updatePhoto(user.getId(), path);
                fileStorage.delete(oldPhoto);
                AuthSession.refresh(updated);
                showSuccess("Foto profil diperbarui");
                // Muat ulang halaman agar avatar di header ikut berubah.
                UI.getCurrent().getPage().reload();
            } catch (DomainException | IOException ex) {
                loadingBar.setVisible(false);
                savePhoto.setEnabled(true);
                showError(ex.getMessage());
            }
        });

        saveProfile.addClickListener(event -> {
            loadingBar.setVisible(true);
            saveProfile.setEnabled(false);
            try {
                User updated = userUseCase.updateProfile(user.getId(), fullName.getValue(), password.getValue());
                AuthSession.refresh(updated);
                password.clear();
                loadingBar.setVisible(false);
                saveProfile.setEnabled(true);
                showSuccess("Profil diperbarui");
            } catch (DomainException ex) {
                loadingBar.setVisible(false);
                saveProfile.setEnabled(true);
                showError(ex.getMessage());
            }
        });

        H1 title = new H1("Profil");
        title.getStyle().set("margin", "0");
        Paragraph subtitle = new Paragraph("Perbarui nama, password, dan foto profil.");
        subtitle.getStyle().set("margin", "0").set("color", "#4B5563");
        Span photoLabel = new Span("Foto Profil");
        photoLabel.getStyle().set("font-weight", "600").set("color", "#1F2937");

        HorizontalLayout photoRow = new HorizontalLayout(photoFrame, photoPlaceholder);
        photoRow.setAlignItems(FlexComponent.Alignment.CENTER);
        photoRow.setWidthFull();

        Div card = PageCard.wrap(title, subtitle, photoLabel, photoRow, upload, savePhoto,
                fullName, username, password, loadingBar, saveProfile);
        card.setHeight(null);
        card.getStyle().set("max-width", "560px");
        add(card);
    }

    private void configurePhotoFrame() {
        photoPreview.setWidth("120px");
        photoPreview.setHeight("120px");
        photoPreview.getStyle()
                .set("border-radius", "50%")
                .set("object-fit", "cover")
                .set("display", "block")
                .set("cursor", "pointer");
        photoPreview.getElement().setAttribute("title", "Klik untuk memperbesar");
        photoPreview.addClickListener(
                event -> ImagePreviewDialog.open(photoPreview.getSrc(), photoPreview.getAlt().orElse("Foto profil")));

        photoInitial.getStyle()
                .set("width", "120px")
                .set("height", "120px")
                .set("border-radius", "50%")
                .set("display", "inline-flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("background", "#DBEAFE")
                .set("color", "#1D4ED8")
                .set("font-weight", "700")
                .set("font-size", "2.5rem");

        photoFrame.getStyle()
                .set("width", "120px")
                .set("height", "120px")
                .set("border-radius", "50%")
                .set("overflow", "hidden")
                .set("border", "3px solid #93C5FD")
                .set("flex-shrink", "0")
                .set("background", "#EFF6FF");
        photoPlaceholder.getStyle().set("color", "#6B7280");
    }

    private void refreshPhoto(User user) {
        String source = user.getFullName() == null || user.getFullName().isBlank()
                ? user.getUsername() : user.getFullName();
        photoInitial.setText(source.substring(0, 1).toUpperCase());

        photoFrame.removeAll();
        if (user.getPhotoPath() == null || user.getPhotoPath().isBlank()) {
            photoFrame.add(photoInitial);
            photoPlaceholder.setVisible(true);
            return;
        }
        photoPreview.setSrc("/uploads/" + user.getPhotoPath());
        photoPreview.setAlt(user.getFullName());
        photoFrame.add(photoPreview);
        photoPlaceholder.setVisible(false);
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
