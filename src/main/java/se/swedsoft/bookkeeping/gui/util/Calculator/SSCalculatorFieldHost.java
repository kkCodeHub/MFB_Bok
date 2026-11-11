package se.swedsoft.bookkeeping.gui.util.Calculator;

import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.util.table.editors.SSBigDecimalCellEditor;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Small reusable host for opening the shared calculator from any numeric field.
 */
public class SSCalculatorFieldHost {

    private final Component iOwner;
    private JFormattedTextField iTargetField;

    public SSCalculatorFieldHost(Component iOwner) {
        this.iOwner = iOwner;
    }

    public void bind(JFormattedTextField iTargetField) {
        this.iTargetField = iTargetField;
    }

    public JFormattedTextField getTargetField() {
        return iTargetField;
    }

    public void openForCurrentField() {
        if (iTargetField == null) {
            return;
        }
        openFor(iTargetField, iOwner);
    }

    public static void attachToEditor(SSBigDecimalCellEditor editor, Component owner) {
        editor.setOnEditingDoubleClick(field -> {
            SSCalculatorFieldHost host = new SSCalculatorFieldHost(owner);
            host.bind(field);
            host.openForCurrentField();
        });
        editor.setToolTipText("Skift + dubbelklick för calculator");
    }

    public static void openFor(JFormattedTextField iTargetField, Component iOwner) {
        if (iTargetField == null) {
            return;
        }

        String initialValue = getInitialValue(iTargetField);

        SwingCalculator.showDialog(SSMainFrame.getInstance(), iOwner, initialValue, result -> {
            try {
                BigDecimal value = new BigDecimal(result.replace(',', '.'));
                iTargetField.setValue(value);
                iTargetField.requestFocusInWindow();
                iTargetField.selectAll();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                        iTargetField,
                        "Ogiltigt värde från kalkylatorn: " + result,
                        "Kalkylatorfel",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static String getInitialValue(JFormattedTextField iTargetField) {
        Object currentValue = iTargetField.getValue();
        if (currentValue instanceof Number) {
            return new BigDecimal(currentValue.toString())
                    .setScale(2, RoundingMode.HALF_UP)
                    .toPlainString();
        }

        if (iTargetField.getText() != null) {
            String textValue = iTargetField.getText().trim().replace(",", ".");
            try {
                return new BigDecimal(textValue)
                        .setScale(2, RoundingMode.HALF_UP)
                        .toPlainString();
            } catch (NumberFormatException ex) {
                return textValue;
            }
        }

        return "";
    }
}
