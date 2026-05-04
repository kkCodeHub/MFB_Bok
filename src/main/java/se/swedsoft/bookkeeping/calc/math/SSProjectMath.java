package se.swedsoft.bookkeeping.calc.math;


import se.swedsoft.bookkeeping.data.SSNewProject;

import java.util.List;
import java.util.Optional;


/**
 * User: Andreas Lago
 * Date: 2006-okt-05
 * Time: 15:41:58
 */
public class SSProjectMath {
    private SSProjectMath() {}

    /**
     * Returns one project for the current company.
     *
     * @param iProjects
     * @param iNumber
     * @return The project or empty
     */
    public static Optional<SSNewProject> getProject(List<SSNewProject> iProjects, String iNumber) {
        for (SSNewProject iProject: iProjects) {
            if (iProject.getNumber().equals(iNumber)) {
                return Optional.of(iProject);
            }
        }
        return Optional.empty();
    }

}
