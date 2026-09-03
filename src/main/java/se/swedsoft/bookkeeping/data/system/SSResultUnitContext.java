package se.swedsoft.bookkeeping.data.system;

import se.swedsoft.bookkeeping.data.SSNewResultUnit;
import se.swedsoft.bookkeeping.persistence.Repositories;

import java.util.List;
import java.util.Optional;

/**
 * ResultUnit domain facade.
 */
public final class SSResultUnitContext {

    private SSResultUnitContext() {}

    public static List<SSNewResultUnit> getResultUnits() {
        return Repositories.resultUnits().findAll();
    }

    public static List<SSNewResultUnit> getResultUnits(List<SSNewResultUnit> pResultUnits) {
        return Repositories.resultUnits().findAll(pResultUnits);
    }

    public static void addResultUnit(SSNewResultUnit pResultUnit) {
        Repositories.resultUnits().add(pResultUnit);
    }

    public static void updateResultUnit(SSNewResultUnit pResultUnit) {
        Repositories.resultUnits().update(pResultUnit);
    }

    public static void deleteResultUnit(SSNewResultUnit pResultUnit) {
        Repositories.resultUnits().delete(pResultUnit);
    }

    public static Optional<SSNewResultUnit> getResultUnit(SSNewResultUnit pResultUnit) {
        return Repositories.resultUnits().findById(pResultUnit);
    }

    public static Optional<SSNewResultUnit> getResultUnit(String pResultUnitNumber) {
        return Repositories.resultUnits().findByNumber(pResultUnitNumber);
    }
}
