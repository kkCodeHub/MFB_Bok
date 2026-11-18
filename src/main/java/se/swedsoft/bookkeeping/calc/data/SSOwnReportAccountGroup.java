package se.swedsoft.bookkeeping.calc.data;


import se.swedsoft.bookkeeping.data.SSAccount;

import java.util.LinkedList;
import java.util.List;


/**
 * User: Johan Gunnarsson
 * Date: 2007-nov-29
 * Time: 08:51:44
 */
public class SSOwnReportAccountGroup {

    private Integer iId;

    private List<SSAccount> iAccounts;

    private List<SSOwnReportAccountGroup> iGroups;

    private String iName;

    private String iSumTitle;

    /**
     *
     */
    public SSOwnReportAccountGroup() {
        iGroups = null;
        iId = -1;
    }

    /**
     *
     */
    public Integer getId() {
        return iId;
    }

    /**
     *
     * @param iId
     */
    public void setId(Integer iId) {
        this.iId = iId;
    }

    public void setAccounts(List<SSAccount> iAccounts) {
        this.iAccounts = iAccounts;
    }

    /**
     *
     */
    public List<SSAccount> getAccounts() {
        return iAccounts;
    }

    /**
     *
     */
    public String getName() {
        return iName;
    }

    /***
     *
     * @param iName
     */
    public void setName(String iName) {
        this.iName = iName;
    }

    /**
     *
     */
    public List<SSOwnReportAccountGroup> getGroups() {
        return iGroups;
    }

    public void setGroups(List<SSOwnReportAccountGroup> iGroups) {
        this.iGroups = iGroups;
    }

    /**
     *
     * @param pAccountGroup
     */
    public void addAccountGroup(SSOwnReportAccountGroup pAccountGroup) {
        if (iGroups == null) {
            iGroups = new LinkedList<>();
        }

        iGroups.add(pAccountGroup);
    }

    public void setSumTitle(String iSumTitle) {
        this.iSumTitle = iSumTitle;
    }

    public String getSumTitle() {
        return iSumTitle;
    }

    public String toString() {
        return toString("");
    }

    public String toString(String Seperator) {
        return iName;
    }

}
