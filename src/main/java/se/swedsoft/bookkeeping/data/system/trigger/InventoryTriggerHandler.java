package se.swedsoft.bookkeeping.data.system.trigger;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.swedsoft.bookkeeping.data.SSIndelivery;
import se.swedsoft.bookkeeping.data.SSInventory;
import se.swedsoft.bookkeeping.data.SSOutdelivery;
import se.swedsoft.bookkeeping.data.system.SSTriggerRuntime;
import se.swedsoft.bookkeeping.gui.indelivery.SSIndeliveryFrame;
import se.swedsoft.bookkeeping.gui.inventory.SSInventoryFrame;
import se.swedsoft.bookkeeping.gui.outdelivery.SSOutdeliveryFrame;
import se.swedsoft.bookkeeping.persistence.Repositories;

final class InventoryTriggerHandler implements SSTriggerCategoryHandler {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryTriggerHandler.class);

    private final SSTriggerRuntime iRuntime;

    InventoryTriggerHandler(SSTriggerRuntime pRuntime) {
        iRuntime = pRuntime;
    }

    @Override
    public boolean handle(String iTriggerName, String iTableName, String iNumber) {
        if (iTriggerName == null) {
            return false;
        }
        if (!SSTriggerMatcher.isAny(iTriggerName,
                "NEWINVENTORY", "EDITINVENTORY", "DELETEINVENTORY",
                "NEWINDELIVERY", "EDITINDELIVERY", "DELETEINDELIVERY",
                "NEWOUTDELIVERY", "EDITOUTDELIVERY", "DELETEOUTDELIVERY")) {
            return false;
        }

        if (iTriggerName.equals("NEWINVENTORY")) {
            List<SSInventory> iInventories = iRuntime.getInventories();
            if (iInventories == null) {
                return true;
            }
            SSInventory iInventory = new SSInventory();
            iInventory.setNumber(Integer.parseInt(iNumber));
            Optional<SSInventory> optInventory = Repositories.inventories().findByInventory(iInventory);
            if (optInventory.isEmpty()) {
                LOG.warn("NEWINVENTORY trigger: entity not found for number {}", iNumber);
                return true;
            }
            iInventory = optInventory.get();
            if (!iInventories.contains(iInventory)) {
                iInventories.add(iInventory);
            }
            if (SSInventoryFrame.getInstance() != null) {
                SSInventoryFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (iTriggerName.equals("EDITINVENTORY")) {
            List<SSInventory> iInventories = iRuntime.getInventories();
            if (iInventories == null) {
                return true;
            }
            SSInventory iInventory = new SSInventory();
            iInventory.setNumber(Integer.parseInt(iNumber));
            Optional<SSInventory> optInventory = Repositories.inventories().findByInventory(iInventory);
            if (optInventory.isEmpty()) {
                LOG.warn("EDITINVENTORY trigger: entity not found for number {}", iNumber);
                return true;
            }
            iInventory = optInventory.get();
            int iIndex = iInventories.lastIndexOf(iInventory);
            if (iIndex == -1) {
                return true;
            }
            iInventories.remove(iIndex);
            iInventories.add(iIndex, iInventory);
            if (SSInventoryFrame.getInstance() != null) {
                SSInventoryFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (iTriggerName.equals("DELETEINVENTORY")) {
            List<SSInventory> iInventories = iRuntime.getInventories();
            if (iInventories == null) {
                return true;
            }
            SSInventory iInventory = new SSInventory();
            iInventory.setNumber(Integer.parseInt(iNumber));
            iInventories.remove(iInventory);
            if (SSInventoryFrame.getInstance() != null) {
                SSInventoryFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (iTriggerName.equals("NEWINDELIVERY")) {
            List<SSIndelivery> iIndeliveries = iRuntime.getIndeliveries();
            if (iIndeliveries == null) {
                return true;
            }
            SSIndelivery iIndelivery = new SSIndelivery();
            iIndelivery.setNumber(Integer.parseInt(iNumber));
            Optional<SSIndelivery> optIndelivery = Repositories.indeliveries().findByIndelivery(iIndelivery);
            if (optIndelivery.isEmpty()) {
                LOG.warn("NEWINDELIVERY trigger: entity not found for number {}", iNumber);
                return true;
            }
            iIndelivery = optIndelivery.get();
            if (!iIndeliveries.contains(iIndelivery)) {
                iIndeliveries.add(iIndelivery);
            }
            if (SSIndeliveryFrame.getInstance() != null) {
                SSIndeliveryFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (iTriggerName.equals("EDITINDELIVERY")) {
            List<SSIndelivery> iIndeliveries = iRuntime.getIndeliveries();
            if (iIndeliveries == null) {
                return true;
            }
            SSIndelivery iIndelivery = new SSIndelivery();
            iIndelivery.setNumber(Integer.parseInt(iNumber));
            Optional<SSIndelivery> optIndelivery = Repositories.indeliveries().findByIndelivery(iIndelivery);
            if (optIndelivery.isEmpty()) {
                LOG.warn("EDITINDELIVERY trigger: entity not found for number {}", iNumber);
                return true;
            }
            iIndelivery = optIndelivery.get();
            int iIndex = iIndeliveries.lastIndexOf(iIndelivery);
            if (iIndex == -1) {
                return true;
            }
            iIndeliveries.remove(iIndex);
            iIndeliveries.add(iIndex, iIndelivery);
            if (SSIndeliveryFrame.getInstance() != null) {
                SSIndeliveryFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (iTriggerName.equals("DELETEINDELIVERY")) {
            List<SSIndelivery> iIndeliveries = iRuntime.getIndeliveries();
            if (iIndeliveries == null) {
                return true;
            }
            SSIndelivery iIndelivery = new SSIndelivery();
            iIndelivery.setNumber(Integer.parseInt(iNumber));
            iIndeliveries.remove(iIndelivery);
            if (SSIndeliveryFrame.getInstance() != null) {
                SSIndeliveryFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (iTriggerName.equals("NEWOUTDELIVERY")) {
            List<SSOutdelivery> iOutdeliveries = iRuntime.getOutdeliveries();
            if (iOutdeliveries == null) {
                return true;
            }
            SSOutdelivery iOutdelivery = new SSOutdelivery();
            iOutdelivery.setNumber(Integer.parseInt(iNumber));
            Optional<SSOutdelivery> optOutdelivery = Repositories.outdeliveries().findByOutdelivery(iOutdelivery);
            if (optOutdelivery.isEmpty()) {
                LOG.warn("NEWOUTDELIVERY trigger: entity not found for number {}", iNumber);
                return true;
            }
            iOutdelivery = optOutdelivery.get();
            if (!iOutdeliveries.contains(iOutdelivery)) {
                iOutdeliveries.add(iOutdelivery);
            }
            if (SSOutdeliveryFrame.getInstance() != null) {
                SSOutdeliveryFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (iTriggerName.equals("EDITOUTDELIVERY")) {
            List<SSOutdelivery> iOutdeliveries = iRuntime.getOutdeliveries();
            if (iOutdeliveries == null) {
                return true;
            }
            SSOutdelivery iOutdelivery = new SSOutdelivery();
            iOutdelivery.setNumber(Integer.parseInt(iNumber));
            Optional<SSOutdelivery> optOutdelivery = Repositories.outdeliveries().findByOutdelivery(iOutdelivery);
            if (optOutdelivery.isEmpty()) {
                LOG.warn("EDITOUTDELIVERY trigger: entity not found for number {}", iNumber);
                return true;
            }
            iOutdelivery = optOutdelivery.get();
            int iIndex = iOutdeliveries.lastIndexOf(iOutdelivery);
            if (iIndex == -1) {
                return true;
            }
            iOutdeliveries.remove(iIndex);
            iOutdeliveries.add(iIndex, iOutdelivery);
            if (SSOutdeliveryFrame.getInstance() != null) {
                SSOutdeliveryFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (iTriggerName.equals("DELETEOUTDELIVERY")) {
            List<SSOutdelivery> iOutdeliveries = iRuntime.getOutdeliveries();
            if (iOutdeliveries == null) {
                return true;
            }
            SSOutdelivery iOutdelivery = new SSOutdelivery();
            iOutdelivery.setNumber(Integer.parseInt(iNumber));
            iOutdeliveries.remove(iOutdelivery);
            if (SSOutdeliveryFrame.getInstance() != null) {
                SSOutdeliveryFrame.getInstance().updateFrame();
            }
            return true;
        }
        return true;
    }
}

