package org.longg.nh.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a node in the project structure tree.
 * Each node can have a name, path, type, and children nodes.
 */
public class ProjectStructureNode implements Serializable {
    private String name;
    private String path;
    private NodeType type;
    private List<ProjectStructureNode> children;

    /**
     * Types of nodes in the project structure
     */
    public enum NodeType {
        DIRECTORY("Directory"),
        JAVA_SOURCE("Java Source"),
        RESOURCE("Resource"),
        CONFIG("Configuration"),
        TEST("Test");

        private final String displayName;

        NodeType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public ProjectStructureNode() {
        this.children = new ArrayList<>();
        this.type = NodeType.DIRECTORY;
    }

    public ProjectStructureNode(String name, String path, NodeType type) {
        this.name = name;
        this.path = path;
        this.type = type;
        this.children = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public NodeType getType() {
        return type;
    }

    public void setType(NodeType type) {
        this.type = type;
    }

    public List<ProjectStructureNode> getChildren() {
        return children;
    }

    public void setChildren(List<ProjectStructureNode> children) {
        this.children = children;
    }

    /**
     * Adds a child node to this node
     * 
     * @param child The child node to add
     */
    public void addChild(ProjectStructureNode child) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(child);
    }

    /**
     * Removes a child node from this node
     * 
     * @param child The child node to remove
     * @return true if the child was removed, false otherwise
     */
    public boolean removeChild(ProjectStructureNode child) {
        if (this.children != null) {
            return this.children.remove(child);
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectStructureNode that = (ProjectStructureNode) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(path, that.path) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, path, type);
    }

    @Override
    public String toString() {
        return name;
    }
} 