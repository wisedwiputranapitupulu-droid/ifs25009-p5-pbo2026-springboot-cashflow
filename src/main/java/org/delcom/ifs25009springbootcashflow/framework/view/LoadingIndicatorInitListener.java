package org.delcom.ifs25009springbootcashflow.framework.view;

import com.vaadin.flow.component.page.LoadingIndicatorConfiguration;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/**
 * Konfigurasi global: indikator loading dan dokumen HTML awal.
 *
 * <p>Judul halaman dan atribut {@code lang} ditulis langsung ke HTML yang dikirim server, bukan lewat
 * JavaScript, supaya auditor (Axe/Lighthouse) yang membaca dokumen sebelum Flow selesai merender tetap
 * menemukan {@code <title>} dan bahasa halaman. Stylesheet tema dimuat tanpa memblokir render pertama.
 */
@Component
public class LoadingIndicatorInitListener implements VaadinServiceInitListener {

    static final String DEFAULT_TITLE = "Cash Flow";
    static final String DEFAULT_LANG = "id";

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.addIndexHtmlRequestListener(response -> prepareDocument(response.getDocument()));
        event.getSource().addUIInitListener(uiEvent -> {
            LoadingIndicatorConfiguration conf = uiEvent.getUI().getLoadingIndicatorConfiguration();
            conf.setApplyDefaultTheme(true);
            conf.setFirstDelay(150);
            conf.setSecondDelay(600);
            conf.setThirdDelay(1500);
        });
    }

    /** Melengkapi HTML awal: lang, title, dan stylesheet tema yang tidak memblokir render. */
    static void prepareDocument(Document document) {
        Element html = document.selectFirst("html");
        if (html != null && html.attr("lang").isBlank()) {
            html.attr("lang", DEFAULT_LANG);
        }

        Element head = document.head();
        Element title = head.selectFirst("title");
        if (title == null) {
            head.appendElement("title").text(DEFAULT_TITLE);
        } else if (title.text().isBlank()) {
            title.text(DEFAULT_TITLE);
        }

        for (Element link : head.select("link[rel=stylesheet][href]")) {
            String href = link.attr("href");
            if (href.contains("lumo/lumo.css") || href.contains("styles.css")) {
                // Muat tanpa memblokir render pertama; <noscript> menjaga tampilan bila JS mati.
                link.attr("media", "print").attr("onload", "this.media='all'");
                Element fallback = new Element("noscript");
                fallback.appendElement("link").attr("rel", "stylesheet").attr("href", href);
                link.after(fallback);
            }
        }
    }
}
