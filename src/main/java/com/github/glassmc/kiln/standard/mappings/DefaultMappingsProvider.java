package com.github.glassmc.kiln.standard.mappings;

import net.fabricmc.mapping.tree.*;
import net.fabricmc.mapping.util.EntryTriple;
import org.apache.commons.io.FileUtils;
import org.gradle.internal.Pair;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Default mappings provider.
 */
public abstract class DefaultMappingsProvider implements IMappingsProvider {

    private final Map<String, String> intemediaryMappings = new HashMap<String, String>() {
        {
            put("1.7.10", "https://maven.legacyfabric.net/net/fabricmc/intermediary/1.7.10/intermediary-1.7.10-v2.jar");
            put("1.8.9", "https://maven.legacyfabric.net/net/fabricmc/intermediary/1.8.9/intermediary-1.8.9-v2.jar");
            put("1.12.2", "https://maven.legacyfabric.net/net/fabricmc/intermediary/1.12.2/intermediary-1.12.2-v2.jar");
            put("1.14", "https://maven.fabricmc.net/net/fabricmc/intermediary/1.14/intermediary-1.14-v2.jar");
            put("1.14.1", "https://maven.fabricmc.net/net/fabricmc/intermediary/1.14.1/intermediary-1.14.1-v2.jar");
            put("1.14.2", "https://maven.fabricmc.net/net/fabricmc/intermediary/1.14.2/intermediary-1.14.2-v2.jar");
            put("1.14.3", "https://maven.fabricmc.net/net/fabricmc/intermediary/1.14.3/intermediary-1.14.3-v2.jar");
            put("1.14.4", "https://maven.fabricmc.net/net/fabricmc/intermediary/1.14.4/intermediary-1.14.4-v2.jar");
            put("1.15", "https://maven.fabricmc.net/net/fabricmc/intermediary/1.15/intermediary-1.15-v2.jar");
            put("1.15.1", "https://maven.fabricmc.net/net/fabricmc/intermediary/1.15.1/intermediary-1.15.1-v2.jar");
            put("1.15.2", "https://maven.fabricmc.net/net/fabricmc/intermediary/1.15.2/intermediary-1.15.2-v2.jar");
            put("1.16", "https://maven.fabricmc.net/net/fabricmc/intermediary/1.16/intermediary-1.16-v2.jar");
            put("1.16.1", "https://maven.fabricmc.net/net/fabricmc/intermediary/1.16.1/intermediary-1.16.1-v2.jar");
            put("1.16.2", "https://maven.fabricmc.net/net/fabricmc/intermediary/1.16.2/intermediary-1.16.2-v2.jar");
            put("1.16.3", "https://maven.fabricmc.net/net/fabricmc/yarn/1.16.3+build.47/yarn-1.16.3+build.47-v2.jar");
            put("1.16.4", "https://maven.fabricmc.net/net/fabricmc/intermediary/1.16.4/intermediary-1.16.4-v2.jar");
            put("1.16.5", "https://maven.fabricmc.net/net/fabricmc/intermediary/1.16.5/intermediary-1.16.5-v2.jar");
            put("1.17", "https://maven.fabricmc.net/net/fabricmc/intermediary/1.17/intermediary-1.17-v2.jar");
            put("1.17.1", "https://maven.fabricmc.net/net/fabricmc/intermediary/1.17.1/intermediary-1.17.1-v2.jar");
            put("1.18", "https://maven.fabricmc.net/net/fabricmc/intermediary/1.18/intermediary-1.18-v2.jar");
            put("1.18.1", "https://maven.fabricmc.net/net/fabricmc/intermediary/1.18.1/intermediary-1.18.1-v2.jar");
        }
    };

    private TinyTree namedTree;
    private TinyTree intermediaryTree;
    private Map<String, List<String>> parentClasses;
    private String version;

    @Override
    public void setup(File minecraftFile, String version) throws IOException, NoSuchMappingsException {
        this.version = version;

        File temp = new File(minecraftFile, "temp");
        String intermediaryMapping = intemediaryMappings.get(version);

        if(intermediaryMapping == null) {
            throw new NoSuchMappingsException(version);
        }

        URL intermediaryURL;

        try {
            intermediaryURL = new URL(intermediaryMapping);
        } catch(MalformedURLException e) {
            throw new Error(e);
        }

        try {
            String intermediaryFileBase = intermediaryURL.getFile().substring(intermediaryURL.getFile().lastIndexOf("/")).substring(1).replace(".jar", "");

            File intermediaryMappings = new File(temp, intermediaryFileBase + ".tiny");

            if(!intermediaryMappings.exists()) {
                File intermediaryMappingsFile = new File(temp, intermediaryFileBase + ".jar");
                FileUtils.copyURLToFile(intermediaryURL, intermediaryMappingsFile);

                JarFile intermediaryJARFile = new JarFile(intermediaryMappingsFile);
                FileUtils.copyInputStreamToFile(intermediaryJARFile.getInputStream(new ZipEntry("mappings/mappings.tiny")), intermediaryMappings);
                intermediaryJARFile.close();
            }

            setupNamedMappings(temp, version);

            this.intermediaryTree = TinyMappingFactory.load(new BufferedReader(new FileReader(intermediaryMappings)));

            this.namedTree = loadNamedMappings(intermediaryTree);

            this.parentClasses = new HashMap<>();
            JarFile jarFile = new JarFile(new File(minecraftFile, "client-" + version + ".jar"));
            Enumeration<JarEntry> entries = jarFile.entries();
            while(entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if(entry.getName().endsWith(".class")) {
                    ClassNode classNode = new ClassNode();
                    ClassReader classReader = new ClassReader(jarFile.getInputStream(entry));
                    classReader.accept(classNode, 0);

                    List<String> parents = parentClasses.computeIfAbsent(classNode.name, k -> new ArrayList<>());
                    parents.add(classNode.superName);
                    parents.addAll(classNode.interfaces);
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    protected abstract void setupNamedMappings(File temp, String version) throws IOException, NoSuchMappingsException;

    protected abstract TinyTree loadNamedMappings(TinyTree intermediaryTree);

    @Override
    public Remapper getRemapper(Direction direction) {
        String input = direction == Direction.TO_NAMED ? "official" : "named";
        String middle = "intermediary";
        String output = direction == Direction.TO_NAMED ? "named" : "official";

        TinyRemapper initial = new TinyRemapper(direction == Direction.TO_NAMED ? this.intermediaryTree : this.namedTree, input, middle);
        TinyRemapper result = new TinyRemapper(direction == Direction.TO_NAMED ? this.namedTree : this.intermediaryTree, middle, output);

        return new Remapper() {

            @Override
            public String map(String name) {
                return result.map(initial.map(name));
            }

            @Override
            public String mapMethodName(String owner, String name, String descriptor) {
                for(ClassDef classDef : getClasses(getObfName(owner, direction, initial, result), direction)) {
                    String middleName = classDef.getName(middle);
                    String initialName = classDef.getName(input);

                    String newName = result.mapMethodName(middleName, initial.mapMethodName(initialName, name, descriptor), initial.mapMethodDesc(descriptor));
                    if(!newName.equals(name)) {
                        return newName;
                    }
                }
                return name;
            }

            @Override
            public String mapFieldName(String owner, String name, String descriptor) {
                for(ClassDef classDef : getClasses(getObfName(owner, direction, initial, result), direction)) {
                    String middleName = classDef.getName(middle);
                    String initialName = classDef.getName(input);

                    String newName = result.mapFieldName(middleName, initial.mapFieldName(initialName, name, ""), "");
                    if(!newName.equals(name)) {
                        return newName;
                    }
                }
                return name;
            }

        };
    }

    private List<ClassDef> getClasses(String obfName, Direction direction) {
        List<ClassDef> parents = new ArrayList<>();

        ClassDef classDef = this.intermediaryTree.getDefaultNamespaceClassMap().get(obfName);
        if(classDef != null) {
            ClassDef toAdd;
            if(direction == Direction.TO_NAMED) {
                toAdd = classDef;
            } else {
                toAdd = this.namedTree.getDefaultNamespaceClassMap().get(classDef.getName("intermediary"));
            }

            parents.add(toAdd);
        }

        if(parentClasses.get(obfName) != null) {
            for(String string : parentClasses.get(obfName)) {
                parents.addAll(this.getClasses(string, direction));
            }
        }

        return parents;
    }

    private String getObfName(String name, Direction direction, Remapper initial, Remapper result) {
        if(direction == Direction.TO_NAMED) {
            return name;
        } else if(direction == Direction.TO_OBFUSCATED) {
            return result.map(initial.map(name));
        }
        return name;
    }

    private static class TinyRemapper extends Remapper {

        private final Map<String, String> classNames = new HashMap<>();
        private final Map<EntryTriple, String> fieldNames = new HashMap<>();
        private final Map<EntryTriple, String> methodNames = new HashMap<>();

        private TinyRemapper(TinyTree tree, String from, String to) {
            for (ClassDef clazz : tree.getClasses()) {
                String classNameFrom = clazz.getName(from);
                String classNameTo = clazz.getName(to);

                classNames.put(classNameFrom, classNameTo);

                for(FieldDef field : clazz.getFields()) {
                    fieldNames.put(new EntryTriple(classNameFrom, field.getName(from), ""), field.getName(to));
                }
                for(MethodDef method : clazz.getMethods()) {
                    methodNames.put(new EntryTriple(classNameFrom, method.getName(from), method.getDescriptor(from)), method.getName(to));
                }
            }
        }

        @Override
        public String map(String name) {
            return classNames.getOrDefault(name, name);
        }

        @Override
        public String mapFieldName(final String owner, final String name, final String descriptor) {
            return fieldNames.getOrDefault(new EntryTriple(owner, name, ""), name);
        }

        @Override
        public String mapMethodName(final String owner, final String name, final String descriptor) {
            return methodNames.getOrDefault(new EntryTriple(owner, name, descriptor), name);
        }

    }

    @Override
    public String getVersion() {
        return this.version;
    }

}
