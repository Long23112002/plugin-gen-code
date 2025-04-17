package org.longg.nh.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration for project structure.
 * Contains a name and a list of root nodes representing the project structure.
 */
public class ProjectStructureConfig implements Serializable {
    private String configName;
    private List<ProjectStructureNode> rootNodes;

    public ProjectStructureConfig() {
        this.rootNodes = new ArrayList<>();
    }

    public ProjectStructureConfig(String configName) {
        this.configName = configName;
        this.rootNodes = new ArrayList<>();
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public List<ProjectStructureNode> getRootNodes() {
        return rootNodes;
    }

    public void setRootNodes(List<ProjectStructureNode> rootNodes) {
        this.rootNodes = rootNodes;
    }

    /**
     * Adds a root node to the configuration
     * 
     * @param node The node to add
     */
    public void addRootNode(ProjectStructureNode node) {
        if (this.rootNodes == null) {
            this.rootNodes = new ArrayList<>();
        }
        this.rootNodes.add(node);
    }

    /**
     * Removes a root node from the configuration
     * 
     * @param node The node to remove
     * @return true if the node was removed, false otherwise
     */
    public boolean removeRootNode(ProjectStructureNode node) {
        if (this.rootNodes != null) {
            return this.rootNodes.remove(node);
        }
        return false;
    }

    /**
     * Creates a default configuration with common project structure
     * 
     * @return A default configuration
     */
    public static ProjectStructureConfig createDefault() {
        ProjectStructureConfig config = new ProjectStructureConfig("Default Configuration");
        
        // Main source structure
        ProjectStructureNode src = new ProjectStructureNode("src", "src", ProjectStructureNode.NodeType.DIRECTORY);
        
        // Main code
        ProjectStructureNode main = new ProjectStructureNode("main", "src/main", ProjectStructureNode.NodeType.DIRECTORY);
        ProjectStructureNode java = new ProjectStructureNode("java", "src/main/java", ProjectStructureNode.NodeType.JAVA_SOURCE);
        ProjectStructureNode resources = new ProjectStructureNode("resources", "src/main/resources", ProjectStructureNode.NodeType.RESOURCE);
        
        main.addChild(java);
        main.addChild(resources);
        src.addChild(main);
        
        // Test code
        ProjectStructureNode test = new ProjectStructureNode("test", "src/test", ProjectStructureNode.NodeType.DIRECTORY);
        ProjectStructureNode testJava = new ProjectStructureNode("java", "src/test/java", ProjectStructureNode.NodeType.TEST);
        ProjectStructureNode testResources = new ProjectStructureNode("resources", "src/test/resources", ProjectStructureNode.NodeType.RESOURCE);
        
        test.addChild(testJava);
        test.addChild(testResources);
        src.addChild(test);
        
        config.addRootNode(src);
        
        return config;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectStructureConfig that = (ProjectStructureConfig) o;
        return Objects.equals(configName, that.configName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configName);
    }

    @Override
    public String toString() {
        return configName;
    }
} 