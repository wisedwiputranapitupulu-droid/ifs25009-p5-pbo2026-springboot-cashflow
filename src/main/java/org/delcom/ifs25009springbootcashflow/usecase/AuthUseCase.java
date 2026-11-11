package org.delcom.ifs25009springbootcashflow.usecase;

import org.delcom.ifs25009springbootcashflow.domain.PasswordHasher;
import org.delcom.ifs25009springbootcashflow.domain.entity.Role;
import org.delcom.ifs25009springbootcashflow.domain.entity.User;
import org.delcom.ifs25009springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25009springbootcashflow.domain.repository.IUserRepository;

import java.util.regex.Pattern;

/** Proses bisnis autentikasi: registrasi, login, dan seed admin. */
public class AuthUseCase {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9_.-]{3,50}");
    static final int MIN_PASSWORD_LENGTH = 4;

    private final IUserRepository userRepository;

    public AuthUseCase(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(String username, String password, String fullName) throws ValidationException {
        String cleanUsername = username == null ? "" : username.trim();
        String cleanName = fullName == null ? "" : fullName.trim();
        if (cleanName.isEmpty()) {
            throw new ValidationException("Nama lengkap tidak boleh kosong");
        }
        if (!USERNAME_PATTERN.matcher(cleanUsername).matches()) {
            throw new ValidationException("Username 3-50 karakter (huruf, angka, titik, garis bawah, atau strip)");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Password minimal " + MIN_PASSWORD_LENGTH + " karakter");
        }
        if (userRepository.findByUsername(cleanUsername).isPresent()) {
            throw new ValidationException("Username sudah dipakai");
        }
        return userRepository.save(cleanUsername, PasswordHasher.hash(password), cleanName, Role.USER);
    }

    public User login(String username, String password) throws ValidationException {
        String cleanUsername = username == null ? "" : username.trim();
        if (cleanUsername.isEmpty() || password == null || password.isEmpty()) {
            throw new ValidationException("Username dan password wajib diisi");
        }
        return userRepository.findByUsername(cleanUsername)
                .filter(user -> PasswordHasher.matches(password, user.getPasswordHash()))
                .orElseThrow(() -> new ValidationException("Username atau password salah"));
    }

    /** Membuat admin dari konfigurasi ROOT_* bila belum ada. */
    public User ensureRootAdmin(String username, String password) throws ValidationException {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new ValidationException("ROOT_USERNAME dan ROOT_PASSWORD wajib diisi");
        }
        String cleanUsername = username.trim();
        return userRepository.findByUsername(cleanUsername).orElseGet(() ->
                userRepository.save(cleanUsername, PasswordHasher.hash(password), "Administrator", Role.ADMIN));
    }
}
