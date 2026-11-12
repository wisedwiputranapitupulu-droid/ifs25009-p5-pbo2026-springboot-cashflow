package org.delcom.ifs25009springbootcashflow.framework.view;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Cache jangka panjang untuk aset statis (bundle Vaadin ber-hash, ikon, CSS) agar kunjungan ulang cepat. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StaticCacheFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest req && response instanceof HttpServletResponse res) {
            String path = req.getRequestURI();
            if (path.startsWith("/VAADIN/build/")) {
                res.setHeader("Cache-Control", "public, max-age=31536000, immutable");
            } else if (path.startsWith("/icons/") || path.equals("/styles.css") || path.startsWith("/lumo/")) {
                res.setHeader("Cache-Control", "public, max-age=604800");
            }
        }
        chain.doFilter(request, response);
    }
}
