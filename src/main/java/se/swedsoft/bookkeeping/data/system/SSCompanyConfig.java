package se.swedsoft.bookkeeping.data.system;


import org.fribok.bookkeeping.app.Path;
import se.swedsoft.bookkeeping.data.SSNewCompany;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Johan Gunnarsson
 * Date: 2007-jan-26
 * Time: 11:40:47
 */
public class SSCompanyConfig {    private static final Logger LOG = LoggerFactory.getLogger(SSCompanyConfig.class);
    private static final String LAST_COMPANY_FILE = "lastcompanyopen.config";
    private static final String COMPANY_SETTINGS_FILE = "companysettings.config";
    private static final String KEY_COMPANY_ID = "company.id";
    private static final String KEY_COMPANY_NAME = "company.name";
    private static final String KEY_COMPANY_CURRENT = "company.current";
    private static final String KEY_CURRENT_YEAR_INDEX = "company.currentYearIndex";
    private static final String KEY_YEAR_COUNT = "year.count";
    private static final String KEY_YEAR_FROM = "from";
    private static final String KEY_YEAR_TO = "to";
    private static final String KEY_YEAR_ACCOUNT_PLAN = "accountPlan";
    private static final String KEY_YEAR_CURRENT = "current";

    private SSCompanyConfig() {}
/*
    public static void saveLastOpenCompany(SSSystemCompany iLastCompany) {
        File iFile = new File(Path.get(Path.APP_BASE), LAST_COMPANY_FILE);

        if (iFile.exists()) {
            iFile.delete();
        }
        try (FileOutputStream outputStream = new FileOutputStream(iFile)) {
            Properties properties = new Properties();
            writeSystemCompany(properties, iLastCompany);
            properties.store(outputStream, "Last open company (V2 explicit format)");
        } catch (IOException e) {
            LOG.error("Unexpected error", e);
        }
    }

    public static Optional<SSSystemCompany> openLastOpenCompany() {
        File iFile = new File(Path.get(Path.APP_BASE), LAST_COMPANY_FILE);

        if (!iFile.exists()) {
            return Optional.empty();
        }

        try (FileInputStream inputStream = new FileInputStream(iFile)) {
            Properties properties = new Properties();
            properties.load(inputStream);
            return Optional.of(readSystemCompany(properties));
        } catch (IOException e) {
            iFile.delete();
            return Optional.empty();
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            return Optional.empty();
        }
    }

    public static void saveCompanySetting(SSNewCompany iCompany) {
        if (iCompany == null) {
            return;
        }

        File iFile = new File(Path.get(Path.APP_BASE), COMPANY_SETTINGS_FILE);
        Properties properties = new Properties();
        properties.setProperty(KEY_COMPANY_ID, Integer.toString(iCompany.getId()));
        properties.setProperty(KEY_COMPANY_NAME, iCompany.getName() == null ? "" : iCompany.getName());

        try {
            if (iFile.exists()) {
                iFile.delete();
            }
            try (FileOutputStream outputStream = new FileOutputStream(iFile)) {
                properties.store(outputStream, "Company setting (V2 explicit format)");
            }
        } catch (IOException e) {
            LOG.error("Unexpected error", e);
        }
    }

    public static Optional<SSSystemCompany> openCompanySetting(SSSystemCompany iCompany) {
        File iFile = new File(Path.get(Path.APP_BASE), COMPANY_SETTINGS_FILE);

        if (!iFile.exists()) {
            return Optional.empty();
        }

        try (FileInputStream inputStream = new FileInputStream(iFile)) {
            Properties properties = new Properties();
            properties.load(inputStream);
            String storedName = properties.getProperty(KEY_COMPANY_NAME, "");
            if (iCompany != null && storedName.equals(iCompany.getName())) {
                SSSystemCompany systemCompany = new SSSystemCompany();
                systemCompany.setName(storedName);
                return Optional.of(systemCompany);
            }
            return Optional.empty();
        } catch (IOException e) {
            return Optional.empty();
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            return Optional.empty();
        }
    }

    private static void writeSystemCompany(Properties properties, SSSystemCompany company) {
        if (company == null) {
            return;
        }
        properties.setProperty(KEY_COMPANY_NAME, company.getName() == null ? "" : company.getName());
        properties.setProperty(KEY_COMPANY_CURRENT, Boolean.toString(company.isCurrent()));

        List<SSSystemYear> years = company.getYears() == null ? new LinkedList<>() : company.getYears();
        properties.setProperty(KEY_YEAR_COUNT, Integer.toString(years.size()));

        int currentYearIndex = -1;
        for (int i = 0; i < years.size(); i++) {
            SSSystemYear year = years.get(i);
            String prefix = "year." + i + '.';
            LocalDate from = year.getLocalFrom();
            LocalDate to = year.getLocalTo();
            if (from != null) {
                properties.setProperty(prefix + KEY_YEAR_FROM, from.toString());
            }
            if (to != null) {
                properties.setProperty(prefix + KEY_YEAR_TO, to.toString());
            }
            if (year.getAccountPlan() != null) {
                properties.setProperty(prefix + KEY_YEAR_ACCOUNT_PLAN, year.getAccountPlan());
            }
            properties.setProperty(prefix + KEY_YEAR_CURRENT, Boolean.toString(year.isCurrent()));
            if (year.isCurrent()) {
                currentYearIndex = i;
            }
        }
        properties.setProperty(KEY_CURRENT_YEAR_INDEX, Integer.toString(currentYearIndex));
    }

    private static SSSystemCompany readSystemCompany(Properties properties) {
        SSSystemCompany company = new SSSystemCompany();
        company.setName(properties.getProperty(KEY_COMPANY_NAME, ""));
        company.setCurrent(Boolean.parseBoolean(properties.getProperty(KEY_COMPANY_CURRENT, "false")));

        int count = Integer.parseInt(properties.getProperty(KEY_YEAR_COUNT, "0"));
        int currentYearIndex = Integer.parseInt(properties.getProperty(KEY_CURRENT_YEAR_INDEX, "-1"));
        List<SSSystemYear> years = new LinkedList<>();
        for (int i = 0; i < count; i++) {
            String prefix = "year." + i + '.';
            SSSystemYear year = new SSSystemYear();
            String fromValue = properties.getProperty(prefix + KEY_YEAR_FROM);
            if (fromValue != null && !fromValue.isEmpty()) {
                year.setLocalFrom(LocalDate.parse(fromValue));
            }
            String toValue = properties.getProperty(prefix + KEY_YEAR_TO);
            if (toValue != null && !toValue.isEmpty()) {
                year.setLocalTo(LocalDate.parse(toValue));
            }
            year.setAccountPlan(properties.getProperty(prefix + KEY_YEAR_ACCOUNT_PLAN));
            year.setCurrent(i == currentYearIndex
                    || Boolean.parseBoolean(properties.getProperty(prefix + KEY_YEAR_CURRENT, "false")));
            years.add(year);
        }
        company.setYears(years);
        return company;
    }

    public static void deleteFiles() {
        File iFile1 = new File(Path.get(Path.APP_BASE), LAST_COMPANY_FILE);
        File iFile2 = new File(Path.get(Path.APP_BASE), COMPANY_SETTINGS_FILE);

        if (iFile1.exists()) {
            iFile1.delete();
        }

        if (iFile2.exists()) {
            iFile2.delete();
        }
    }
 */
}
