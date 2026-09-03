package se.swedsoft.bookkeeping.gui.util.datechooser;

import javax.swing.*;
import javax.swing.plaf.SpinnerUI;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicSpinnerUI;
import java.awt.*;
import java.util.Objects;

/**
 * Spinner with selectable UI mode.
 */
public class JSpinnerX extends JSpinner {

    /**
     * Supported UI modes.
     */
    public enum UiMode {
        STANDARD,
        HORIZONTAL_BUTTONS
    }

    private UiMode iUiMode = UiMode.STANDARD;

    /**
     * Creates a spinner with the default model and UI mode.
     */
    public JSpinnerX() {
        super();
    }

    /**
     * Creates a spinner with a custom model and default UI mode.
     *
     * @param model the spinner model
     */
 //   public JSpinnerX(SpinnerModel model) {
 //       super(model);
 //   }

    /**
     * Sets the UI mode.
     *
     * @param uiMode the UI mode
     */
    public void setUiMode(UiMode uiMode) {
        iUiMode = Objects.requireNonNull(uiMode, "uiMode");
        applyUiMode();
    }

    /**
     * @return the current UI mode
     */
 //   public UiMode getUiMode() {
 //       return iUiMode;
 //   }

    @Override
    public void updateUI() {
        super.updateUI();
        if (iUiMode == UiMode.HORIZONTAL_BUTTONS) {
            setUI(new SSHorizontalSpinnerUI());
        }
    }

    private void applyUiMode() {
        if (iUiMode == UiMode.HORIZONTAL_BUTTONS) {
            setUI(new SSHorizontalSpinnerUI());
        } else {
            setUI((SpinnerUI) UIManager.getUI(this));
        }
    }

    private static final class SSHorizontalSpinnerUI extends BasicSpinnerUI {

        @Override
        protected Component createNextButton() {
            Component iButton = super.createNextButton();
            if (iButton instanceof BasicArrowButton) {
                ((BasicArrowButton) iButton).setDirection(SwingConstants.NORTH);
            }
            return iButton;
        }

        @Override
        protected Component createPreviousButton() {
            Component iButton = super.createPreviousButton();
            if (iButton instanceof BasicArrowButton) {
                ((BasicArrowButton) iButton).setDirection(SwingConstants.SOUTH);
            }
            return iButton;
        }

        @Override
        protected LayoutManager createLayout() {
            return new SSHorizontalSpinnerLayout();
        }
    }

    private static final class SSHorizontalSpinnerLayout implements LayoutManager {
        private Component iEditor;
        private Component iNext;
        private Component iPrevious;

        @Override
        public void addLayoutComponent(String name, Component comp) {
            if ("Editor".equals(name)) {
                iEditor = comp;
            } else if ("Next".equals(name)) {
                iNext = comp;
            } else if ("Previous".equals(name)) {
                iPrevious = comp;
            }
        }

        @Override
        public void removeLayoutComponent(Component comp) {
            if (comp == iEditor) {
                iEditor = null;
            } else if (comp == iNext) {
                iNext = null;
            } else if (comp == iPrevious) {
                iPrevious = null;
            }
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            synchronized (parent.getTreeLock()) {
                Insets iInsets = parent.getInsets();
                Dimension iEditorSize = iEditor != null ? iEditor.getPreferredSize() : new Dimension(0, 0);
                Dimension iPreviousSize = iPrevious != null ? iPrevious.getPreferredSize() : new Dimension(0, 0);
                Dimension iNextSize = iNext != null ? iNext.getPreferredSize() : new Dimension(0, 0);

                int iButtonWidth = iPreviousSize.width + iNextSize.width;
                int iHeight = Math.max(iEditorSize.height, Math.max(iPreviousSize.height, iNextSize.height));

                return new Dimension(
                        iInsets.left + iInsets.right + iEditorSize.width + iButtonWidth,
                        iInsets.top + iInsets.bottom + iHeight);
            }
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            synchronized (parent.getTreeLock()) {
                Insets iInsets = parent.getInsets();
                Dimension iEditorSize = iEditor != null ? iEditor.getMinimumSize() : new Dimension(0, 0);
                Dimension iPreviousSize = iPrevious != null ? iPrevious.getMinimumSize() : new Dimension(0, 0);
                Dimension iNextSize = iNext != null ? iNext.getMinimumSize() : new Dimension(0, 0);

                int iButtonWidth = iPreviousSize.width + iNextSize.width;
                int iHeight = Math.max(iEditorSize.height, Math.max(iPreviousSize.height, iNextSize.height));

                return new Dimension(
                        iInsets.left + iInsets.right + iEditorSize.width + iButtonWidth,
                        iInsets.top + iInsets.bottom + iHeight);
            }
        }

        @Override
        public void layoutContainer(Container parent) {
            synchronized (parent.getTreeLock()) {
                Insets iInsets = parent.getInsets();
                int iX = iInsets.left;
                int iY = iInsets.top;
                int iWidth = parent.getWidth() - iInsets.left - iInsets.right;
                int iHeight = parent.getHeight() - iInsets.top - iInsets.bottom;

                int iPreviousWidth = iPrevious != null ? iPrevious.getPreferredSize().width : 0;
                int iNextWidth = iNext != null ? iNext.getPreferredSize().width : 0;
                int iButtonsWidth = iPreviousWidth + iNextWidth;
                int iEditorWidth = Math.max(0, iWidth - iButtonsWidth);

                if (iEditor != null) {
                    iEditor.setBounds(iX, iY, iEditorWidth, iHeight);
                }

                int iButtonX = iX + iEditorWidth;

                if (iPrevious != null) {
                    iPrevious.setBounds(iButtonX, iY, iPreviousWidth, iHeight);
                    iButtonX += iPreviousWidth;
                }

                if (iNext != null) {
                    iNext.setBounds(iButtonX, iY, iNextWidth, iHeight);
                }
            }
        }
    }
}
