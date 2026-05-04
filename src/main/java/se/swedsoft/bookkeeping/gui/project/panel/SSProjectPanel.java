/*
 * 2005-2010
 * $Id$
 */
package se.swedsoft.bookkeeping.gui.project.panel;


import se.swedsoft.bookkeeping.data.SSNewProject;
import se.swedsoft.bookkeeping.gui.util.datechooser.SSDateChooser;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import javax.swing.*;
import java.awt.event.*;
/**
 * @author
 */
public class SSProjectPanel {

    private SSNewProject iProject;

    protected JPanel iPanel;

    private JButton iOkButton;

    private JButton iCancelButton;

    protected JTextArea iDescription;

    protected JTextField iName;

    protected JFormattedTextField iNumber;

    protected JCheckBox iConcluded;

    protected SSDateChooser iConcludedDate;

    /**
     * Default constructor.
     * @param iEdit
     */
    public SSProjectPanel(boolean iEdit) {
        iNumber.setEnabled(!iEdit);
        iNumber.setValue("");

        iConcluded.addItemListener(e -> iConcludedDate.setEnabled(iConcluded.isSelected()));

        SwingUtilities.invokeLater(() -> {

                if (iNumber.isEnabled()) {
                    iNumber.requestFocusInWindow();
                } else {
                    iName.requestFocusInWindow();
                }


            });

        iNumber.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iName.requestFocusInWindow());
                }
            }
        });

        iName.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iDescription.requestFocusInWindow());
                }
            }
        });

        iDescription.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iConcluded.requestFocusInWindow());
                }
            }
        });

        iConcluded.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(
                            () -> {

                                    if (iConcluded.isSelected()) {
                                        iConcludedDate.getEditor().getComponent(0).requestFocusInWindow();
                                    } else {
                                        iOkButton.requestFocusInWindow();
                                    }

                                });
                }
            }
        });

        iConcludedDate.getEditor().getComponent(0).addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iOkButton.requestFocusInWindow());
                }
            }
        });

        iOkButton.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    SwingUtilities.invokeLater(() -> iCancelButton.requestFocusInWindow());
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> {

                            for (ActionListener al : iOkButton.getActionListeners()) {
                                al.actionPerformed(null);
                            }

                        });
                }
            }
        });

        iCancelButton.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    SwingUtilities.invokeLater(() -> iOkButton.requestFocusInWindow());
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> {

                            for (ActionListener al : iCancelButton.getActionListeners()) {
                                al.actionPerformed(null);
                            }

                        });
                }
            }
        });
    }

    /**
     *
     * @return
     */
    public JPanel getPanel() {
        return iPanel;
    }

    /**
     *
     * @param pActionListener
     */

    public void addOkAction(ActionListener pActionListener) {
        iOkButton.addActionListener(pActionListener);
    }

    /**
     *
     * @param pActionListener
     */
    public void addCancelAction(ActionListener pActionListener) {
        iCancelButton.addActionListener(pActionListener);
    }

    /**
     *
     * @param iProject
     */
    public void setProject(SSNewProject iProject) {
        this.iProject = iProject;

        iNumber.setValue(iProject.getNumber());
        iName.setText(iProject.getName());
        iDescription.setText(iProject.getDescription());
        iConcluded.setSelected(iProject.getConcluded());
        iConcludedDate.setLocalDate(
                iProject.getLocalConcludedDate() != null
                        ? iProject.getLocalConcludedDate()
                        : SSDateUtil.today());
    }

    /**
     *
     * @return
     */
    public SSNewProject getProject() {
        iProject.setNumber(iNumber.getText());
        iProject.setName(iName.getText());
        iProject.setDescription(iDescription.getText());
        iProject.setConcluded(iConcluded.isSelected());
        iProject.setLocalConcludedDate(iConcludedDate.getLocalDate());

        return iProject;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.gui.project.panel.SSProjectPanel");
        sb.append("{iCancelButton=").append(iCancelButton);
        sb.append(", iConcluded=").append(iConcluded);
        sb.append(", iConcludedDate=").append(iConcludedDate);
        sb.append(", iDescription=").append(iDescription);
        sb.append(", iName=").append(iName);
        sb.append(", iNumber=").append(iNumber);
        sb.append(", iOkButton=").append(iOkButton);
        sb.append(", iPanel=").append(iPanel);
        sb.append(", iProject=").append(iProject);
        sb.append('}');
        return sb.toString();
    }
}
