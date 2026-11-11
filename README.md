# ifs25009-p5-pbo2026-springboot-cashflow

Praktikum P5 PBO 2026 — **JDBC (Java Database Connection)**, studi kasus **Cashflow (Catatan Keuangan)**.
Spring Boot 4.1.2 · Vaadin 25.3.4 · JDBC PostgreSQL · Java 25 · Clean Architecture.

Package: `org.delcom.ifs25009springbootcashflow`

## Menjalankan

1. Buat database sebelum run:
   ```sql
   CREATE DATABASE pbo2026_cashflow;
   ```
2. Salin `.env.example` menjadi `.env`, sesuaikan `DB_USERNAME` / `DB_PASSWORD`.
3. Jalankan:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
   Buka `http://localhost:8080`. Login admin: `ROOT_USERNAME` / `ROOT_PASSWORD` dari `.env` (default `admin` / `admin`).
4. Build JAR (wajib profil production Vaadin):
   ```powershell
   .\mvnw.cmd clean package -Pproduction
   java -jar target\ifs25009-p5-pbo2026-springboot-cashflow-0.0.1-SNAPSHOT.jar
   ```
5. Test + Jacoco: `.\mvnw.cmd clean verify` (laporan: `target/site/jacoco/index.html`).

## Struktur Layer (dependency mengarah ke dalam)

```
domain      entity (Transaction, TransactionType, User, Role, SortOption), exception, repository (port), PasswordHasher
usecase     AuthUseCase, FinanceUseCase, UserUseCase   (tanpa I/O UI)
adapter     presenter (FinancePresenter), repository (InMemory*, Jdbc*, SchemaInitializer), storage (FileStorage)
framework   auth (AuthSession), view (Login, Register, MainLayout, Finance, Profile, Users, UserTransactions, ...)
config      AppConfig  (composition root: bean JDBC, use case, presenter, FileStorage, seed admin, /uploads/**)
```

## Rute & Hak Akses

| Rute | View | Akses |
|---|---|---|
| `/login`, `/register` | LoginView, RegisterView | Publik; bila sudah login dialihkan ke `/` |
| `/uploads/**`, `/icons/**`, `styles.css` | statis | Publik |
| `/` | FinanceView | USER & ADMIN (wajib login) |
| `/profile` | ProfileView | USER & ADMIN (wajib login) |
| `/users` | UsersView | ADMIN saja (USER dialihkan ke `/`) |
| `/user-transactions/{id}` | UserTransactionsView | ADMIN saja, read-only |

## ID elemen form login (otomasi)

`login-username`, `login-password`, `login-submit`.

## Fitur

- Register / login / logout; sesi tetap setelah refresh (cookie `cashflow_uid` + flag `LOGGED_OUT_KEY`); password SHA-256; admin di-seed dari `ROOT_*`.
- Tambah Pemasukan / Pengeluaran (+ bukti gambar opsional), ubah, hapus dengan konfirmasi.
- Kartu ringkasan: total transaksi, total pemasukan, total pengeluaran, saldo.
- Cari keterangan (case-insensitive), urutkan lewat pilihan `SortOption` (6 opsi) atau klik header kolom grid.
- Klik bukti → modal zoom (klik gambar toggle 1.0x ↔ 1.8x, tombol Perkecil / Perbesar / Tutup di kanan footer).
- Format uang `Rp 1,000,000.00` di kartu, grid, dan notifikasi.
- Validasi: keterangan kosong, jumlah non-numerik / ≤ 0, ID tidak valid; error ditampilkan lewat `Notification`.
- Data persisten di PostgreSQL; unit test memakai InMemory, tes JDBC memakai H2.
