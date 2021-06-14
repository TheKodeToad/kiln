package ml.glassmc.kiln.main;

import ml.glassmc.kiln.main.task.GetRunConfiguration;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.io.File;

public class KilnMainPlugin implements Plugin<Project> {

    private static KilnMainPlugin instance;

    public static KilnMainPlugin getInstance() {
        return instance;
    }

    private Project project;

    @Override
    public void apply(Project project) {
        instance = this;
        this.project = project;

        project.getTasks().register("getRunConfiguration", GetRunConfiguration.class);
    }

    public File getCache() {
        File cache = new File(this.project.getGradle().getGradleUserHomeDir() + "/caches/kiln");
        cache.mkdirs();
        return cache;
    }

    public Project getProject() {
        return project;
    }

}
