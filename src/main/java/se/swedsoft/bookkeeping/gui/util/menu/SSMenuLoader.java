package se.swedsoft.bookkeeping.gui.util.menu;


import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Date: 2006-feb-27
 * @version $Id$
 */
public class SSMenuLoader {    private static final Logger LOG = LoggerFactory.getLogger(SSMenuLoader.class);


    private final Map<String, JMenuBar > iMenuBars;
    private final Map<String, JMenu    > iMenus;
    private final Map<String, JMenuItem> iMenuItems;

    private final Map<String, List<ActionListener>> iActions;

    private final Map<String, List<JComponent>> iDependancies;

    /**
     * Default constructor.
     */
    public SSMenuLoader() {
        iMenuBars = new HashMap<>();
        iMenus = new HashMap<>();
        iMenuItems = new HashMap<>();

        iActions = new HashMap<>();
        iDependancies = new HashMap<>();
    }

    /**
     * Loads menus from an XML stream.
     *
     * @param stream the menu XML stream
     */
    public void loadMenus(InputStream stream) {
        iMenuBars.clear();
        iMenus.clear();
        iMenuItems.clear();

        XMLReader iReader;

        try {
            SAXParserFactory iFactory = SAXParserFactory.newInstance();
            SAXParser iParser = iFactory.newSAXParser();
            iReader = iParser.getXMLReader();
        } catch (ParserConfigurationException | SAXException e) {
            LOG.error("Unexpected error", e);
            return;
        }

        iReader.setContentHandler(new MenuBuilder());

        try {
            iReader.parse(new InputSource(stream));
        } catch (SAXException | IOException ex) {
            LOG.error("Unexpected error", ex);
        }
    }

    /**
     * Returns a menubar by name.
     *
     * @param pName the menubar name
     * @return the menubar, or {@code null} if not found
     */
    public JMenuBar getMenuBar(String pName) {
        return iMenuBars.get(pName);
    }

    /**
     * Returns a menu by name.
     *
     * @param pName the menu name
     * @return the menu, or {@code null} if not found
     */
    public JMenu getMenu(String pName) {
        return iMenus.get(pName);
    }

        /**
         * Registers a named action listener.
         *
         * @param pName the action name
         * @param pAction the listener to register
         */
    public void addActionListener(String pName, ActionListener pAction) {
        iActions.computeIfAbsent(pName, key -> new LinkedList<>()).add(pAction);
    }

        /**
         * Registers a dependent component.
         *
         * @param pGroup the dependency group
         * @param iComponent the component to toggle
         */
    public void addDependancy(String pGroup, JComponent iComponent) {
        iDependancies.computeIfAbsent(pGroup, key -> new LinkedList<>()).add(iComponent);
    }

        /**
         * Enables or disables a dependency group.
         *
         * @param pGroup the dependency group
         * @param pEnabled {@code true} to enable, {@code false} to disable
         */
    public void setEnabled(String pGroup, boolean pEnabled) {
        List<JComponent> iComponents = iDependancies.get(pGroup);

        if (iComponents != null) {
            for (JComponent iComponent: iComponents) {
                iComponent.setEnabled(pEnabled);
            }
        }
    }

        /**
         * Notifies registered listeners.
         *
         * @param pName the action name
         * @param pAction the action event
         */
    private void notifyActionListeners(String pName, ActionEvent pAction) {
        List<ActionListener> iListeners = iActions.get(pName);

        if (iListeners != null) {
            for (ActionListener iListener: iListeners) {
                iListener.actionPerformed(pAction);
            }
        } else {
            LOG.info("(SSMenuLoader)No listeners for " + pName);
        }

    }

        /**
         * Creates an action listener for a named action.
         *
         * @param pName the action name
         * @return the listener
         */
    private ActionListener createActionListener(final String pName) {
        return e -> notifyActionListeners(pName, e);
    }

    /**
     *
     */
    private class MenuBuilder extends DefaultHandler {

        private JMenuBar iMenuBar;

        private final Stack<JMenu> iMenuStack;

        /**
         *
         */
        public MenuBuilder() {
            iMenuBar = null;
            iMenuStack = new Stack<>();
        }

        /**
         * Creates and registers a menubar.
         *
         * @param iAttributes SAX attributes for the current element
         */
        private void createMenuBar(Attributes iAttributes) {
            String iName = iAttributes.getValue("Name");

            iMenuBar = new JMenuBar();
            iMenuBar.setName(iName);

            iMenuBars.put(iName, iMenuBar);
        }

        /**
         * Creates and registers a menu.
         *
         * @param iAttributes SAX attributes for the current element
         * @return the created menu
         */
        private JMenu createMenu(Attributes iAttributes) {
            String iName = iAttributes.getValue("Name");
            String iText = iAttributes.getValue("Text");
            String iBundle = iAttributes.getValue("Bundle");
            String iDependent = iAttributes.getValue("Dependent");

            JMenu iMenu = new JMenu();

            iMenu.setName(iName);

            if (iBundle == null) {
                iMenu.setText(iText);
            } else {
                SSMenuUtils.setupMenu(iMenu, iBundle);
            }

            if (iDependent != null) {
                addDependancy(iDependent, iMenu);
            }

            add(iMenu);

            iMenus.put(iName, iMenu);

            return iMenu;
        }

        /**
         * Creates and registers a menu item.
         *
         * @param iAttributes SAX attributes for the current element
         */
        private void createMenuItem(Attributes iAttributes) {
            String iName = iAttributes.getValue("Name");
            String iText = iAttributes.getValue("Text");
            String iAction = iAttributes.getValue("Action");
            String iBundle = iAttributes.getValue("Bundle");
            String iDependent = iAttributes.getValue("Dependent");

            JMenuItem iMenuItem = new JMenuItem();

            iMenuItem.setName(iName);
            iMenuItem.addActionListener(createActionListener(iAction));

            if (iBundle == null) {
                iMenuItem.setText(iText);
            } else {
                SSMenuUtils.setupMenuItem(iMenuItem, iBundle);
            }

            if (iDependent != null) {
                addDependancy(iDependent, iMenuItem);
            }

            add(iMenuItem);

            iMenuItems.put(iName, iMenuItem);
        }

        private void createSeparator() {
            if (!iMenuStack.isEmpty()) {
                JMenu iParent = iMenuStack.peek();

                iParent.addSeparator();
            }
        }

        /**
         * Adds a menu component either to the current menu or to the menubar root.
         *
         * @param iComponent the component to add
         */
        private void add(JComponent iComponent) {
            if (!iMenuStack.isEmpty()) {
                JMenu iParent = iMenuStack.peek();

                iParent.add(iComponent);
            } else {
                if (iMenuBar != null) {
                    iMenuBar.add(iComponent);
                }
            }
        }

        private String getElementName(String localName, String qName) {
            return (localName != null && !localName.isEmpty()) ? localName : qName;
        }

        /**
         * Handles the start of an element while building menu structures.
         *
         * @param uri the namespace URI
         * @param localName the local element name
         * @param qName the qualified element name
         * @param iAttributes SAX attributes for the current element
         */
        @Override
        public void startElement(String uri, String localName, String qName, Attributes iAttributes) {
            String iElementName = getElementName(localName, qName);

            if ("MenuBar".equalsIgnoreCase(iElementName)) {
                createMenuBar(iAttributes);
            }

            if ("Menu".equalsIgnoreCase(iElementName)) {
                JMenu iMenu = createMenu(iAttributes);

                iMenuStack.push(iMenu);
            }

            if ("MenuItem".equalsIgnoreCase(iElementName)) {
                createMenuItem(iAttributes);
            }

            if ("Separator".equalsIgnoreCase(iElementName)) {
                createSeparator();

            }

        }

        /**
         * Handles the end of an element while building menu structures.
         *
         * @param uri the namespace URI
         * @param localName the local element name
         * @param qName the qualified element name
         */
        @Override
        public void endElement(String uri, String localName, String qName) {
            String iElementName = getElementName(localName, qName);

            if ("MenuBar".equalsIgnoreCase(iElementName)) {
                iMenuBar = null;
            }

            if ("Menu".equalsIgnoreCase(iElementName)) {
                iMenuStack.pop();
            }

        }

        @Override
        public String toString() {
            return "se.swedsoft.bookkeeping.gui.util.menu.SSMenuLoader.MenuBuilder"
                    + "{iMenuBar=" + iMenuBar
                    + ", iMenuStack=" + iMenuStack
                    + '}';
        }
    }

    @Override
    public String toString() {
        return "se.swedsoft.bookkeeping.gui.util.menu.SSMenuLoader"
                + "{iActions=" + iActions
                + ", iDependancies=" + iDependancies
                + ", iMenuBars=" + iMenuBars
                + ", iMenuItems=" + iMenuItems
                + ", iMenus=" + iMenus
                + '}';
    }
}
