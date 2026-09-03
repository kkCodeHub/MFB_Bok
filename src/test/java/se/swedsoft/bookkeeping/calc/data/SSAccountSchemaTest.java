package se.swedsoft.bookkeeping.calc.data;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSAccountPlan;
import se.swedsoft.bookkeeping.data.SSAccountPlanType;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests for {@link SSAccountSchema}.
 */
class SSAccountSchemaTest {

    @Test
    void getAccountSchemaFallsBackWhenAccountPlanTypeIsMissing() {
        SSNewAccountingYear year = new SSNewAccountingYear();
        SSAccountPlan accountPlan = new SSAccountPlan();
        accountPlan.setType((SSAccountPlanType) null);
        year.setAccountPlan(accountPlan);

        assertThatCode(() -> SSAccountSchema.getAccountSchema(year)).doesNotThrowAnyException();
    }
}
