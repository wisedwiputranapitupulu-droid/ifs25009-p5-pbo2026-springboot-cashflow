package org.delcom.ifs25009springbootcashflow.config;

import org.delcom.ifs25009springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25009springbootcashflow.adapter.repository.JdbcTransactionRepository;
import org.delcom.ifs25009springbootcashflow.adapter.repository.JdbcUserRepository;
import org.delcom.ifs25009springbootcashflow.adapter.repository.SchemaInitializer;
import org.delcom.ifs25009springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25009springbootcashflow.domain.repository.ITransactionRepository;
import org.delcom.ifs25009springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25009springbootcashflow.usecase.AuthUseCase;
import org.delcom.ifs25009springbootcashflow.usecase.FinanceUseCase;
import org.delcom.ifs25009springbootcashflow.usecase.UserUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;

/** Composition root: seluruh wiring bean dilakukan di sini, bukan tersebar di view. */
@Configuration
public class AppConfig {

    @Bean
    public IUserRepository userRepository(DataSource dataSource) {
        SchemaInitializer.initialize(dataSource);
        return new JdbcUserRepository(dataSource);
    }

    @Bean
    public ITransactionRepository transactionRepository(DataSource dataSource, IUserRepository userRepository) {
        // Bergantung pada userRepository agar skema (users + transactions) pasti sudah dibuat.
        return new JdbcTransactionRepository(dataSource);
    }

    @Bean
    public AuthUseCase authUseCase(IUserRepository userRepository) {
        return new AuthUseCase(userRepository);
    }

    @Bean
    public UserUseCase userUseCase(IUserRepository userRepository) {
        return new UserUseCase(userRepository);
    }

    @Bean
    public FinanceUseCase financeUseCase(ITransactionRepository transactionRepository) {
        return new FinanceUseCase(transactionRepository);
    }

    @Bean
    public FinancePresenter financePresenter() {
        return new FinancePresenter();
    }

    @Bean
    public FileStorage fileStorage(@Value("${app.upload-dir}") String uploadDir) {
        return new FileStorage(uploadDir);
    }

    @Bean
    public ApplicationRunner seedRootAdmin(
            AuthUseCase authUseCase,
            @Value("${app.root-username}") String rootUsername,
            @Value("${app.root-password}") String rootPassword
    ) {
        return args -> authUseCase.ensureRootAdmin(rootUsername, rootPassword);
    }

    @Bean
    public WebMvcConfigurer uploadsResourceConfigurer(FileStorage fileStorage) {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                String location = fileStorage.getRoot().toUri().toString();
                if (!location.endsWith("/")) {
                    location = location + "/";
                }
                registry.addResourceHandler("/uploads/**").addResourceLocations(location);
            }
        };
    }
}
