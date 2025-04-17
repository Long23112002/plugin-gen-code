package org.longg.nh.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.longg.nh.model.ArchitectureConfig;
import org.longg.nh.model.ProjectStructureConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public final class ConfigurationService {

    private final Map<String, ArchitectureConfig> projectConfigs = new HashMap<>();
    private final Map<String, List<ProjectStructureConfig>> projectStructureTemplates = new HashMap<>();

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
        
        // Load project structure templates
        File templatesFile = new File(projectPath, "entity-generator-templates.json");
        if (templatesFile.exists()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                ProjectStructureConfig[] templates = mapper.readValue(templatesFile, ProjectStructureConfig[].class);
                
                List<ProjectStructureConfig> templateList = new ArrayList<>();
                for (ProjectStructureConfig template : templates) {
                    templateList.add(template);
                }
                
                projectStructureTemplates.put(projectPath, templateList);
            } catch (IOException e) {
                // Handle error
                projectStructureTemplates.remove(projectPath);
            }
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
            defaultConfig.setUseDtoValidation(false);
            defaultConfig.setCustomDtoPath("");
            defaultConfig.setCustomServicePath("");
            defaultConfig.setCustomRepositoryPath("");
            defaultConfig.setCustomControllerPath("");
            defaultConfig.setCustomFilterPath("");

            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter()
                  .writeValue(new File(projectPath, "entity-generator-config.json"), defaultConfig);
            
            projectConfigs.put(projectPath, defaultConfig);
            
            // Save default project structure template
            ProjectStructureConfig defaultTemplate = new ProjectStructureConfig();
            List<ProjectStructureConfig> templates = new ArrayList<>();
            templates.add(defaultTemplate);
            
            mapper.writerWithDefaultPrettyPrinter()
                  .writeValue(new File(projectPath, "entity-generator-templates.json"), templates);
                  
            projectStructureTemplates.put(projectPath, templates);
        } catch (IOException e) {
            // Handle error
        }
    }
    
    public void updateConfiguration(Project project, ArchitectureConfig config) {
        String projectPath = project.getBasePath();
        if (projectPath == null) {
            return;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter()
                  .writeValue(new File(projectPath, "entity-generator-config.json"), config);
            
            projectConfigs.put(projectPath, config);
        } catch (IOException e) {
            // Handle error
        }
    }
    
    public void saveProjectStructureTemplates(Project project, List<ProjectStructureConfig> templates) {
        String projectPath = project.getBasePath();
        if (projectPath == null) {
            return;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter()
                  .writeValue(new File(projectPath, "entity-generator-templates.json"), templates);
            
            projectStructureTemplates.put(projectPath, templates);
        } catch (IOException e) {
            // Handle error
        }
    }
    
    public List<ProjectStructureConfig> getProjectStructureTemplates(Project project) {
        String projectPath = project.getBasePath();
        if (!projectStructureTemplates.containsKey(projectPath)) {
            loadConfiguration(project);
        }
        
        List<ProjectStructureConfig> templates = projectStructureTemplates.get(projectPath);
        if (templates == null || templates.isEmpty()) {
            templates = new ArrayList<>();
            templates.add(new ProjectStructureConfig());
            projectStructureTemplates.put(projectPath, templates);
        }
        
        return templates;
    }
} 