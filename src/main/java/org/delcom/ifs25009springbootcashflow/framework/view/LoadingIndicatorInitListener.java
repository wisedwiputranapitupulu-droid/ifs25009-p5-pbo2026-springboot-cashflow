package org.delcom.ifs25009springbootcashflow.framework.view;

import com.vaadin.flow.component.page.LoadingIndicatorConfiguration;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.stereotype.Component;

/** Konfigurasi indikator loading global dengan delay responsif. */
@Component
public class LoadingIndicatorInitListener implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiEvent -> {
            LoadingIndicatorConfiguration conf = uiEvent.getUI().getLoadingIndicatorConfiguration();
            conf.setApplyDefaultTheme(true);
            conf.setFirstDelay(150);
            conf.setSecondDelay(600);
            conf.setThirdDelay(1500);
            uiEvent.getUI().getPage().setTitle("Cash Flow");
            // Atribut lang pada <html> (aksesibilitas & SEO).
            uiEvent.getUI().getPage().executeJs("document.documentElement.lang = 'id';");
        });
    }
}
