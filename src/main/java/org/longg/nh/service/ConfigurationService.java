package org.longg.nh.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.longg.nh.model.ArchitectureConfig;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public final class ConfigurationService {

    private final Map<String, ArchitectureConfig> projectConfigs = new HashMap<>();

    public static ConfigurationService getInstance() {
        return com.intellij.openapi.application.ApplicationManager
                .getApplication()
                .getService(ConfigurationService.class);
    }

    public Optional<ArchitectureConfig> getConfiguration(Project project) {
        String projectPath = project.getBasePath();
        if (!projectConfigs.containsKey(projectPath)) {
            loadConfiguration(project);
        }
        return Optional.ofNullable(projectConfigs.get(projectPath));
    }

    public void loadConfiguration(Project project) {
        String projectPath = project.getBasePath();
        if (projectPath == null) {
            return;
        }

        File configFile = new File(projectPath, "entity-generator-config.json");
        if (!configFile.exists()) {
            projectConfigs.remove(projectPath);
            return;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            ArchitectureConfig config = mapper.readValue(configFile, ArchitectureConfig.class);
            projectConfigs.put(projectPath, config);
        } catch (IOException e) {
            projectConfigs.remove(projectPath);
        }
    }

    public void saveDefaultConfiguration(Project project) {
        String projectPath = project.getBasePath();
        if (projectPath == null) {
            return;
        }

        try {
            ArchitectureConfig defaultConfig = new ArchitectureConfig();
            defaultConfig.setName("Default");
            defaultConfig.setDescription("Default architecture pattern");
            defaultConfig.setDtoPackage("dto");
            defaultConfig.setServicePackage("service");
            defaultConfig.setRepositoryPackage("repository");
            defaultConfig.setControllerPackage("controller");
            defaultConfig.setFilterPackage("filter");
            defaultConfig.setUseLombok(true);

            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter()
                  .writeValue(new File(projectPath, "entity-generator-config.json"), defaultConfig);
            
            projectConfigs.put(projectPath, defaultConfig);
        } catch (IOException e) {
            // Handle error
        }
    }
} 