package org.delcom.ifs25009springbootcashflow.framework.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/** Kartu konten halaman agar tampilan konsisten. */
final class PageCard {

    private PageCard() {
    }

    static Div wrap(Component... content) {
        VerticalLayout body = new VerticalLayout(content);
        body.setPadding(false);
        body.setSpacing(true);
        body.setSizeFull();

        Div card = new Div(body);
        card.setWidthFull();
        card.setHeightFull();
        card.getStyle()
                .set("background", "#FFFFFF")
                .set("border", "1px solid #E5E7EB")
                .set("border-radius", "12px")
                .set("padding", "1.5rem")
                .set("box-shadow", "0 8px 24px rgba(15, 23, 42, 0.06)")
                .set("box-sizing", "border-box");
        card.getElement().setAttribute("role", "main");
        return card;
    }
}
