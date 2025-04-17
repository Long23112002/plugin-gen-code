package org.longg.nh.dialog;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.longg.nh.model.ArchitectureConfig;
import org.longg.nh.model.ProjectStructureConfig;
import org.longg.nh.model.ProjectStructureNode;
import org.longg.nh.service.ConfigurationService;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectStructureDialog extends DialogWrapper {
    private final Project project;
    private final ConfigurationService configService;
    private JComboBox<String> templateSelector;
    private JBTextField templateNameField;
    private Tree structureTree;
    private DefaultTreeModel treeModel;
    private final Map<String, ProjectStructureConfig> templates;
    private ProjectStructureConfig currentTemplate;
    private JComboBox<ProjectStructureNode.NodeType> componentTypeComboBox;
    private JButton saveTemplateButton;
    private JButton deleteTemplateButton;
    private JButton addFolderButton;
    private JButton removeFolderButton;
    private JButton renameFolderButton;
    
    public ProjectStructureDialog(Project project) {
        super(project);
        this.project = project;
        this.configService = ConfigurationService.getInstance();
        
        // Convert List<ProjectStructureConfig> to Map<String, ProjectStructureConfig>
        List<ProjectStructureConfig> templateList = configService.getProjectStructureTemplates(project);
        this.templates = new HashMap<>();
        for (ProjectStructureConfig config : templateList) {
            templates.put(config.getConfigName(), config);
        }
        
        if (templates.isEmpty()) {
            // Create default template if none exists
            currentTemplate = createDefaultTemplate();
            templates.put("Default", currentTemplate);
            
            // Save template list
            List<ProjectStructureConfig> newTemplateList = new ArrayList<>(templates.values());
            configService.saveProjectStructureTemplates(project, newTemplateList);
        } else {
            currentTemplate = templates.values().iterator().next();
        }
        
        setTitle("Project Structure Configuration");
        setOKButtonText("Apply");
        setCancelButtonText("Cancel");
        init();
    }
    
    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());
        dialogPanel.setPreferredSize(new Dimension(800, 600));
        
        // Top panel with template selection
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel templatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        templatePanel.add(new JBLabel("Template:"));
        
        templateSelector = new JComboBox<>(templates.keySet().toArray(new String[0]));
        templateSelector.addActionListener(e -> {
            String templateName = (String) templateSelector.getSelectedItem();
            if (templateName != null) {
                currentTemplate = templates.get(templateName);
                updateTreeModel();
            }
        });
        templatePanel.add(templateSelector);
        
        JPanel templateNamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        templateNamePanel.add(new JBLabel("Template Name:"));
        templateNameField = new JBTextField(20);
        templateNameField.setText(templateSelector.getSelectedItem() != null ? 
                templateSelector.getSelectedItem().toString() : "");
        templateNamePanel.add(templateNameField);
        
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        saveTemplateButton = new JButton("Save Template");
        saveTemplateButton.addActionListener(e -> saveCurrentTemplate());
        actionPanel.add(saveTemplateButton);
        
        JButton newTemplateButton = new JButton("New Template");
        newTemplateButton.addActionListener(e -> createNewTemplate());
        actionPanel.add(newTemplateButton);
        
        deleteTemplateButton = new JButton("Delete Template");
        deleteTemplateButton.addActionListener(e -> deleteCurrentTemplate());
        actionPanel.add(deleteTemplateButton);
        
        JPanel controlsPanel = new JPanel(new GridLayout(3, 1));
        controlsPanel.add(templatePanel);
        controlsPanel.add(templateNamePanel);
        controlsPanel.add(actionPanel);
        topPanel.add(controlsPanel, BorderLayout.CENTER);
        
        // Main panel with tree and edit controls
        JPanel splitter = new JPanel(new GridLayout(1, 2));
        
        // Tree panel
        JPanel treePanel = new JPanel(new BorderLayout());
        treeModel = createTreeModel();
        structureTree = new Tree(treeModel);
        structureTree.setRootVisible(true);
        structureTree.setShowsRootHandles(true);
        structureTree.expandRow(0);
        
        treePanel.add(new JBScrollPane(structureTree), BorderLayout.CENTER);
        
        // Tree action buttons
        JPanel treeButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addFolderButton = new JButton("Add Folder");
        addFolderButton.setIcon(AllIcons.Actions.NewFolder);
        addFolderButton.addActionListener(e -> addFolder());
        treeButtonPanel.add(addFolderButton);
        
        removeFolderButton = new JButton("Remove");
        removeFolderButton.setIcon(AllIcons.Actions.DeleteTag);
        removeFolderButton.addActionListener(e -> removeFolder());
        removeFolderButton.setEnabled(false);
        treeButtonPanel.add(removeFolderButton);
        
        renameFolderButton = new JButton("Rename");
        renameFolderButton.setIcon(AllIcons.Actions.Edit);
        renameFolderButton.addActionListener(e -> renameFolder());
        renameFolderButton.setEnabled(false);
        treeButtonPanel.add(renameFolderButton);
        
        treePanel.add(treeButtonPanel, BorderLayout.SOUTH);
        
        // Node edit panel
        JPanel nodeEditPanel = new JPanel(new BorderLayout());
        nodeEditPanel.setBorder(JBUI.Borders.empty(10));
        
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        formPanel.add(new JBLabel("Node Name:"));
        JBTextField nameField = new JBTextField();
        formPanel.add(nameField);
        
        formPanel.add(new JBLabel("Path:"));
        JBTextField pathField = new JBTextField();
        formPanel.add(pathField);
        
        formPanel.add(new JBLabel("Type:"));
        componentTypeComboBox = new JComboBox<>(ProjectStructureNode.NodeType.values());
        componentTypeComboBox.setPreferredSize(new Dimension(200, 30));
        componentTypeComboBox.setEnabled(false);
        formPanel.add(componentTypeComboBox);
        
        nodeEditPanel.add(formPanel, BorderLayout.NORTH);
        
        JButton updateNodeButton = new JButton("Update Node");
        updateNodeButton.addActionListener(e -> {
            DefaultMutableTreeNode selectedNode = 
                    (DefaultMutableTreeNode) structureTree.getLastSelectedPathComponent();
            if (selectedNode != null && selectedNode.getUserObject() instanceof ProjectStructureNode) {
                ProjectStructureNode node = (ProjectStructureNode) selectedNode.getUserObject();
                node.setName(nameField.getText());
                node.setPath(pathField.getText());
                node.setType((ProjectStructureNode.NodeType) componentTypeComboBox.getSelectedItem());
                treeModel.nodeChanged(selectedNode);
            }
        });
        JPanel updatePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        updatePanel.add(updateNodeButton);
        nodeEditPanel.add(updatePanel, BorderLayout.SOUTH);
        
        // Update form when a node is selected
        structureTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode selectedNode = 
                    (DefaultMutableTreeNode) structureTree.getLastSelectedPathComponent();
            if (selectedNode != null && selectedNode.getUserObject() instanceof ProjectStructureNode) {
                ProjectStructureNode node = (ProjectStructureNode) selectedNode.getUserObject();
                nameField.setText(node.getName());
                pathField.setText(node.getPath());
                componentTypeComboBox.setSelectedItem(node.getType());
            } else {
                nameField.setText("");
                pathField.setText("");
                componentTypeComboBox.setSelectedIndex(0);
            }
        });
        
        splitter.add(treePanel);
        splitter.add(nodeEditPanel);
        
        dialogPanel.add(topPanel, BorderLayout.NORTH);
        dialogPanel.add(splitter, BorderLayout.CENTER);
        
        return dialogPanel;
    }
    
    private DefaultTreeModel createTreeModel() {
        if (currentTemplate == null) {
            currentTemplate = createDefaultTemplate();
        }
        
        // Create root node for the tree
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Project Structure");
        
        // Add the root nodes from the template to the tree
        for (ProjectStructureNode node : currentTemplate.getRootNodes()) {
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(node);
            rootNode.add(childNode);
            buildTreeNodes(childNode, node.getChildren());
        }
        
        return new DefaultTreeModel(rootNode);
    }
    
    private void buildTreeNodes(DefaultMutableTreeNode parent, List<ProjectStructureNode> children) {
        if (children == null) return;
        
        for (ProjectStructureNode child : children) {
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
            parent.add(childNode);
            buildTreeNodes(childNode, child.getChildren());
        }
    }
    
    private void updateTreeModel() {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Project Structure");
        
        // Add the root nodes from the template to the tree
        for (ProjectStructureNode node : currentTemplate.getRootNodes()) {
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(node);
            rootNode.add(childNode);
            buildTreeNodes(childNode, node.getChildren());
        }
        
        treeModel.setRoot(rootNode);
        structureTree.expandRow(0);
    }
    
    private void addFolder() {
        DefaultMutableTreeNode selectedNode = 
                (DefaultMutableTreeNode) structureTree.getLastSelectedPathComponent();
        if (selectedNode == null) {
            selectedNode = (DefaultMutableTreeNode) treeModel.getRoot();
        }
        
        Object userObject = selectedNode.getUserObject();
        if (userObject instanceof String) {
            // This is the root node, create a new root node in the template
            ProjectStructureNode newNode = new ProjectStructureNode();
            newNode.setName("New Folder");
            newNode.setPath("/new-folder");
            newNode.setType(ProjectStructureNode.NodeType.DIRECTORY);
            
            currentTemplate.addRootNode(newNode);
            
            DefaultMutableTreeNode newTreeNode = new DefaultMutableTreeNode(newNode);
            selectedNode.add(newTreeNode);
            treeModel.nodesWereInserted(selectedNode, new int[]{selectedNode.getChildCount() - 1});
        } else if (userObject instanceof ProjectStructureNode) {
            // This is a regular node, add a child
            ProjectStructureNode parentNode = (ProjectStructureNode) userObject;
            
            ProjectStructureNode newNode = new ProjectStructureNode();
            newNode.setName("New Folder");
            newNode.setPath(parentNode.getPath() + "/new-folder");
            newNode.setType(ProjectStructureNode.NodeType.DIRECTORY);
            
            parentNode.addChild(newNode);
            
            DefaultMutableTreeNode newTreeNode = new DefaultMutableTreeNode(newNode);
            selectedNode.add(newTreeNode);
            treeModel.nodesWereInserted(selectedNode, new int[]{selectedNode.getChildCount() - 1});
        }
    }
    
    private void removeFolder() {
        // Implementation for removing folder
    }
    
    private void renameFolder() {
        // Implementation for renaming folder
    }
    
    private ProjectStructureConfig createDefaultTemplate() {
        // Create and return a default template
        return ProjectStructureConfig.createDefault();
    }
    
    private void saveCurrentTemplate() {
        // Save current template
        List<ProjectStructureConfig> templateList = new ArrayList<>(templates.values());
        configService.saveProjectStructureTemplates(project, templateList);
    }
    
    private void createNewTemplate() {
        // Create new template
    }
    
    private void deleteCurrentTemplate() {
        // Delete current template
    }
} 