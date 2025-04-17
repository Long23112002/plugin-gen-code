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
    private boolean useDtoValidation;
    
    // Custom paths
    private String customDtoPath;
    private String customServicePath;
    private String customRepositoryPath;
    private String customControllerPath;
    private String customFilterPath;

    public ArchitectureConfig() {
        this.name = "Default";
        this.description = "Default architecture pattern";
        this.dtoPackage = "dto";
        this.servicePackage = "service";
        this.repositoryPackage = "repository";
        this.controllerPackage = "controller";
        this.filterPackage = "filter";
        this.useLombok = true;
        this.useDtoValidation = false;
        this.customDtoPath = "";
        this.customServicePath = "";
        this.customRepositoryPath = "";
        this.customControllerPath = "";
        this.customFilterPath = "";
    }

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
    
    public boolean isUseDtoValidation() {
        return useDtoValidation;
    }
    
    public void setUseDtoValidation(boolean useDtoValidation) {
        this.useDtoValidation = useDtoValidation;
    }
    
    public String getCustomDtoPath() {
        return customDtoPath;
    }
    
    public void setCustomDtoPath(String customDtoPath) {
        this.customDtoPath = customDtoPath;
    }
    
    public String getCustomServicePath() {
        return customServicePath;
    }
    
    public void setCustomServicePath(String customServicePath) {
        this.customServicePath = customServicePath;
    }
    
    public String getCustomRepositoryPath() {
        return customRepositoryPath;
    }
    
    public void setCustomRepositoryPath(String customRepositoryPath) {
        this.customRepositoryPath = customRepositoryPath;
    }
    
    public String getCustomControllerPath() {
        return customControllerPath;
    }
    
    public void setCustomControllerPath(String customControllerPath) {
        this.customControllerPath = customControllerPath;
    }
    
    public String getCustomFilterPath() {
        return customFilterPath;
    }
    
    public void setCustomFilterPath(String customFilterPath) {
        this.customFilterPath = customFilterPath;
    }
} 