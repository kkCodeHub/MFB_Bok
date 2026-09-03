package se.swedsoft.bookkeeping.data.system;

import se.swedsoft.bookkeeping.data.SSNewProject;
import se.swedsoft.bookkeeping.persistence.Repositories;

import java.util.List;
import java.util.Optional;

/**
 * Project domain facade.
 */
public final class SSProjectContext {

    private SSProjectContext() {}

    public static List<SSNewProject> getProjects() {
        return Repositories.projects().findAll();
    }

    public static List<SSNewProject> getProjects(List<SSNewProject> pProjects) {
        return Repositories.projects().findAll(pProjects);
    }

    public static void addProject(SSNewProject pProject) {
        Repositories.projects().add(pProject);
    }

    public static void updateProject(SSNewProject pProject) {
        Repositories.projects().update(pProject);
    }

    public static void deleteProject(SSNewProject pProject) {
        Repositories.projects().delete(pProject);
    }

    public static Optional<SSNewProject> getProject(SSNewProject pProject) {
        return Repositories.projects().findById(pProject);
    }

    public static Optional<SSNewProject> getProject(String pProjectNumber) {
        return Repositories.projects().findByNumber(pProjectNumber);
    }
}
