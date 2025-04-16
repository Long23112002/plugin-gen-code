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
import org.longg.nh.service.CodeGenerationService;
import org.longg.nh.util.JavaClassAnalyzer;
import org.longg.nh.util.JavaClassAnalyzer.ClassField;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class EntityCodeGeneratorDialog extends DialogWrapper {
    private final Project project;
    private final PsiClass entityClass;
    private final ArchitectureConfig config;
    private final List<ClassField> entityFields;
    private final CodeGenerationService codeGenerationService;

    private JBCheckBox generateDtoCheckbox;
    private JBCheckBox generateServiceCheckbox;
    private JBCheckBox generateRepositoryCheckbox;
    private JBCheckBox generateControllerCheckbox;
    private JBCheckBox generateFilterCheckbox;
    private JBList<String> fieldsList;
    private JList<String> filterFieldsList;

    public EntityCodeGeneratorDialog(Project project, PsiClass entityClass, ArchitectureConfig config) {
        super(project);
        this.project = project;
        this.entityClass = entityClass;
        this.config = config;
        this.entityFields = JavaClassAnalyzer.getClassFields(entityClass);
        this.codeGenerationService = new CodeGenerationService(project, config, entityClass);

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
        JTabbedPane tabbedPane = new JTabbedPane();
        
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

        // Add checkboxes to a panel
        JPanel checkboxesPanel = new JPanel(new GridLayout(5, 1, 0, 10));
        checkboxesPanel.setBorder(JBUI.Borders.empty(10));
        checkboxesPanel.add(generateDtoCheckbox);
        checkboxesPanel.add(generateServiceCheckbox);
        checkboxesPanel.add(generateRepositoryCheckbox);
        checkboxesPanel.add(generateControllerCheckbox);
        checkboxesPanel.add(generateFilterCheckbox);
        
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
        fieldListsPanel.add(fieldsScrollPane);
        fieldListsPanel.add(filterFieldsScrollPane);
        
        fieldsPanel.add(fieldListsPanel, BorderLayout.CENTER);
        tabbedPane.addTab("Fields", fieldsPanel);

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
        boolean hasSelection = generateDtoCheckbox.isSelected() ||
                             generateServiceCheckbox.isSelected() ||
                             generateRepositoryCheckbox.isSelected() ||
                             generateControllerCheckbox.isSelected() ||
                             generateFilterCheckbox.isSelected();
        
        setOKActionEnabled(hasSelection);
    }

    @Override
    protected void doOKAction() {
        if (!generateDtoCheckbox.isSelected() &&
            !generateServiceCheckbox.isSelected() &&
            !generateRepositoryCheckbox.isSelected() &&
            !generateControllerCheckbox.isSelected() &&
            !generateFilterCheckbox.isSelected()) {
            Messages.showErrorDialog(
                "Please select at least one component to generate",
                "No Selection"
            );
            return;
        }

        WriteCommandAction.runWriteCommandAction(project, () -> {
            try {
                generateSelectedComponents();
                super.doOKAction();
            } catch (Exception e) {
                Messages.showErrorDialog(
                    "Error generating code: " + e.getMessage(),
                    "Generation Error"
                );
            }
        });
    }

    private void generateSelectedComponents() {
        Map<String, PsiClass> generatedClasses = new HashMap<>();

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
            PsiClass dtoClass = codeGenerationService.generateDto(selectedDtoFields);
            PsiFile dtoFile = dtoClass.getContainingFile();
            saveFile(dtoFile, config.getDtoPackage());
            generatedClasses.put("dto", dtoClass);
        }

        // Generate Repository
        if (generateRepositoryCheckbox.isSelected()) {
            PsiClass repositoryClass = codeGenerationService.generateRepository(selectedFilterFields);
            PsiFile repositoryFile = repositoryClass.getContainingFile();
            saveFile(repositoryFile, config.getRepositoryPackage());
            generatedClasses.put("repository", repositoryClass);
        }

        // Generate Service
        if (generateServiceCheckbox.isSelected()) {
            PsiClass serviceClass = codeGenerationService.generateService(generateRepositoryCheckbox.isSelected());
            PsiFile serviceFile = serviceClass.getContainingFile();
            saveFile(serviceFile, config.getServicePackage());
            generatedClasses.put("service", serviceClass);
        }

        // Generate Controller
        if (generateControllerCheckbox.isSelected()) {
            PsiClass controllerClass = codeGenerationService.generateController(generateServiceCheckbox.isSelected());
            PsiFile controllerFile = controllerClass.getContainingFile();
            saveFile(controllerFile, config.getControllerPackage());
            generatedClasses.put("controller", controllerClass);
        }

        // Generate Filter
        if (generateFilterCheckbox.isSelected() && !selectedFilterFields.isEmpty()) {
            PsiClass filterClass = codeGenerationService.generateFilter(selectedFilterFields);
            PsiFile filterFile = filterClass.getContainingFile();
            saveFile(filterFile, config.getFilterPackage());
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

    private void saveFile(PsiFile file, String packageSuffix) {
        String packageName = ((PsiJavaFile) file).getPackageName();
        
        String[] packageParts = packageName.split("\\.");
        PsiDirectory baseDir = ApplicationManager.getApplication().runReadAction(
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
        
        if (baseDir == null) {
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
            
            baseDir = sourceDir;
            
            // Create package directories
            for (String part : packageParts) {
                PsiDirectory existingDir = baseDir.findSubdirectory(part);
                if (existingDir != null) {
                    baseDir = existingDir;
                } else {
                    baseDir = baseDir.createSubdirectory(part);
                }
            }
        } else {
            // Create component package directory
            String componentPackage = packageParts[packageParts.length - 1];
            PsiDirectory componentDir = baseDir.findSubdirectory(componentPackage);
            if (componentDir != null) {
                baseDir = componentDir;
            } else {
                baseDir = baseDir.createSubdirectory(componentPackage);
            }
        }
        
        // Make sure the file doesn't exist already
        String fileName = file.getName();
        PsiFile existingFile = baseDir.findFile(fileName);
        
        if (existingFile != null) {
            int result = Messages.showYesNoDialog(
                "File " + fileName + " already exists. Do you want to overwrite it?",
                "File Already Exists",
                Messages.getQuestionIcon()
            );
            
            if (result == Messages.NO) {
                return;
            }
            
            existingFile.delete();
        }
        
        // Add file to directory
        baseDir.add(file);
    }
} 