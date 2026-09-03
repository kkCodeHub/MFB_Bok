package se.swedsoft.bookkeeping.importexport.excel;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSAccountPlan;
import se.swedsoft.bookkeeping.importexport.util.SSImportException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SSAccountPlanDefaultResourceDiscoveryTest {

    @Test
    void discoverDefaultExcelResourcesFindsBundledPlans() throws Exception {
        List<String> resources = SSAccountPlanDefaultResourceDiscovery.discoverDefaultExcelResources(
                SSAccountPlanDefaultResourceDiscoveryTest.class.getClassLoader());

        assertThat(resources).isNotEmpty();
        assertThat(resources)
                .allMatch(path -> path.startsWith("account/default/"))
                .allMatch(path -> path.endsWith(".xls") || path.endsWith(".xlsx"));
        assertThat(resources).contains("account/default/1Bas 2026 K2-AB&EF Full.xlsx");
    }

    @Test
    void validateFileNameAgainstPlanNameUsesSameRule() {
        SSAccountPlan plan = new SSAccountPlan();
        plan.setName("Plan-A");

        // Filnamn utan inledande siffror
        SSAccountPlanImporter.validateFileNameAgainstPlanName("Plan-A.xlsx", plan);

        // Filnamn med inledande siffror - ska accepteras om resten av namn stämmer
        SSAccountPlanImporter.validateFileNameAgainstPlanName("1Plan-A.xls", plan);
        SSAccountPlanImporter.validateFileNameAgainstPlanName("123Plan-A.xlsx", plan);

        // Felaktigt namn ska kasta exception
        assertThatThrownBy(() -> SSAccountPlanImporter.validateFileNameAgainstPlanName("Plan-B.xlsx", plan))
                .isInstanceOf(SSImportException.class);

        assertThatThrownBy(() -> SSAccountPlanImporter.validateFileNameAgainstPlanName("1Plan-B.xlsx", plan))
                .isInstanceOf(SSImportException.class);
    }
}
