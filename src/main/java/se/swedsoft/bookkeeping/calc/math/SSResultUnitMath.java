package se.swedsoft.bookkeeping.calc.math;


import se.swedsoft.bookkeeping.data.SSNewResultUnit;
import se.swedsoft.bookkeeping.data.system.SSDB;

import java.util.List;
import java.util.Optional;


/**
 * User: Andreas Lago
 * Date: 2007-jan-17
 * Time: 10:54:27
 */
public class SSResultUnitMath {
    private SSResultUnitMath() {}

    /**
     * Returns one resultunit for the current company.
     *
     * @param pNumber
     * @return The resultunit or empty
     */
    public static Optional<SSNewResultUnit> getResultUnit(String pNumber) {

        List<SSNewResultUnit> iResultUnits = SSDB.getInstance().getResultUnits();

        for (SSNewResultUnit iResultUnit: iResultUnits) {
            if (iResultUnit.getNumber().equals(pNumber)) {
                return Optional.of(iResultUnit);
            }
        }
        return Optional.empty();
    }
}
