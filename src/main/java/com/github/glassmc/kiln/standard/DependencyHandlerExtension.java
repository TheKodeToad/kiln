package com.github.glassmc.kiln.standard;

import com.github.glassmc.kiln.standard.mappings.IMappingsProvider;
import com.github.glassmc.kiln.standard.mappings.MCPMappingsProvider;
import com.github.glassmc.kiln.standard.mappings.MojangMappingsProvider;
import com.github.glassmc.kiln.common.Util;
import com.github.glassmc.kiln.standard.mappings.NoSuchMappingsException;
import com.github.glassmc.kiln.standard.mappings.ObfuscatedMappingsProvider;
import com.github.glassmc.kiln.standard.mappings.YarnMappingsProvider;
import com.github.jezza.Toml;
import com.github.jezza.TomlTable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.file.FileCollection;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SuppressWarnings("unused")
public class DependencyHandlerExtension {

    public static FileCollection minecraft(String id, String version, String mappingsProviderId) {
        KilnStandardPlugin plugin = KilnStandardPlugin.getInstance();
        File pluginCache = plugin.getCache();

        IMappingsProvider mappingsProvider;
        switch(mappingsProviderId) {
            case "yarn":
                mappingsProvider = new YarnMappingsProvider();
                break;
            case "mojang":
                mappingsProvider = new MojangMappingsProvider();
                break;
            case "mcp":
                mappingsProvider = new MCPMappingsProvider();
                break;
            case "obfuscated":
            default:
                mappingsProvider = new ObfuscatedMappingsProvider();
        }
        plugin.addMappingsProvider(mappingsProvider);

        List<String> filesToDepend = new ArrayList<>();

        File minecraftFile = new File(pluginCache, "minecraft");
        File versionFile = new File(minecraftFile, version);
        File versionMappedJARFile = new File(versionFile, id + "-" + version + "-" + mappingsProvider.getID() + ".jar");
        File versionMappedLibraries = new File(versionFile, "mappedLibraries");

        Util.setupMinecraft(id, version, pluginCache, new ObfuscatedMappingsProvider());

        try {
            mappingsProvider.setup(versionFile, version);
        } catch (NoSuchMappingsException e) {
            e.printStackTrace();
        }

        Util.setupMinecraft(id, version, pluginCache, mappingsProvider);

        filesToDepend.add(versionMappedJARFile.getAbsolutePath());
        for (File file : Objects.requireNonNull(versionMappedLibraries.listFiles())) {
            filesToDepend.add(file.getAbsolutePath());
        }

        return plugin.getProject().files(filesToDepend.toArray());
    }

}
