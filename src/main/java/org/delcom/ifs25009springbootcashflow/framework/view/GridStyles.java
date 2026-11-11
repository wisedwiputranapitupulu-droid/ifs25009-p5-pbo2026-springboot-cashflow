package org.delcom.ifs25009springbootcashflow.framework.view;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;

/** Styling grid: header kontras, stripe, dan wrap sel. */
final class GridStyles {

    private GridStyles() {
    }

    static void apply(Grid<?> grid) {
        grid.addClassName("app-grid");
        grid.addThemeVariants(
                GridVariant.LUMO_ROW_STRIPES,
                GridVariant.LUMO_COLUMN_BORDERS,
                GridVariant.LUMO_WRAP_CELL_CONTENT
        );
    }
}
