package org.longg.nh.dialog;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.Nullable;
import org.longg.nh.model.ArchitectureConfig;
import org.longg.nh.model.ValidationOption;
import org.longg.nh.service.CodeGenerationService;
import org.longg.nh.util.JavaClassAnalyzer;
import org.longg.nh.util.JavaClassAnalyzer.ClassField;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import com.intellij.ui.components.JBLabel;
import java.io.File;

public class EntityCodeGeneratorDialog extends DialogWrapper {
    private final Project project;
    private final PsiClass entityClass;
    private final ArchitectureConfig config;
    private final List<ClassField> entityFields;
    private final CodeGenerationService codeGenerationService;
    private final List<PsiFile> existingGeneratedFiles = new ArrayList<>();

    private JBCheckBox generateDtoCheckbox;
    private JBCheckBox generateServiceCheckbox;
    private JBCheckBox generateRepositoryCheckbox;
    private JBCheckBox generateControllerCheckbox;
    private JBCheckBox generateFilterCheckbox;
    private JBCheckBox useDtoValidationCheckbox;
    private JBList<String> fieldsList;
    private JList<String> filterFieldsList;
    private JButton configureValidationsButton;
    private Map<String, ValidationOption> validationOptions = new HashMap<>();
    
    // Custom path fields
    private JTextField customDtoPathField;
    private JTextField customServicePathField;
    private JTextField customRepositoryPathField;
    private JTextField customControllerPathField;
    private JTextField customFilterPathField;
    private JTextField customDtoNameField;

    private JComboBox<String> architectureComboBox;
    private final List<String> projectFolders = new ArrayList<>();
    
    // Main UI components
    private JTabbedPane tabbedPane;

    // Adicionar campo para o pacote base
    private JTextField basePackageField;

    // Add these declarations to the class variables area
    private JComboBox<String> dtoFolderComboBox;
    private JComboBox<String> serviceFolderComboBox;
    private JComboBox<String> repositoryFolderComboBox;
    private JComboBox<String> controllerFolderComboBox;
    private JComboBox<String> filterFolderComboBox;

    public EntityCodeGeneratorDialog(Project project, PsiClass entityClass, ArchitectureConfig config) {
        super(project);
        this.project = project;
        this.entityClass = entityClass;
        this.config = config;
        this.entityFields = JavaClassAnalyzer.getClassFields(entityClass);
        this.codeGenerationService = new CodeGenerationService(project, config, entityClass);

        // We'll call scanProjectFolders after UI initialization
        findExistingGeneratedFiles();

        setTitle("Generate Code from Entity: " + entityClass.getName());
        setOKButtonText("Generate");
        setCancelButtonText("Cancel");
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setPreferredSize(new Dimension(700, 600));
        mainPanel.setBorder(JBUI.Borders.empty(10));

        // Create header panel
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBorder(JBUI.Borders.emptyBottom(10));
        JLabel headerLabel = new JLabel("Select components to generate for " + entityClass.getName());
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
        headerPanel.add(headerLabel);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Create content panel with tabs
        tabbedPane = new JTabbedPane();
        
        // Components tab
        JPanel componentsPanel = new JPanel(new BorderLayout());
        componentsPanel.setBorder(JBUI.Borders.empty(10));
        
        // Create checkboxes with icons
        generateDtoCheckbox = new JBCheckBox("DTO");
        generateDtoCheckbox.setIcon(AllIcons.FileTypes.Java);
        generateDtoCheckbox.setToolTipText("Generate Data Transfer Object");
        generateDtoCheckbox.setSelected(true);
        
        generateServiceCheckbox = new JBCheckBox("Service");
        generateServiceCheckbox.setIcon(AllIcons.Nodes.Class);
        generateServiceCheckbox.setToolTipText("Generate Service layer");
        generateServiceCheckbox.setSelected(true);
        
        generateRepositoryCheckbox = new JBCheckBox("Repository");
        generateRepositoryCheckbox.setIcon(AllIcons.Nodes.Interface);
        generateRepositoryCheckbox.setToolTipText("Generate Repository interface");
        generateRepositoryCheckbox.setSelected(true);
        
        generateControllerCheckbox = new JBCheckBox("Controller");
        generateControllerCheckbox.setIcon(AllIcons.Nodes.Class);
        generateControllerCheckbox.setToolTipText("Generate REST Controller");
        generateControllerCheckbox.setSelected(true);
        
        generateFilterCheckbox = new JBCheckBox("Filter");
        generateFilterCheckbox.setIcon(AllIcons.Nodes.Class);
        generateFilterCheckbox.setToolTipText("Generate Filter parameters");
        generateFilterCheckbox.setSelected(false);
        
        useDtoValidationCheckbox = new JBCheckBox("Use DTO Validation");
        useDtoValidationCheckbox.setIcon(AllIcons.Actions.CheckMulticaret);
        useDtoValidationCheckbox.setToolTipText("Add validation annotations to DTO fields");
        useDtoValidationCheckbox.setSelected(config.isUseDtoValidation());
        
        // Enable/disable validation checkbox based on DTO checkbox
        generateDtoCheckbox.addActionListener(e -> {
            useDtoValidationCheckbox.setEnabled(generateDtoCheckbox.isSelected());
        });

        // Add checkboxes to a panel
        JPanel checkboxesPanel = new JPanel(new GridLayout(6, 1, 0, 10));
        checkboxesPanel.setBorder(JBUI.Borders.empty(10));
        checkboxesPanel.add(generateDtoCheckbox);
        checkboxesPanel.add(generateServiceCheckbox);
        checkboxesPanel.add(generateRepositoryCheckbox);
        checkboxesPanel.add(generateControllerCheckbox);
        checkboxesPanel.add(generateFilterCheckbox);
        checkboxesPanel.add(useDtoValidationCheckbox);
        
        componentsPanel.add(checkboxesPanel, BorderLayout.WEST);
        tabbedPane.addTab("Components", componentsPanel);

        // Fields tab
        JPanel fieldsPanel = new JPanel(new BorderLayout());
        fieldsPanel.setBorder(JBUI.Borders.empty(10));

        // Create field list for DTO generation
        String[] fieldNames = entityFields.stream()
                .map(ClassField::getName)
                .toArray(String[]::new);

        fieldsList = new JBList<>(fieldNames);
        fieldsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fieldsList.setVisibleRowCount(15);
        fieldsList.setSelectionInterval(0, fieldNames.length - 1);
        JBScrollPane fieldsScrollPane = new JBScrollPane(fieldsList);
        fieldsScrollPane.setBorder(BorderFactory.createTitledBorder("Select fields for DTO"));

        // Add configure validations button
        configureValidationsButton = new JButton("Configure Field Validations");
        configureValidationsButton.setEnabled(config.isUseDtoValidation());
        configureValidationsButton.addActionListener(e -> configureValidations());
        
        // Create a panel for DTO fields and validation button
        JPanel dtoPanel = new JPanel(new BorderLayout());
        
        // Add DTO naming panel at the top
        dtoPanel.add(createDtoNamingPanel(), BorderLayout.NORTH);
        
        // Add the fields scroll pane to the center
        dtoPanel.add(fieldsScrollPane, BorderLayout.CENTER);
        
        // Add validation button at the bottom if applicable
        dtoPanel.add(configureValidationsButton, BorderLayout.SOUTH);

        // Create field list for Filter generation
        filterFieldsList = new JList<>(fieldNames);
        filterFieldsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        filterFieldsList.setVisibleRowCount(15);
        JBScrollPane filterFieldsScrollPane = new JBScrollPane(filterFieldsList);
        filterFieldsScrollPane.setBorder(BorderFactory.createTitledBorder("Select fields for Filter"));

        // Enable/disable filter fields based on checkbox
        generateFilterCheckbox.addActionListener(e -> {
            filterFieldsList.setEnabled(generateFilterCheckbox.isSelected());
            if (generateFilterCheckbox.isSelected()) {
                filterFieldsList.setSelectionInterval(0, fieldNames.length - 1);
            }
        });
        filterFieldsList.setEnabled(false);

        // Add field lists to a panel
        JPanel fieldListsPanel = new JPanel(new GridLayout(2, 1, 0, 10));
        fieldListsPanel.add(dtoPanel);
        fieldListsPanel.add(filterFieldsScrollPane);
        
        fieldsPanel.add(fieldListsPanel, BorderLayout.CENTER);
        tabbedPane.addTab("Fields", fieldsPanel);
        
        // Link validation checkbox to configure button
        useDtoValidationCheckbox.addActionListener(e -> {
            configureValidationsButton.setEnabled(useDtoValidationCheckbox.isSelected());
        });

        // Custom Paths tab
        JPanel pathsPanel = new JPanel(new GridBagLayout());
        pathsPanel.setBorder(JBUI.Borders.empty(10));
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = JBUI.insets(5);
        
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.weightx = 1.0;
        
        pathsPanel.add(createArchitecturePanel(), c);
        
        c.gridwidth = 1;
        
        // DTO Path
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.0;
        pathsPanel.add(new JLabel("DTO Path:"), c);
        
        JPanel dtoPathPanel = new JPanel(new BorderLayout());
        customDtoPathField = new JTextField(config.getCustomDtoPath(), 25);
        dtoPathPanel.add(customDtoPathField, BorderLayout.CENTER);
        
        // Initialize the combo box
        scanProjectFolders();
        dtoFolderComboBox = new JComboBox<>(projectFolders.toArray(new String[0]));
        dtoFolderComboBox.setEditable(true);
        dtoFolderComboBox.addActionListener(e -> {
            if (dtoFolderComboBox.getSelectedItem() != null) {
                customDtoPathField.setText(dtoFolderComboBox.getSelectedItem().toString());
            }
        });
        dtoPathPanel.add(dtoFolderComboBox, BorderLayout.EAST);
        
        c.gridx = 1;
        c.weightx = 1.0;
        pathsPanel.add(dtoPathPanel, c);
        
        // Service Path
        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0.0;
        pathsPanel.add(new JLabel("Service Path:"), c);
        
        JPanel servicePathPanel = new JPanel(new BorderLayout());
        customServicePathField = new JTextField(config.getCustomServicePath(), 25);
        servicePathPanel.add(customServicePathField, BorderLayout.CENTER);
        
        serviceFolderComboBox = new JComboBox<>(projectFolders.toArray(new String[0]));
        serviceFolderComboBox.setEditable(true);
        serviceFolderComboBox.addActionListener(e -> {
            if (serviceFolderComboBox.getSelectedItem() != null) {
                customServicePathField.setText(serviceFolderComboBox.getSelectedItem().toString());
            }
        });
        servicePathPanel.add(serviceFolderComboBox, BorderLayout.EAST);
        
        c.gridx = 1;
        c.weightx = 1.0;
        pathsPanel.add(servicePathPanel, c);
        
        // Repository Path
        c.gridx = 0;
        c.gridy = 3;
        c.weightx = 0.0;
        pathsPanel.add(new JLabel("Repository Path:"), c);
        
        JPanel repositoryPathPanel = new JPanel(new BorderLayout());
        customRepositoryPathField = new JTextField(config.getCustomRepositoryPath(), 25);
        repositoryPathPanel.add(customRepositoryPathField, BorderLayout.CENTER);
        
        repositoryFolderComboBox = new JComboBox<>(projectFolders.toArray(new String[0]));
        repositoryFolderComboBox.setEditable(true);
        repositoryFolderComboBox.addActionListener(e -> {
            if (repositoryFolderComboBox.getSelectedItem() != null) {
                customRepositoryPathField.setText(repositoryFolderComboBox.getSelectedItem().toString());
            }
        });
        repositoryPathPanel.add(repositoryFolderComboBox, BorderLayout.EAST);
        
        c.gridx = 1;
        c.weightx = 1.0;
        pathsPanel.add(repositoryPathPanel, c);
        
        // Controller Path
        c.gridx = 0;
        c.gridy = 4;
        c.weightx = 0.0;
        pathsPanel.add(new JLabel("Controller Path:"), c);
        
        JPanel controllerPathPanel = new JPanel(new BorderLayout());
        customControllerPathField = new JTextField(config.getCustomControllerPath(), 25);
        controllerPathPanel.add(customControllerPathField, BorderLayout.CENTER);
        
        controllerFolderComboBox = new JComboBox<>(projectFolders.toArray(new String[0]));
        controllerFolderComboBox.setEditable(true);
        controllerFolderComboBox.addActionListener(e -> {
            if (controllerFolderComboBox.getSelectedItem() != null) {
                customControllerPathField.setText(controllerFolderComboBox.getSelectedItem().toString());
            }
        });
        controllerPathPanel.add(controllerFolderComboBox, BorderLayout.EAST);
        
        c.gridx = 1;
        c.weightx = 1.0;
        pathsPanel.add(controllerPathPanel, c);
        
        // Filter Path
        c.gridx = 0;
        c.gridy = 5;
        c.weightx = 0.0;
        pathsPanel.add(new JLabel("Filter Path:"), c);
        
        JPanel filterPathPanel = new JPanel(new BorderLayout());
        customFilterPathField = new JTextField(config.getCustomFilterPath(), 25);
        filterPathPanel.add(customFilterPathField, BorderLayout.CENTER);
        
        filterFolderComboBox = new JComboBox<>(projectFolders.toArray(new String[0]));
        filterFolderComboBox.setEditable(true);
        filterFolderComboBox.addActionListener(e -> {
            if (filterFolderComboBox.getSelectedItem() != null) {
                customFilterPathField.setText(filterFolderComboBox.getSelectedItem().toString());
            }
        });
        filterPathPanel.add(filterFolderComboBox, BorderLayout.EAST);
        
        c.gridx = 1;
        c.weightx = 1.0;
        pathsPanel.add(filterPathPanel, c);
        
        // Add explanation text
        c.gridx = 0;
        c.gridy = 6;
        c.gridwidth = 2;
        c.weightx = 1.0;
        JLabel pathExplanationLabel = new JLabel("<html><i>Leave blank to use default paths based on package names.</i></html>");
        pathsPanel.add(pathExplanationLabel, c);
        
        // Add some padding at the bottom
        c.gridy = 7;
        c.weighty = 1.0;
        pathsPanel.add(new JPanel(), c);
        
        tabbedPane.addTab("Custom Paths", pathsPanel);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Add validation
        generateDtoCheckbox.addActionListener(e -> validateInput());
        generateServiceCheckbox.addActionListener(e -> validateInput());
        generateRepositoryCheckbox.addActionListener(e -> validateInput());
        generateControllerCheckbox.addActionListener(e -> validateInput());
        generateFilterCheckbox.addActionListener(e -> validateInput());

        // Add footer with author name
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footerPanel.setBorder(JBUI.Borders.emptyTop(10));
        JLabel authorLabel = new JLabel("Nguyễn Hải Long");
        authorLabel.setFont(authorLabel.getFont().deriveFont(Font.ITALIC, 10f));
        authorLabel.setForeground(Color.GRAY);
        footerPanel.add(authorLabel);
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    private void validateInput() {
        // Validate that at least one component is selected for generation
        if (!generateDtoCheckbox.isSelected() &&
            !generateServiceCheckbox.isSelected() &&
            !generateRepositoryCheckbox.isSelected() &&
            !generateControllerCheckbox.isSelected() &&
            !generateFilterCheckbox.isSelected()) {
            Messages.showErrorDialog(
                "Please select at least one component to generate",
                "No Selection"
            );
            throw new IllegalStateException("No components selected for generation");
        }
        
        // For DTO, validate that at least one field is selected
        if (generateDtoCheckbox.isSelected() && fieldsList.getSelectedIndices().length == 0) {
            Messages.showErrorDialog(
                "Please select at least one field for the DTO",
                "No DTO Fields Selected"
            );
            throw new IllegalStateException("No fields selected for DTO generation");
        }
        
        // For Filter, validate that at least one field is selected
        if (generateFilterCheckbox.isSelected() && filterFieldsList.getSelectedIndices().length == 0) {
            Messages.showErrorDialog(
                "Please select at least one field for the Filter",
                "No Filter Fields Selected"
            );
            throw new IllegalStateException("No fields selected for Filter generation");
        }
        
        // Validate custom paths if provided
        validateCustomPaths();
    }
    
    /**
     * Validates that the custom paths provided are valid or can be created
     */
    private void validateCustomPaths() {
        List<String> pathsToCheck = new ArrayList<>();
        
        // Only check paths for components that are selected
        if (generateDtoCheckbox.isSelected() && !customDtoPathField.getText().trim().isEmpty()) {
            pathsToCheck.add(customDtoPathField.getText().trim());
        }
        
        if (generateServiceCheckbox.isSelected() && !customServicePathField.getText().trim().isEmpty()) {
            pathsToCheck.add(customServicePathField.getText().trim());
        }
        
        if (generateRepositoryCheckbox.isSelected() && !customRepositoryPathField.getText().trim().isEmpty()) {
            pathsToCheck.add(customRepositoryPathField.getText().trim());
        }
        
        if (generateControllerCheckbox.isSelected() && !customControllerPathField.getText().trim().isEmpty()) {
            pathsToCheck.add(customControllerPathField.getText().trim());
        }
        
        if (generateFilterCheckbox.isSelected() && !customFilterPathField.getText().trim().isEmpty()) {
            pathsToCheck.add(customFilterPathField.getText().trim());
        }
        
        // Now check that each path is valid or can be created
        for (String path : pathsToCheck) {
            if (!isPathValidOrCreatable(path)) {
                Messages.showErrorDialog(
                    "The path '" + path + "' is not valid or cannot be created.",
                    "Invalid Path"
                );
                throw new IllegalStateException("Invalid path: " + path);
            }
        }
    }
    
    /**
     * Checks if a path is valid or can be created
     */
    private boolean isPathValidOrCreatable(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        
        // Check for invalid characters in path
        if (path.contains("\\") || path.contains(":") || path.contains("?") || 
            path.contains("\"") || path.contains("<") || path.contains(">") || 
            path.contains("|") || path.contains("*")) {
            return false;
        }
        
        // Check if path already exists
        String fullPath = project.getBasePath() + "/" + path;
        File directory = new File(fullPath);
        if (directory.exists()) {
            return directory.isDirectory();
        }
        
        // If it doesn't exist, check if parent directory exists and is writable
        File parentDir = directory.getParentFile();
        return parentDir != null && (parentDir.exists() || isPathValidOrCreatable(getRelativePath(parentDir)));
    }
    
    /**
     * Gets the relative path of a directory from the project root
     */
    private String getRelativePath(File dir) {
        try {
            String projectPath = project.getBasePath();
            String dirPath = dir.getCanonicalPath();
            
            if (projectPath != null && dirPath.startsWith(projectPath)) {
                return dirPath.substring(projectPath.length() + 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return dir.getPath();
    }

    @Override
    protected void doOKAction() {
        // Run validation first
        validateInput();
        
        // We don't need to check for redundant files since we're removing that UI
        
        try {
            // We must wrap the entire operation in a WriteCommandAction
            WriteCommandAction.writeCommandAction(project)
                .run(() -> {
                    try {
                        generateSelectedComponents();
                        
                        // Auto-run IDE with the generated code
                        autoRunIDE();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Messages.showErrorDialog(
                            "Error generating code: " + e.getMessage(),
                            "Generation Error"
                        );
                    }
                });
            
            super.doOKAction();
        } catch (Exception e) {
            e.printStackTrace();
            Messages.showErrorDialog(
                "Error generating code: " + e.getMessage(),
                "Generation Error"
            );
        }
    }

    private void generateSelectedComponents() {
        Map<String, PsiClass> generatedClasses = new HashMap<>();

        // Update config with UI settings
        config.setUseDtoValidation(useDtoValidationCheckbox.isSelected());
        config.setCustomDtoPath(customDtoPathField.getText().trim());
        config.setCustomServicePath(customServicePathField.getText().trim());
        config.setCustomRepositoryPath(customRepositoryPathField.getText().trim());
        config.setCustomControllerPath(customControllerPathField.getText().trim());
        config.setCustomFilterPath(customFilterPathField.getText().trim());

        // Ensure directories exist
        List<String> directoriesToCreate = new ArrayList<>();
        if (!config.getCustomDtoPath().isEmpty()) directoriesToCreate.add(config.getCustomDtoPath());
        if (!config.getCustomServicePath().isEmpty()) directoriesToCreate.add(config.getCustomServicePath());
        if (!config.getCustomRepositoryPath().isEmpty()) directoriesToCreate.add(config.getCustomRepositoryPath());
        if (!config.getCustomControllerPath().isEmpty()) directoriesToCreate.add(config.getCustomControllerPath());
        if (!config.getCustomFilterPath().isEmpty()) directoriesToCreate.add(config.getCustomFilterPath());
        
        // Create all required directories
        for (String dir : directoriesToCreate) {
            createDirectoryIfNeeded(dir);
        }

        // Collect selected DTO fields
        Set<String> selectedDtoFields = new HashSet<>();
        if (generateDtoCheckbox.isSelected()) {
            for (int index : fieldsList.getSelectedIndices()) {
                selectedDtoFields.add(entityFields.get(index).getName());
            }
        }

        // Collect selected filter fields
        Set<String> selectedFilterFields = new HashSet<>();
        if (generateFilterCheckbox.isSelected()) {
            for (int index : filterFieldsList.getSelectedIndices()) {
                selectedFilterFields.add(entityFields.get(index).getName());
            }
        }

        // Generate DTO
        if (generateDtoCheckbox.isSelected() && !selectedDtoFields.isEmpty()) {
            String customDtoName = customDtoNameField.getText().trim();
            PsiClass dtoClass = customDtoName.isEmpty() 
                ? codeGenerationService.generateDto(selectedDtoFields, 
                                                 config.isUseDtoValidation() ? validationOptions : null)
                : codeGenerationService.generateDto(selectedDtoFields, 
                                                 config.isUseDtoValidation() ? validationOptions : null,
                                                 customDtoName);
            PsiFile dtoFile = dtoClass.getContainingFile();
            saveFile(dtoFile, config.getDtoPackage(), config.getCustomDtoPath());
            generatedClasses.put("dto", dtoClass);
        }

        // Generate Repository
        if (generateRepositoryCheckbox.isSelected()) {
            PsiClass repositoryClass = codeGenerationService.generateRepository(selectedFilterFields);
            PsiFile repositoryFile = repositoryClass.getContainingFile();
            saveFile(repositoryFile, config.getRepositoryPackage(), config.getCustomRepositoryPath());
            generatedClasses.put("repository", repositoryClass);
        }

        // Generate Service
        if (generateServiceCheckbox.isSelected()) {
            PsiClass serviceClass = codeGenerationService.generateService(generateRepositoryCheckbox.isSelected());
            PsiFile serviceFile = serviceClass.getContainingFile();
            saveFile(serviceFile, config.getServicePackage(), config.getCustomServicePath());
            generatedClasses.put("service", serviceClass);
        }

        // Generate Controller
        if (generateControllerCheckbox.isSelected()) {
            PsiClass controllerClass = codeGenerationService.generateController(generateServiceCheckbox.isSelected());
            PsiFile controllerFile = controllerClass.getContainingFile();
            saveFile(controllerFile, config.getControllerPackage(), config.getCustomControllerPath());
            generatedClasses.put("controller", controllerClass);
        }

        // Generate Filter
        if (generateFilterCheckbox.isSelected() && !selectedFilterFields.isEmpty()) {
            PsiClass filterClass = codeGenerationService.generateFilter(selectedFilterFields);
            PsiFile filterFile = filterClass.getContainingFile();
            saveFile(filterFile, config.getFilterPackage(), config.getCustomFilterPath());
            generatedClasses.put("filter", filterClass);
        }

        // Open generated files in editor
        for (Map.Entry<String, PsiClass> entry : generatedClasses.entrySet()) {
            PsiFile file = entry.getValue().getContainingFile();
            if (file.getVirtualFile() != null) {
                FileEditorManager.getInstance(project).openFile(file.getVirtualFile(), true);
            }
        }
    }

    private void saveFile(PsiFile file, String packageSuffix, String customPath) {
        // Primeiro criar os diretórios se ainda não existirem
        if (customPath != null && !customPath.isEmpty()) {
            createDirectoryIfNeeded(customPath);
        }
        
        String packageName = ((PsiJavaFile) file).getPackageName();
        final PsiDirectory[] baseDir = {null};
        
        // Check if custom path is provided and valid
        if (customPath != null && !customPath.isEmpty()) {
            PsiDirectory customDir = ApplicationManager.getApplication().runReadAction(
                (Computable<PsiDirectory>) () -> {
                    PsiManager psiManager = PsiManager.getInstance(project);
                    VirtualFile targetDir = project.getBaseDir().findFileByRelativePath(customPath);
                    // Verify that the directory exists
                    if (targetDir != null && targetDir.exists() && targetDir.isDirectory()) {
                        return psiManager.findDirectory(targetDir);
                    }
                    return null;
                }
            );
            
            if (customDir != null) {
                // If custom path exists, we need to create or find package structure
                final PsiDirectory finalCustomDir = customDir;
                baseDir[0] = WriteCommandAction.writeCommandAction(project)
                    .compute(() -> {
                        PsiDirectory currentDir = finalCustomDir;
                        String[] packageParts = packageName.split("\\.");
                        for (String part : packageParts) {
                            PsiDirectory existingDir = currentDir.findSubdirectory(part);
                            if (existingDir != null) {
                                currentDir = existingDir;
                            } else {
                                currentDir = currentDir.createSubdirectory(part);
                            }
                        }
                        return currentDir;
                    });
            } else {
                // If we couldn't find the directory, we need to create it through VFS
                try {
                    VirtualFile projectRoot = project.getBaseDir();
                    // Create nested directories one by one
                    String[] pathParts = customPath.split("/");
                    final VirtualFile[] currentDir = {projectRoot};
                    
                    WriteCommandAction.writeCommandAction(project)
                        .run(() -> {
                            try {
                                for (String part : pathParts) {
                                    if (part.isEmpty()) continue;
                                    
                                    VirtualFile childDir = currentDir[0].findChild(part);
                                    if (childDir == null || !childDir.exists() || !childDir.isDirectory()) {
                                        // Create directory through VFS
                                        currentDir[0] = currentDir[0].createChildDirectory(this, part);
                                    } else {
                                        currentDir[0] = childDir;
                                    }
                                }
                            } catch (Exception e) {
                                Messages.showErrorDialog(
                                    "Failed to create directory structure: " + e.getMessage(),
                                    "Directory Creation Error"
                                );
                                e.printStackTrace();
                            }
                        });
                    
                    // Now that we have created the directory, find it again
                    VirtualFile targetDir = project.getBaseDir().findFileByRelativePath(customPath);
                    if (targetDir != null && targetDir.exists()) {
                        PsiManager psiManager = PsiManager.getInstance(project);
                        final PsiDirectory psiDir = psiManager.findDirectory(targetDir);
                        
                        // Create package structure
                        if (psiDir != null) {
                            baseDir[0] = WriteCommandAction.writeCommandAction(project)
                                .compute(() -> {
                                    PsiDirectory dir = psiDir;
                                    String[] packageParts = packageName.split("\\.");
                                    for (String part : packageParts) {
                                        PsiDirectory existingDir = dir.findSubdirectory(part);
                                        if (existingDir != null) {
                                            dir = existingDir;
                                        } else {
                                            dir = dir.createSubdirectory(part);
                                        }
                                    }
                                    return dir;
                                });
                        }
                    }
                } catch (Exception e) {
                    Messages.showErrorDialog(
                        "Failed to create directory structure: " + e.getMessage(),
                        "Directory Creation Error"
                    );
                    e.printStackTrace();
                }
            }
        }
        
        // Fall back to default package-based path if custom path is invalid
        if (baseDir[0] == null) {
            String[] packageParts = packageName.split("\\.");
            PsiDirectory defaultBaseDir = ApplicationManager.getApplication().runReadAction(
                (Computable<PsiDirectory>) () -> {
                    JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
                    String basePackage = packageName.substring(0, packageName.lastIndexOf('.'));
                    
                    PsiPackage psiPackage = psiFacade.findPackage(basePackage);
                    if (psiPackage != null && psiPackage.getDirectories().length > 0) {
                        return psiPackage.getDirectories()[0];
                    }
                    
                    return null;
                }
            );
            
            if (defaultBaseDir == null) {
                // Try to find source directory
                PsiDirectory sourceDir = ApplicationManager.getApplication().runReadAction(
                    (Computable<PsiDirectory>) () -> {
                        PsiManager psiManager = PsiManager.getInstance(project);
                        return psiManager.findDirectory(
                                project.getBaseDir().findFileByRelativePath("src/main/java")
                        );
                    }
                );
                
                if (sourceDir == null) {
                    throw new RuntimeException("Cannot find source directory");
                }
                
                final PsiDirectory finalSourceDir = sourceDir;
                baseDir[0] = WriteCommandAction.writeCommandAction(project)
                    .compute(() -> {
                        PsiDirectory currentDir = finalSourceDir;
                        // Create package directories
                        for (String part : packageParts) {
                            PsiDirectory existingDir = currentDir.findSubdirectory(part);
                            if (existingDir != null) {
                                currentDir = existingDir;
                            } else {
                                currentDir = currentDir.createSubdirectory(part);
                            }
                        }
                        return currentDir;
                    });
            } else {
                // Create component package directory
                final PsiDirectory finalBaseDir = defaultBaseDir;
                baseDir[0] = WriteCommandAction.writeCommandAction(project)
                    .compute(() -> {
                        String componentPackage = packageParts[packageParts.length - 1];
                        PsiDirectory componentDir = finalBaseDir.findSubdirectory(componentPackage);
                        if (componentDir != null) {
                            return componentDir;
                        } else {
                            return finalBaseDir.createSubdirectory(componentPackage);
                        }
                    });
            }
        }
        
        // Handle implementation files
        if (file.getName().endsWith("Impl.java")) {
            final PsiDirectory finalBaseDir = baseDir[0];
            baseDir[0] = WriteCommandAction.writeCommandAction(project)
                .compute(() -> {
                    PsiDirectory implDir = finalBaseDir.findSubdirectory("impl");
                    if (implDir == null) {
                        implDir = finalBaseDir.createSubdirectory("impl");
                    }
                    return implDir;
                });
        }
        
        // Make sure the file doesn't exist already
        String fileName = file.getName();
        final PsiDirectory finalBaseDir = baseDir[0];
        PsiFile existingFile = ApplicationManager.getApplication().runReadAction(
            (Computable<PsiFile>) () -> finalBaseDir.findFile(fileName)
        );
        
        if (existingFile != null) {
            // Check if the file content is different
            boolean isDifferent = !existingFile.getText().equals(file.getText());
            
            if (isDifferent) {
                // Skip asking and just overwrite the file
                WriteCommandAction.writeCommandAction(project)
                    .run(() -> existingFile.delete());
            } else {
                // File exists with identical content, no need to regenerate, just silently skip
                return;
            }
        }
        
        // Add file to directory
        final PsiDirectory finalBaseDir2 = baseDir[0];
        WriteCommandAction.writeCommandAction(project)
            .run(() -> finalBaseDir2.add(file));
    }

    private void configureValidations() {
        // Get selected DTO fields
        Set<String> selectedDtoFields = new HashSet<>();
        for (int index : fieldsList.getSelectedIndices()) {
            selectedDtoFields.add(entityFields.get(index).getName());
        }

        // Prepare validation options
        for (ClassField field : entityFields) {
            if (selectedDtoFields.contains(field.getName())) {
                if (!validationOptions.containsKey(field.getName())) {
                    ValidationOption option = new ValidationOption(field.getName(), field.getType());
                    validationOptions.put(field.getName(), option);
                }
            }
        }
        
        // Create and show table with validation options
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1; // Only the "Configure" column is editable
            }
        };
        
        model.addColumn("Field");
        model.addColumn("Actions");
        
        JTable table = new JTable(model);
        table.setRowHeight(30);
        
        // Add selected fields to table
        for (ClassField field : entityFields) {
            if (selectedDtoFields.contains(field.getName())) {
                model.addRow(new Object[]{field.getName(), "Configure"});
            }
        }
        
        // Add button to each row in the "Configure" column
        table.getColumnModel().getColumn(1).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(1).setCellEditor(new ButtonEditor(new JCheckBox(), field -> {
            // Find the field in entityFields
            ClassField classField = entityFields.stream()
                    .filter(f -> f.getName().equals(field))
                    .findFirst()
                    .orElse(null);
                    
            if (classField != null) {
                ValidationOption option = validationOptions.get(field);
                ValidationOptionsDialog dialog = new ValidationOptionsDialog(
                    option, 
                    classField.getType(), 
                    classField.isCollection()
                );
                
                if (dialog.showAndGet()) {
                    // Option was updated in the dialog
                }
            }
        }));
        
        // Create dialog
        DialogWrapper dialog = new DialogWrapper(true) {
            {
                init();
                setTitle("Configure Field Validations");
            }
            
            @Nullable
            @Override
            protected JComponent createCenterPanel() {
                JPanel panel = new JPanel(new BorderLayout());
                panel.setPreferredSize(new Dimension(400, 300));
                panel.add(new JScrollPane(table), BorderLayout.CENTER);
                return panel;
            }
        };
        
        dialog.show();
    }

    // Button renderer and editor for the validation options table
    private static class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }
    
    private static class ButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private String fieldName;
        private boolean isPushed;
        private final Consumer<String> callback;
        
        public ButtonEditor(JCheckBox checkBox, Consumer<String> callback) {
            super(checkBox);
            this.callback = callback;
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (isSelected) {
                button.setForeground(table.getSelectionForeground());
                button.setBackground(table.getSelectionBackground());
            } else {
                button.setForeground(table.getForeground());
                button.setBackground(UIManager.getColor("Button.background"));
            }
            
            button.setText(value == null ? "" : value.toString());
            fieldName = table.getValueAt(row, 0).toString();
            isPushed = true;
            return button;
        }
        
        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                callback.accept(fieldName);
            }
            isPushed = false;
            return "Configure";
        }
        
        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }

    // In the createCenterPanel method, after creating the fieldsPanel section
    // Add DTO naming panel to the DTO panel section
    private JPanel createDtoNamingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("DTO Naming"));
        
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputPanel.add(new JBLabel("Custom DTO Name:"));
        
        customDtoNameField = new JTextField(20);
        customDtoNameField.setToolTipText("Leave blank to use default naming (EntityNameDto)");
        inputPanel.add(customDtoNameField);
        
        panel.add(inputPanel, BorderLayout.CENTER);
        return panel;
    }

    private void scanProjectFolders() {
        projectFolders.clear();
        
        // Add commonly used folders directly based on common Java package standards
        projectFolders.add("");
        projectFolders.add("src/main/java");
        
        // Get the base package as folders
        String basePackage = basePackageField.getText();
        if (basePackage == null || basePackage.isEmpty()) {
            basePackage = "com.example";
        }
        
        String basePackagePath = "src/main/java/" + basePackage.replace('.', '/');
        projectFolders.add(basePackagePath);
        
        // Add common module paths
        projectFolders.add(basePackagePath + "/dto");
        projectFolders.add(basePackagePath + "/model");
        projectFolders.add(basePackagePath + "/service");
        projectFolders.add(basePackagePath + "/repository");
        projectFolders.add(basePackagePath + "/controller");
        projectFolders.add(basePackagePath + "/filter");
        
        // Add DDD style paths
        projectFolders.add(basePackagePath + "/domain/dto");
        projectFolders.add(basePackagePath + "/domain/service");
        projectFolders.add(basePackagePath + "/domain/repository");
        projectFolders.add(basePackagePath + "/application/controller");
        projectFolders.add(basePackagePath + "/infrastructure/filter");
    }

    private void createDirectoryIfNeeded(String path) {
        if (path == null || path.trim().isEmpty()) return;
        
        String fullPath = project.getBasePath() + "/" + path;
        File directory = new File(fullPath);
        if (!directory.exists()) {
            try {
                if (directory.mkdirs()) {
                    System.out.println("Created directory: " + fullPath);
                } else {
                    System.out.println("Failed to create directory: " + fullPath);
                    // If mkdirs fails, try to create it via the VFS API
                    createDirectoryViaVFS(path);
                }
            } catch (Exception e) {
                e.printStackTrace();
                // If Java IO fails, try to create via the VFS API
                createDirectoryViaVFS(path);
            }
        } else if (!directory.isDirectory()) {
            throw new IllegalStateException("Path exists but is not a directory: " + fullPath);
        }
    }
    
    /**
     * Creates a directory using IntelliJ's VFS API, which is more reliable in some cases
     */
    private void createDirectoryViaVFS(String path) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                VirtualFile projectRoot = project.getBaseDir();
                // Create nested directories one by one
                String[] pathParts = path.split("/");
                VirtualFile currentDir = projectRoot;
                
                for (String part : pathParts) {
                    if (part.isEmpty()) continue;
                    
                    VirtualFile childDir = currentDir.findChild(part);
                    if (childDir == null || !childDir.exists() || !childDir.isDirectory()) {
                        // Create directory through VFS
                        currentDir = currentDir.createChildDirectory(this, part);
                    } else {
                        currentDir = childDir;
                    }
                }
            } catch (Exception e) {
                Messages.showErrorDialog(
                    "Failed to create directory structure: " + e.getMessage(),
                    "Directory Creation Error"
                );
                e.printStackTrace();
            }
        });
    }

    private JPanel createArchitecturePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Project Architecture"));
        
        JPanel controlsPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        
        // Linha para o padrão de arquitetura
        JPanel architecturePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        architecturePanel.add(new JBLabel("Architecture Pattern:"));
        
        architectureComboBox = new JComboBox<>(new String[]{"Custom", "DDD (Domain-Driven Design)", "MVC (Model-View-Controller)"});
        architectureComboBox.setToolTipText("Select an architectural pattern to use for code organization");
        architectureComboBox.addActionListener(e -> updatePathsBasedOnArchitecture());
        architecturePanel.add(architectureComboBox);
        
        // Linha para o pacote base
        JPanel packagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        packagePanel.add(new JBLabel("Base Package:"));
        
        basePackageField = new JTextField("com.example", 20);
        basePackageField.setToolTipText("Base package name to use for generated code");
        basePackageField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updatePathsBasedOnArchitecture();
                scanProjectFolders();  // Rescan folders when base package changes
                updateFolderLists();   // Update all folder combo boxes
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                updatePathsBasedOnArchitecture();
                scanProjectFolders();  // Rescan folders when base package changes
                updateFolderLists();   // Update all folder combo boxes
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                updatePathsBasedOnArchitecture();
                scanProjectFolders();  // Rescan folders when base package changes
                updateFolderLists();   // Update all folder combo boxes
            }
        });
        packagePanel.add(basePackageField);
        
        controlsPanel.add(architecturePanel);
        controlsPanel.add(packagePanel);
        
        panel.add(controlsPanel, BorderLayout.CENTER);
        return panel;
    }

    private void updatePathsBasedOnArchitecture() {
        String selected = (String) architectureComboBox.getSelectedItem();
        if (selected == null) return;
        
        String basePackage = basePackageField.getText().trim();
        if (basePackage.isEmpty()) {
            basePackage = "com.example";
        }
        
        // Substitua os pontos por barras para criar o caminho de diretório
        String basePath = "src/main/java/" + basePackage.replace('.', '/');
        
        if (selected.startsWith("DDD")) {
            // Domain-Driven Design pattern
            customDtoPathField.setText(basePath + "/domain/dto");
            customServicePathField.setText(basePath + "/domain/service");
            customRepositoryPathField.setText(basePath + "/domain/repository");
            customControllerPathField.setText(basePath + "/application/controller");
            customFilterPathField.setText(basePath + "/infrastructure/filter");
        } else if (selected.startsWith("MVC")) {
            // Model-View-Controller pattern
            customDtoPathField.setText(basePath + "/model/dto");
            customServicePathField.setText(basePath + "/service");
            customRepositoryPathField.setText(basePath + "/repository");
            customControllerPathField.setText(basePath + "/controller");
            customFilterPathField.setText(basePath + "/filter");
        }
    }

    /**
     * Updates all folder combo boxes with the current project folders
     */
    private void updateFolderLists() {
        // Create a new array of folder paths
        String[] folderArray = projectFolders.toArray(new String[0]);
        
        // Update the model for each ComboBox
        if (dtoFolderComboBox != null) {
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(folderArray);
            dtoFolderComboBox.setModel(model);
        }
        
        if (serviceFolderComboBox != null) {
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(folderArray);
            serviceFolderComboBox.setModel(model);
        }
        
        if (repositoryFolderComboBox != null) {
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(folderArray);
            repositoryFolderComboBox.setModel(model);
        }
        
        if (controllerFolderComboBox != null) {
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(folderArray);
            controllerFolderComboBox.setModel(model);
        }
        
        if (filterFolderComboBox != null) {
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(folderArray);
            filterFolderComboBox.setModel(model);
        }
    }

    /**
     * Finds all existing generated files for this entity
     */
    private void findExistingGeneratedFiles() {
        existingGeneratedFiles.clear();
        
        String entityName = entityClass.getName();
        
        // Common generated file patterns for an entity
        String[] filePatterns = {
            entityName + "Dto.java",
            entityName + "Repository.java",
            entityName + "Service.java",
            entityName + "ServiceImpl.java",
            entityName + "Controller.java",
            entityName + "Filter.java"
        };
        
        // Search in the source directory
        ApplicationManager.getApplication().runReadAction(() -> {
            PsiManager psiManager = PsiManager.getInstance(project);
            
            // Find the source directory
            VirtualFile sourceDir = project.getBaseDir().findFileByRelativePath("src/main/java");
            if (sourceDir != null && sourceDir.exists()) {
                searchForFiles(psiManager.findDirectory(sourceDir), filePatterns, existingGeneratedFiles);
            }
        });
    }
    
    /**
     * Recursively searches for files matching the given patterns.
     */
    private void searchForFiles(PsiDirectory dir, String[] filePatterns, List<PsiFile> result) {
        if (dir == null) return;
        
        // Check files in this directory
        for (PsiFile file : dir.getFiles()) {
            String fileName = file.getName();
            for (String pattern : filePatterns) {
                if (fileName.equals(pattern)) {
                    result.add(file);
                    break;
                }
            }
        }
        
        // Recursively check subdirectories
        for (PsiDirectory subdir : dir.getSubdirectories()) {
            searchForFiles(subdir, filePatterns, result);
        }
    }

    /**
     * Automatically runs the IDE after generation
     */
    private void autoRunIDE() {
        SwingUtilities.invokeLater(() -> {
            try {
                // Refresh the project to ensure all generated files are visible
                project.getBaseDir().refresh(true, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
} 