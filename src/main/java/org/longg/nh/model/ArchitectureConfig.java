package org.longg.nh.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

/**
 * Configuration model for defining architectural patterns
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArchitectureConfig {

    private String name;
    private String description;
    private String dtoPackage;
    private String servicePackage;
    private String repositoryPackage;
    private String controllerPackage;
    private String filterPackage;
    private boolean useLombok;
    private Map<String, String> templates;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDtoPackage() {
        return dtoPackage;
    }

    public void setDtoPackage(String dtoPackage) {
        this.dtoPackage = dtoPackage;
    }

    public String getServicePackage() {
        return servicePackage;
    }

    public void setServicePackage(String servicePackage) {
        this.servicePackage = servicePackage;
    }

    public String getRepositoryPackage() {
        return repositoryPackage;
    }

    public void setRepositoryPackage(String repositoryPackage) {
        this.repositoryPackage = repositoryPackage;
    }

    public String getControllerPackage() {
        return controllerPackage;
    }

    public void setControllerPackage(String controllerPackage) {
        this.controllerPackage = controllerPackage;
    }

    public String getFilterPackage() {
        return filterPackage;
    }

    public void setFilterPackage(String filterPackage) {
        this.filterPackage = filterPackage;
    }

    public boolean isUseLombok() {
        return useLombok;
    }

    public void setUseLombok(boolean useLombok) {
        this.useLombok = useLombok;
    }

    public Map<String, String> getTemplates() {
        return templates;
    }

    public void setTemplates(Map<String, String> templates) {
        this.templates = templates;
    }
} 