package org.delcom.ifs25009springbootcashflow.framework.view;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Locale;

/**
 * Cache jangka panjang untuk aset statis agar kunjungan ulang cepat (audit "efficient cache policy").
 *
 * <p>Header dikunci lewat pembungkus response: servlet Vaadin/Spring yang menulis {@code Cache-Control}
 * atau {@code Expires} sesudah filter ini tidak bisa lagi menimpanya.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StaticCacheFilter implements Filter {

    static final String IMMUTABLE = "public, max-age=31536000, immutable";
    static final String LONG = "public, max-age=2592000";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest req && response instanceof HttpServletResponse res) {
            String policy = policyFor(req.getRequestURI(), req.getQueryString());
            if (policy != null) {
                res.setHeader("Cache-Control", policy);
                chain.doFilter(request, new LockedCacheResponse(res));
                return;
            }
        }
        chain.doFilter(request, response);
    }

    /** Menentukan kebijakan cache; {@code null} berarti biarkan default (halaman dinamis, API, upload). */
    static String policyFor(String path, String query) {
        if (path.startsWith("/VAADIN/build/")) {
            return IMMUTABLE;
        }
        boolean versioned = query != null && query.contains("v-c=");
        if (versioned && (path.equals("/styles.css") || path.startsWith("/lumo/"))) {
            return IMMUTABLE;
        }
        if (path.startsWith("/icons/") || path.equals("/styles.css") || path.startsWith("/lumo/")) {
            return LONG;
        }
        return null;
    }

    /** Mengabaikan penulisan ulang header cache oleh komponen lain di dalam rantai filter. */
    private static final class LockedCacheResponse extends HttpServletResponseWrapper {

        LockedCacheResponse(HttpServletResponse response) {
            super(response);
        }

        private static boolean locked(String name) {
            if (name == null) {
                return false;
            }
            String n = name.toLowerCase(Locale.ROOT);
            return n.equals("cache-control") || n.equals("expires") || n.equals("pragma");
        }

        @Override
        public void setHeader(String name, String value) {
            if (!locked(name)) {
                super.setHeader(name, value);
            }
        }

        @Override
        public void addHeader(String name, String value) {
            if (!locked(name)) {
                super.addHeader(name, value);
            }
        }

        @Override
        public void setDateHeader(String name, long date) {
            if (!locked(name)) {
                super.setDateHeader(name, date);
            }
        }

        @Override
        public void addDateHeader(String name, long date) {
            if (!locked(name)) {
                super.addDateHeader(name, date);
            }
        }

        @Override
        public void setIntHeader(String name, int value) {
            if (!locked(name)) {
                super.setIntHeader(name, value);
            }
        }

        @Override
        public void addIntHeader(String name, int value) {
            if (!locked(name)) {
                super.addIntHeader(name, value);
            }
        }
    }
}
