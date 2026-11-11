package se.swedsoft.bookkeeping.gui.util.Calculator;

import com.ezylang.evalex.Expression;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSDialog;

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
        super(iOwner, "Miniräknare");
        this.resultCallback = resultCallback;
        add(calculatorPanel, BorderLayout.CENTER);
        configureUi();
        bindButtonActions();
    }

    private void configureUi() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        display.setFont(new Font("Arial", Font.PLAIN, 24));

        JButton[] buttons = {
                btn7, btn8, btn9, btnDivide,
                btn4, btn5, btn6, btnMultiply,
                btn1, btn2, btn3, btnMinus,
                btn0, btnDot, btnEquals, btnPlus,
                btnOpen, btnClose, btnPercent, btnClear, btnBackspace, btnCancel
        };
        Font buttonFont = new Font("Arial", Font.BOLD, 20);
        for (JButton button : buttons) {
            button.setFont(buttonFont);
        }

        getRootPane().setDefaultButton(btnEquals);

        pack();
        setSize(360, 480);
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
            default:
                display.setText(display.getText() + value);
                break;
        }
    }

    // Procentknappen gör om det sista talet till tal / 100.
    // Exempel: 200 * 15 % blir 200 * (15/100).
//    private void applyPercent() {
//        String s = display.getText();
//        if (s.isEmpty()) return;
//
//        int end = s.length();
//        int start = end;
//
//        if (Character.isDigit(s.charAt(end - 1)) || s.charAt(end - 1) == '.') {
//            while (start > 0) {
//                char c = s.charAt(start - 1);
//                if (Character.isDigit(c) || c == '.') start--;
//                else break;
//            }
//            if (start > 0 && s.charAt(start - 1) == '-'
//                    && (start == 1 || "+-*/(".indexOf(s.charAt(start - 2)) >= 0)) {
//                start--;
//            }
//        } else {
//            return;
//        }
//
//        String lastNumber = s.substring(start, end);
//        display.setText(s.substring(0, start) + "(" + lastNumber + "/100)");
//    }
    private void applyPercent() {

        String text = display.getText().trim();

        if (text.isEmpty()) {
            return;
        }

        int end = text.length();
        int start = end;

        // Hitta början på det sista talet
        if (Character.isDigit(text.charAt(end - 1))
            || text.charAt(end - 1) == '.') {

            while (start > 0) {
                char c = text.charAt(start - 1);

                if (Character.isDigit(c) || c == '.') {
                    start--;
                } else {
                    break;
                }
            }

        } else {
            return;
        }

        String percentValue = text.substring(start, end);

        // Hitta föregående operator
        int operatorPos = start - 1;

        if (operatorPos < 0) {
            display.setText("(" + percentValue + "/100)");
            return;
        }

        char operator = text.charAt(operatorPos);

        // Multiplikation och division:
        // Procenten räknas som tal / 100.
        if (operator == '*' || operator == '/') {

            display.setText(
                text.substring(0, start)
                    + "(" + percentValue + "/100)"
            );

            return;
        }

        // Addition och subtraktion:
        // Procenten beräknas på föregående tal.
        if (operator == '+' || operator == '-') {

            String leftPart = text.substring(0, operatorPos).trim();

            try {

                BigDecimal previousValue =
                    new Expression(leftPart)
                        .evaluate()
                        .getNumberValue();

                BigDecimal percentage =
                    new BigDecimal(percentValue);

                BigDecimal result =
                    previousValue.multiply(percentage)
                        .divide(new BigDecimal("100"));

                display.setText(
                    text.substring(0, start)
                        + "(" + result.toPlainString() + ")"
                );

            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                    this,
                    "Kunde inte beräkna procenten.",
                    "Fel",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void calculate() {
        String input = display.getText().trim().replace(',', '.');
        if (input.isEmpty()) return;

        try {
            BigDecimal result = new Expression(input).evaluate().getNumberValue();
            String formattedResult = result.stripTrailingZeros().toPlainString();
            display.setText(formattedResult);
            if (resultCallback != null) {
                resultCallback.accept(formattedResult);
                closeDialog(JOptionPane.OK_OPTION);
            }
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
