package se.swedsoft.bookkeeping.data.system;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


class SSDBEventBus {

    private final Map<String, List<PropertyChangeListener>> iListenerMap = new HashMap<>();

    void addPropertyChangeListener(String pProperty, PropertyChangeListener pPropertyChangeListener) {
        List<PropertyChangeListener> iPropertyChangeListeners = iListenerMap.get(pProperty);

        if (iPropertyChangeListeners == null) {
            iPropertyChangeListeners = new LinkedList<>();
            iListenerMap.put(pProperty, iPropertyChangeListeners);
        }

        iPropertyChangeListeners.add(pPropertyChangeListener);
    }

    void notifyListeners(Object pSource, String pProperty, Object pNewValue, Object pOldValue) {
        List<PropertyChangeListener> iPropertyChangeListeners = iListenerMap.get(pProperty);

        if (iPropertyChangeListeners == null) {
            return;
        }

        PropertyChangeEvent iPropertyChangeEvent = new PropertyChangeEvent(pSource, pProperty,
                pOldValue, pNewValue);

        for (PropertyChangeListener iPropertyChangeListener : iPropertyChangeListeners) {
            iPropertyChangeListener.propertyChange(iPropertyChangeEvent);
        }
    }

    @Override
    public String toString() {
        return iListenerMap.toString();
    }
}
