package se.swedsoft.bookkeeping.data;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("deprecation")
class SSLegacyShimTest {

    @Test
    void projectShimShouldExposeLegacyIntApiThroughV2State() {
        SSProject project = new SSProject(101, "Projekt A", "Beskrivning");
        Date concludedDate = new Date();

        project.setConcluded(true);
        project.setConcludedDate(concludedDate);

        assertThat(project.getNumber()).isEqualTo(101);
        assertThat(project.getName()).isEqualTo("Projekt A");
        assertThat(project.getDescription()).isEqualTo("Beskrivning");
        assertThat(project.getConcluded()).isTrue();
        assertThat(project.getConcludedDate()).isEqualTo(concludedDate);
        assertThat(project.toRenderString()).isEqualTo("101");
        assertThat(project.toString()).contains("101", "Projekt A", "Beskrivning");
    }

    @Test
    void projectShimEqualityShouldStillCompareLegacyNumber() {
        SSProject left = new SSProject(7, "A", "X");
        SSProject right = new SSProject(7, "B", "Y");

        assertThat(left).isEqualTo(right);
    }

    @Test
    void resultUnitShimShouldExposeLegacyIntApiThroughV2State() {
        SSResultUnit resultUnit = new SSResultUnit(55, "Enhet A");
        resultUnit.setDescription("Beskrivning");

        assertThat(resultUnit.getNumber()).isEqualTo(55);
        assertThat(resultUnit.getName()).isEqualTo("Enhet A");
        assertThat(resultUnit.getDescription()).isEqualTo("Beskrivning");
        assertThat(resultUnit.toRenderString()).isEqualTo("55");
        assertThat(resultUnit.toString()).contains("55", "Enhet A", "Beskrivning");
    }

    @Test
    void resultUnitShimEqualityShouldStillCompareLegacyNumber() {
        SSResultUnit left = new SSResultUnit(9, "A");
        SSResultUnit right = new SSResultUnit(9, "B");

        assertThat(left).isEqualTo(right);
    }
}


