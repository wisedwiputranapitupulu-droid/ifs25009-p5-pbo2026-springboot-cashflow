package org.delcom.ifs25009springbootcashflow.usecase;

import org.delcom.ifs25009springbootcashflow.domain.PasswordHasher;
import org.delcom.ifs25009springbootcashflow.domain.entity.User;
import org.delcom.ifs25009springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25009springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25009springbootcashflow.domain.repository.IUserRepository;

import java.util.Comparator;
import java.util.List;

/** Proses bisnis pengguna: daftar, profil, dan foto. */
public class UserUseCase {
    private final IUserRepository userRepository;

    public UserUseCase(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> listUsers() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getId))
                .toList();
    }

    public User getUser(Integer id) throws EntityNotFoundException {
        if (id == null) {
            throw new EntityNotFoundException("Pengguna", null);
        }
        return userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Pengguna", id));
    }

    /** Mengubah nama; password hanya diganti bila newPassword tidak kosong. */
    public User updateProfile(Integer id, String fullName, String newPassword)
            throws EntityNotFoundException, ValidationException {
        User user = getUser(id);
        String cleanName = fullName == null ? "" : fullName.trim();
        if (cleanName.isEmpty()) {
            throw new ValidationException("Nama lengkap tidak boleh kosong");
        }
        boolean changePassword = newPassword != null && !newPassword.isEmpty();
        if (changePassword && newPassword.length() < AuthUseCase.MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Password minimal " + AuthUseCase.MIN_PASSWORD_LENGTH + " karakter");
        }
        user.setFullName(cleanName);
        if (changePassword) {
            user.setPasswordHash(PasswordHasher.hash(newPassword));
        }
        return userRepository.save(user);
    }

    public User updatePhoto(Integer id, String photoPath) throws EntityNotFoundException {
        User user = getUser(id);
        user.setPhotoPath(photoPath);
        return userRepository.save(user);
    }
}
