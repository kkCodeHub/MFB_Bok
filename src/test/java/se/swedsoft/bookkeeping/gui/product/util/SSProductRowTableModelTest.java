package se.swedsoft.bookkeeping.gui.product.util;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSProduct;
import se.swedsoft.bookkeeping.data.SSProductRow;

import javax.swing.SwingUtilities;
import java.math.BigDecimal;
import java.util.LinkedList;

import static org.assertj.core.api.Assertions.assertThat;

class SSProductRowTableModelTest {

    @Test
    void shouldRejectDecimalQuantityForWholeQuantityComponent() {
        SSProduct component = product("COMP-WHOLE", true);
        SSProductRow row = row(component, null, 10);
        SSProductRowTableModel model = modelWith(row);

        model.setValueAt(new BigDecimal("1.5"), 0, 2);

        assertThat(row.getQuantity()).isEqualTo(10);
    }

    @Test
    void shouldAllowDecimalQuantityForNonWholeQuantityComponent() {
        SSProduct component = product("COMP-DECIMAL", false);
        SSProductRow row = row(component, null, 10);
        SSProductRowTableModel model = modelWith(row);

        model.setValueAt(new BigDecimal("1.5"), 0, 2);

        assertThat(row.getQuantity()).isEqualTo(15);
    }

    @Test
    void shouldKeepOtherFieldsUnchangedWhenDecimalQuantityIsRejected() {
        SSProduct component = product("COMP-WHOLE-STABLE", true);
        SSProductRow row = row(component, "Original description", 10);
        SSProductRowTableModel model = modelWith(row);

        model.setValueAt(new BigDecimal("1.5"), 0, 2);

        assertThat(row.getQuantity()).isEqualTo(10);
        assertThat(row.getDescription()).isEqualTo("Original description");
        assertThat(row.getProduct()).isSameAs(component);
    }

    @Test
    void shouldAllowDecimalQuantityWhenComponentIsNotSelected() {
        SSProductRow row = row(null, "No product selected", 10);
        SSProductRowTableModel model = modelWith(row);

        model.setValueAt(new BigDecimal("1.5"), 0, 2);

        assertThat(row.getQuantity()).isEqualTo(15);
        assertThat(row.getDescription()).isEqualTo("No product selected");
        assertThat(row.getProduct()).isNull();
    }

    @Test
    void shouldIgnoreEditsInReadOnlyUnitColumn() {
        SSProduct component = product("COMP-READONLY-UNIT", false);
        SSProductRow row = row(component, "Stable", 10);
        SSProductRowTableModel model = modelWith(row);

        model.setValueAt("IGNORED", 0, 3);

        assertThat(row.getQuantity()).isEqualTo(10);
        assertThat(row.getDescription()).isEqualTo("Stable");
        assertThat(row.getProduct()).isSameAs(component);
    }

    @Test
    void shouldSupportQuantityEditOnEdt() throws Exception {
        SSProduct component = product("COMP-EDT", false);
        SSProductRow row = row(component, "EDT row", 10);
        SSProductRowTableModel model = modelWith(row);

        SwingUtilities.invokeAndWait(() -> {
            assertThat(SwingUtilities.isEventDispatchThread()).isTrue();
            model.setValueAt(new BigDecimal("1.5"), 0, 2);
        });

        assertThat(row.getQuantity()).isEqualTo(15);
        assertThat(row.getDescription()).isEqualTo("EDT row");
        assertThat(row.getProduct()).isSameAs(component);
    }

    private static SSProductRowTableModel modelWith(SSProductRow row) {
        SSProductRowTableModel model = new SSProductRowTableModel(new LinkedList<>());
        model.add(row);
        return model;
    }

    private static SSProduct product(String number, boolean wholeOnly) {
        SSProduct component = new SSProduct();
        component.setNumber(number);
        component.setOnlyWholeQuantity(wholeOnly);
        return component;
    }

    private static SSProductRow row(SSProduct product, String description, int quantityTenths) {
        SSProductRow row = new SSProductRow();
        row.setProduct(product);
        row.setDescription(description);
        row.setQuantity(quantityTenths);
        return row;
    }
}
