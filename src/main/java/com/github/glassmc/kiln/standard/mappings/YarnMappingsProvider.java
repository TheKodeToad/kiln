package com.github.glassmc.kiln.standard.mappings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.commons.io.FileUtils;

import net.fabricmc.mapping.tree.TinyMappingFactory;
import net.fabricmc.mapping.tree.TinyTree;

public class YarnMappingsProvider extends DefaultMappingsProvider {

    private final Map<String, String> yarnMappings = new HashMap<String, String>() {
        {
            put("1.7.10", "https://maven.legacyfabric.net/net/fabricmc/yarn/1.7.10+build.202112161959/yarn-1.7.10+build.202112161959-v2.jar");
            put("1.8.9", "https://maven.legacyfabric.net/net/fabricmc/yarn/1.7.10+build.202112161959/yarn-1.7.10+build.202112161959-v2.jar");
            put("1.12.2", "https://maven.legacyfabric.net/net/fabricmc/yarn/1.12.2+build.202106280130/yarn-1.12.2+build.202106280130-v2.jar");
            put("1.14", "https://maven.fabricmc.net/net/fabricmc/yarn/1.14+build.21/yarn-1.14+build.21-v2.jar");
            put("1.14.1", "https://maven.fabricmc.net/net/fabricmc/yarn/1.14.1+build.10/yarn-1.14.1+build.10-v2.jar");
            put("1.14.2", "https://maven.fabricmc.net/net/fabricmc/yarn/1.14.2+build.7/yarn-1.14.2+build.7-v2.jar");
            put("1.14.3", "https://maven.fabricmc.net/net/fabricmc/yarn/1.14.3+build.13/yarn-1.14.3+build.13-v2.jar");
            put("1.14.4", "https://maven.fabricmc.net/net/fabricmc/yarn/1.14.4+build.13/yarn-1.14.4+build.18-v2.jar");
            put("1.15", "https://maven.fabricmc.net/net/fabricmc/yarn/1.15+build.2/yarn-1.15+build.2-v2.jar");
            put("1.15.1", "https://maven.fabricmc.net/net/fabricmc/yarn/1.15.1+build.37/yarn-1.15.1+build.37-v2.jar");
            put("1.15.2", "https://maven.fabricmc.net/net/fabricmc/yarn/1.15.2+build.17/yarn-1.15.2+build.17-v2.jar");
            put("1.16", "https://maven.fabricmc.net/net/fabricmc/yarn/1.15.2+build.17/yarn-1.15.2+build.17-v2.jar");
            put("1.16.1", "https://maven.fabricmc.net/net/fabricmc/yarn/1.16.1+build.21/yarn-1.16.1+build.21-v2.jar");
            put("1.16.2", "https://maven.fabricmc.net/net/fabricmc/yarn/1.16.2+build.47/yarn-1.16.2+build.47-v2.jar");
            put("1.16.3", "https://maven.fabricmc.net/net/fabricmc/yarn/1.16.3+build.47/yarn-1.16.3+build.47-v2.jar");
            put("1.16.4", "https://maven.fabricmc.net/net/fabricmc/yarn/1.16.4+build.9/yarn-1.16.4+build.9-v2.jar");
            put("1.16.5", "https://maven.fabricmc.net/net/fabricmc/yarn/1.16.5+build.10/yarn-1.16.5+build.10-v2.jar");
            put("1.17", "https://maven.fabricmc.net/net/fabricmc/yarn/1.17+build.13/yarn-1.17+build.13-v2.jar");
            put("1.17.1", "https://maven.fabricmc.net/net/fabricmc/yarn/1.17.1+build.13/yarn-1.17.1+build.13-v2.jar");
            put("1.18", "https://maven.fabricmc.net/net/fabricmc/yarn/1.18+build.1/yarn-1.18+build.1-v2.jar");
            put("1.18.1", "https://maven.fabricmc.net/net/fabricmc/yarn/1.18.1+build.22/yarn-1.18.1+build.22-v2.jar");
        }
    };

    @Override
    public String getID() {
        return "yarn";
    }

    @Override
    protected TinyTree setupNamedMappings(File temp, String version, TinyTree intermediaryTree) throws IOException, NoSuchMappingsException {
        String named = yarnMappings.get(version);

        if(named == null) {
            throw new NoSuchMappingsException(version);
        }

        URL namedURL = new URL(named);

        String namedFileBase = namedURL.getFile().substring(namedURL.getFile().lastIndexOf("/")).substring(1)
                .replace(".jar", "");
        File namedMappings = new File(temp, namedFileBase + ".tiny");

        if(!namedMappings.exists()) {
            File namedMappingsFile = new File(temp, namedFileBase + ".jar");
            FileUtils.copyURLToFile(namedURL, namedMappingsFile);

            JarFile namedJARFile = new JarFile(namedMappingsFile);
            FileUtils.copyInputStreamToFile(namedJARFile.getInputStream(new ZipEntry("mappings/mappings.tiny")), namedMappings);
            namedJARFile.close();
        }

        return TinyMappingFactory.load(new BufferedReader(new FileReader(namedMappings)));
    }

}
