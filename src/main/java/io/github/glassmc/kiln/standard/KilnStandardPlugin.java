package io.github.glassmc.kiln.standard;

import io.github.glassmc.kiln.standard.mappings.IMappingsProvider;
import org.apache.commons.io.IOUtils;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.*;

public class KilnStandardPlugin implements Plugin<Project> {

    private static KilnStandardPlugin instance;

    public static KilnStandardPlugin getInstance() {
        return instance;
    }

    private Project project;

    @Override
    public void apply(Project project) {
        instance = this;
        this.project = project;

        project.afterEvaluate(p -> {
            project.getTasks().findByName("compileJava").doLast(new ReobfuscateAction());
        });
    }

    public File getCache() {
        File cache = new File(this.project.getGradle().getGradleUserHomeDir() + "/caches/kiln");
        cache.mkdirs();
        return cache;
    }

    public File getLocalRepository() {
        return new File(this.getCache(), "repository");
    }

    public Project getProject() {
        return project;
    }

    private class ReobfuscateAction implements Action<Task> {

        @Override
        public void execute(Task task) {
            File classes = new File(project.getBuildDir(), "classes");
            for(File file : project.fileTree(classes)) {
                if(!file.getName().endsWith(".class")) {
                    continue;
                }

                try {
                    System.out.println("test");
                    Remapper remapper = DependencyHandlerExtension.mappingsProvider.getRemapper(IMappingsProvider.Direction.TO_OBFUSCATED);

                    InputStream inputStream = new FileInputStream(file);
                    ClassReader classReader = new ClassReader(IOUtils.readFully(inputStream, inputStream.available()));
                    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    ClassVisitor visitor = new ClassRemapper(writer, remapper);
                    classReader.accept(visitor, ClassReader.EXPAND_FRAMES);
                    inputStream.close();
                    OutputStream outputStream = new FileOutputStream(file);
                    outputStream.write(writer.toByteArray());
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

}
