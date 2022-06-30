package com.github.glassmc.kiln.main;

import com.github.glassmc.kiln.common.Pair;
import com.github.glassmc.kiln.main.task.ClearMappings;
import com.github.glassmc.kiln.main.task.GenerateRunConfiguration;
import com.github.glassmc.kiln.standard.KilnStandardPlugin;
import com.github.glassmc.kiln.standard.mappings.IMappingsProvider;
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class KilnMainPlugin implements Plugin<Project> {

    protected static KilnMainPlugin instance;

    public static KilnMainPlugin getInstance() {
        return instance;
    }

    protected Project project;

    private List<Pair<IMappingsProvider, Boolean>> allMappingsProviders = new ArrayList<>();

    @Override
    public void apply(Project project) {
        instance = this;
        this.project = project;

        project.getPlugins().apply("kiln-standard");

        project.getTasks().register("genRunConfiguration", GenerateRunConfiguration.class);
        project.getTasks().register("clearMappings", ClearMappings.class);

        this.setupShadow();
    }

    private void setupShadow() {
        Configuration shadowRuntime = project.getConfigurations().create("shadowRuntime");
        Configuration runtimeOnly = project.getConfigurations().getByName("runtimeOnly");
        runtimeOnly.extendsFrom(shadowRuntime);

        ShadowJar shadowJar = (ShadowJar) project.getTasks().getByName("shadowJar");
        shadowJar.getConfigurations().add(project.getConfigurations().getByName("shadowRuntime"));
    }

    public File getCache() {
        return KilnStandardPlugin.getInstance().getCache();
    }

    public void addAllMappingsProvider(IMappingsProvider mappingsProvider, boolean prefix) {
        this.allMappingsProviders.add(new Pair<>(mappingsProvider, prefix));
    }

    public List<Pair<IMappingsProvider, Boolean>> getAllMappingsProviders() {
        return allMappingsProviders;
    }

}
