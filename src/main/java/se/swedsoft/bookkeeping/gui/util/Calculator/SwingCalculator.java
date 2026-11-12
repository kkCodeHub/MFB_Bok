package se.swedsoft.bookkeeping.gui.util.Calculator;

import com.ezylang.evalex.Expression;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSDialog;
import se.swedsoft.bookkeeping.gui.util.graphics.SSImage;

import java.awt.BorderLayout;
import java.awt.Font;
import java.math.BigDecimal;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Swing-miniräknare med IntelliJ GUI Designer-formulär.
 */
public class SwingCalculator extends SSDialog {
    private final Consumer<String> resultCallback;

    public SwingCalculator(final SSMainFrame iOwner) {
        this(iOwner, null);
    }

    public SwingCalculator(final SSMainFrame iOwner, final Consumer<String> resultCallback) {
        super(iOwner, "Kalkylator");
        this.resultCallback = resultCallback;
        this.setIconImages(SSImage.getApplicationIcons());
        add(calculatorPanel, BorderLayout.CENTER);
        configureUi();
        bindButtonActions();
    }

    private void configureUi() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        display.setFont(new Font("Arial", Font.PLAIN, 18));

        JButton[] buttons = {
                btn7, btn8, btn9, btnDivide,
                btn4, btn5, btn6, btnMultiply,
                btn1, btn2, btn3, btnMinus,
                btn0, btnDot, btnEquals, btnPlus,
                btnOpen, btnClose, btnPercent, btnClear, btnBackspace, btnCancel
        };
        Font buttonFont = new Font("Arial", Font.BOLD, 14);
        for (JButton button : buttons) {
            button.setFont(buttonFont);
        }

        getRootPane().setDefaultButton(btnEquals);

        pack();
        setSize(260, 340);
        setLocationRelativeTo((java.awt.Component) null);
    }

    private void bindButtonActions() {
        JButton[] buttons = {
                btn7, btn8, btn9, btnDivide,
                btn4, btn5, btn6, btnMultiply,
                btn1, btn2, btn3, btnMinus,
                btn0, btnDot, btnEquals, btnPlus,
                btnOpen, btnClose, btnPercent, btnClear, btnBackspace, btnCancel
        };

        for (JButton button : buttons) {
            button.addActionListener(evt -> handleButton(((JButton) evt.getSource()).getText()));
        }
    }

    private void handleButton(String value) {
        switch (value) {
            case "C":
                display.setText("");
                break;
            case "=":
                calculate();
                break;
            case "%":
                applyPercent();
                break;
            case "<X":
                String currentText = display.getText();
                if (!currentText.isEmpty()) {
                    display.setText(currentText.substring(0, currentText.length() - 1));
                }
                break;
            case "Cancel":
                closeDialog(JOptionPane.CANCEL_OPTION);
                break;
            case "─":
                display.setText(display.getText() + "-");
                break;
            default:
                display.setText(display.getText() + value);
                break;
        }
    }

    private void applyPercent() {
        String text = display.getText().trim();

        if (text.isEmpty() || text.endsWith("%")) {
            return;
        }

        int end = text.length();
        int start = end;

        while (start > 0) {
            char c = text.charAt(start - 1);
            if (Character.isDigit(c) || c == '.') {
                start--;
            } else {
                break;
            }
        }

        if (start == end) {
            return;
        }

        display.setText(text + "%");
    }

    private String normalizePercentExpression(String input) {
        String expression = input.trim().replace(" ", "");
        if (expression.isEmpty() || !expression.contains("%")) {
            return expression;
        }

        int percentIndex = expression.lastIndexOf('%');
        if (percentIndex != expression.length() - 1) {
            throw new IllegalArgumentException("Unsupported percent usage");
        }

        int start = percentIndex;
        while (start > 0) {
            char c = expression.charAt(start - 1);
            if (Character.isDigit(c) || c == '.') {
                start--;
            } else {
                break;
            }
        }

        if (start == percentIndex) {
            throw new IllegalArgumentException("Unsupported percent usage");
        }

        String percentageValue = expression.substring(start, percentIndex);
        if (percentageValue.isEmpty()) {
            throw new IllegalArgumentException("Unsupported percent usage");
        }

        if (start == 0) {
            return percentageValue + "/100";
        }

        char operator = expression.charAt(start - 1);
        String leftPart = expression.substring(0, start - 1).trim();
        if (leftPart.isEmpty()) {
            throw new IllegalArgumentException("Unsupported percent usage");
        }

        if (operator == '*') {
            return leftPart + "*(" + percentageValue + "/100)";
        }

        if (operator == '/') {
            return leftPart + "/(" + percentageValue + "/100)";
        }

        if (operator == '+') {
            return leftPart + "+(" + leftPart + "*" + percentageValue + "/100)";
        }

        if (operator == '-') {
            return leftPart + "-(" + leftPart + "*" + percentageValue + "/100)";
        }

        throw new IllegalArgumentException("Unsupported percent usage");
    }

    private void calculate() {
        String input = display.getText().trim().replace(',', '.');
        if (input.isEmpty()) return;

        try {
            String normalizedInput = normalizePercentExpression(input);
            BigDecimal result = new Expression(normalizedInput).evaluate().getNumberValue();
            String formattedResult = result.stripTrailingZeros().toPlainString();
            display.setText(formattedResult);
            if (resultCallback != null) {
                resultCallback.accept(formattedResult);
                closeDialog(JOptionPane.OK_OPTION);
            }
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this,
                "Uttrycket kunde inte beräknas.\nKontrollera uttrycket och försök igen.",
                "Beräkningsfel", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Uttrycket kunde inte beräknas.\nKontrollera uttrycket och försök igen.",
                "Beräkningsfel", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void showDialog(
            final SSMainFrame iMainFrame,
            final java.awt.Component iRelativeTo,
            final String iInitialValue,
            final Consumer<String> iResultCallback) {
        SwingCalculator iDialog = new SwingCalculator(iMainFrame, iResultCallback);
        if (iInitialValue != null && !iInitialValue.trim().isEmpty()) {
            iDialog.display.setText(iInitialValue.trim());
        }
        if (iRelativeTo != null) {
            iDialog.setLocationRelativeTo(iRelativeTo);
        } else {
            iDialog.setLocationRelativeTo(iMainFrame);
        }
        iDialog.showDialog();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SwingCalculator(null).showDialog());
    }

    // Variables declaration - do not modify
    private javax.swing.JPanel calculatorPanel;
    private javax.swing.JTextField display;
    private JButton btn0, btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9;
    private JButton btnDivide, btnMultiply, btnMinus, btnPlus;
    private JButton btnDot, btnEquals, btnOpen, btnClose, btnPercent, btnClear, btnBackspace,
            btnCancel;
    // End of variables declaration
}
