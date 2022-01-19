package com.github.glassmc.kiln.standard.mappings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.gradle.internal.Pair;
import org.json.JSONObject;

import com.github.glassmc.kiln.common.Util;

import net.fabricmc.mapping.tree.ClassDef;
import net.fabricmc.mapping.tree.Descriptored;
import net.fabricmc.mapping.tree.TinyMappingFactory;
import net.fabricmc.mapping.tree.TinyTree;

public class MojangMappingsProvider extends DefaultMappingsProvider {

    @Override
    public String getID() {
        return "mojang";
    }

    @Override
    protected TinyTree setupNamedMappings(File temp, String version, TinyTree intermediaryTree)
            throws IOException, NoSuchMappingsException {
        JSONObject versionInfo = Util.getVersionManifest(version);

        JSONObject downloads = versionInfo.getJSONObject("downloads");
        JSONObject clientMappings = downloads.getJSONObject("client_mappings");

        if(clientMappings == null) {
            throw new NoSuchMappingsException(version);
        }

        URL mojangURL = new URL(clientMappings.getString("url"));

        File mojangMappings = new File(temp, "intermediary2mojang-" + version + ".tiny");

        if(!mojangMappings.exists()) {
            File proguardMojangMappings = new File(temp, "mojang-mappings-" + version + ".txt");
            FileUtils.copyURLToFile(mojangURL, proguardMojangMappings);

            convert(proguardMojangMappings, mojangMappings, intermediaryTree);
        }

        return TinyMappingFactory.load(new BufferedReader(new FileReader(mojangMappings)));
    }

    private static void convert(File proGuardMappings, File tinyMappings, TinyTree intermediaryTree)
            throws IOException {
        StringBuilder result = new StringBuilder();

        ClassDef currentClass = null;

        result.append("tiny\t2\t0\tintermediary\tnamed\n");

        Map<String, String> namedToIntemediary = new HashMap<>();
        Map<String, String> namedToOfficial = new HashMap<>();
        Map<String, Pair<String, String>> lineToClassName = new HashMap<>();
        Map<String, ClassDef> lineToClass = new HashMap<>();

        List<String> lines = Files.readAllLines(proGuardMappings.toPath());

        for(String line : lines) {
            line = strip(line);

            if(!line.endsWith(":")) {
                continue;
            }

            String officialSrcName;
            String srcName;

            officialSrcName = srcName = line.substring(line.indexOf("->") + 3, line.indexOf(":"));
            ClassDef clazz = intermediaryTree.getDefaultNamespaceClassMap().get(srcName);
            if(clazz != null) {
                srcName = clazz.getName("intermediary");
            }

            String dstName = line.substring(0, line.indexOf(" ")).replace(".", "/");

            namedToIntemediary.put(dstName, srcName);
            namedToOfficial.put(dstName, officialSrcName);

            lineToClassName.put(line, Pair.of(srcName, dstName));
            lineToClass.put(line, clazz);
        }

        for(String line : Files.readAllLines(proGuardMappings.toPath())) {
            line = strip(line);

            if(line.isEmpty()) {
                continue;
            }

            if(line.endsWith(":")) {
                currentClass = lineToClass.get(line);
                Pair<String, String> className = lineToClassName.get(line);

                result.append("c\t" + className.getLeft() + "\t" + className.getRight() + "\n");
            } else if(currentClass != null) {
                line = line.substring(line.lastIndexOf(":") + 1);

                String friendlyType = line.substring(0, line.indexOf(" "));
                line = line.substring(line.indexOf(" ") + 1);

                int bracketIndex = line.indexOf("(");

                boolean method = false;

                String name;

                String[] friendlyParameters = null;

                if(bracketIndex != -1) {
                    method = true;
                    name = line.substring(0, line.indexOf("("));

                    friendlyParameters = line.substring(line.indexOf("(") + 1, line.indexOf(")")).split(",");
                } else {
                    name = line.substring(0, line.indexOf(" "));
                }

                line = line.substring(line.indexOf("->") + 3);

                if(method) {
                    String desc = toDescriptor(friendlyParameters, friendlyType);

                    String intermediaryName = getIntemediaryMemberName(currentClass, currentClass.getMethods(), line,
                            remapDescriptor(desc, namedToOfficial));

                    if(intermediaryName == null) {
                        continue;
                    }

                    result.append("\tm\t" + remapDescriptor(desc, namedToIntemediary) + "\t" + intermediaryName + "\t"
                            + name + "\n");
                } else {
                    String desc = toDescriptor(friendlyType);

                    String intemediaryName = getIntemediaryMemberName(currentClass, currentClass.getFields(), line,
                            remapDescriptor(desc, namedToOfficial));

                    if(intemediaryName == null) {
                        continue;
                    }

                    result.append("\tf\t" + remapDescriptor(toDescriptor(friendlyType), namedToIntemediary) + "\t"
                            + intemediaryName + "\t" + name + "\n");
                }
            }
        }

        FileUtils.writeStringToFile(tinyMappings, result.toString(), StandardCharsets.UTF_8);
    }

    private static String strip(String line) {
        if(line.startsWith("#")) {
            return "";
        }

        while(Character.isWhitespace(line.charAt(0))) {
            line = line.substring(1);
        }

        return line;
    }

    private static String getIntemediaryMemberName(ClassDef currentClass, Collection<?> collection, String name,
            String desc) {
        if(currentClass == null) {
            return null;
        }

        for(Object obj : collection) {
            Descriptored member = (Descriptored) obj;

            if(member.getName("official").equals(name) && member.getDescriptor("official").equals(desc)) {
                String newName = member.getName("intermediary");

                if(newName == null) {
                    return null;
                }

                return newName;
            }
        }

        return null;
    }

    private static String toDescriptor(String type) {
        if(type.isEmpty()) {
            return type;
        }

        int dimensions = 0;
        while(type.contains("[]")) {
            dimensions++;
            type = type.replace("[]", "");
        }

        StringBuilder descriptor = new StringBuilder();

        for(int i = 0; i < dimensions; i++) {
            descriptor.append("[");
        }

        switch(type) {
        case "byte":
            descriptor.append("B");
            break;
        case "char":
            descriptor.append("C");
            break;
        case "double":
            descriptor.append("D");
            break;
        case "float":
            descriptor.append("F");
            break;
        case "int":
            descriptor.append("I");
            break;
        case "long":
            descriptor.append("J");
            break;
        case "short":
            descriptor.append("S");
            break;
        case "boolean":
            descriptor.append("Z");
            break;
        case "void":
            descriptor.append("V");
            break;
        default:
            descriptor.append("L" + type.replace(".", "/") + ";");
            break;
        }

        return descriptor.toString();
    }

    private static String toDescriptor(String[] args, String type) {
        StringBuilder result = new StringBuilder();

        result.append("(");

        for(String arg : args) {
            result.append(toDescriptor(arg));
        }

        result.append(")");

        result.append(toDescriptor(type));

        return result.toString();
    }

    private static String remapDescriptor(String descriptor, Map<String, String> map) {
        StringBuilder classBuilder = null;
        StringBuilder result = new StringBuilder();

        for(char c : descriptor.toCharArray()) {
            if(c == ';' && classBuilder != null) {
                String clazz = classBuilder.toString();

                clazz = map.getOrDefault(clazz, clazz);

                result.append("L" + clazz + ";");
                classBuilder = null;
            } else if(c == 'L' && classBuilder == null) {
                classBuilder = new StringBuilder();
            } else {
                if(classBuilder != null) {
                    classBuilder.append(c);
                } else {
                    result.append(c);
                }
            }
        }

        return result.toString();
    }

}
