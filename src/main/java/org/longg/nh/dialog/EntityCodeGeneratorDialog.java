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
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(600, 500));

        // Options panel
        generateDtoCheckbox = new JBCheckBox("Generate DTO", true);
        generateServiceCheckbox = new JBCheckBox("Generate Service", true);
        generateRepositoryCheckbox = new JBCheckBox("Generate Repository", true);
        generateControllerCheckbox = new JBCheckBox("Generate Controller", true);
        generateFilterCheckbox = new JBCheckBox("Generate Filter", false);

        // Create field list for DTO generation
        String[] fieldNames = entityFields.stream()
                .map(ClassField::getName)
                .toArray(String[]::new);

        fieldsList = new JBList<>(fieldNames);
        fieldsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fieldsList.setVisibleRowCount(10);
        fieldsList.setSelectionInterval(0, fieldNames.length - 1); // Select all by default
        JBScrollPane fieldsScrollPane = new JBScrollPane(fieldsList);
        fieldsScrollPane.setBorder(BorderFactory.createTitledBorder("Select fields for DTO"));

        // Create field list for Filter generation
        filterFieldsList = new JList<>(fieldNames);
        filterFieldsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        filterFieldsList.setVisibleRowCount(10);
        JBScrollPane filterFieldsScrollPane = new JBScrollPane(filterFieldsList);
        filterFieldsScrollPane.setBorder(BorderFactory.createTitledBorder("Select fields for Filter"));

        // Enable/disable filter fields based on checkbox
        generateFilterCheckbox.addActionListener(e -> 
            filterFieldsList.setEnabled(generateFilterCheckbox.isSelected())
        );
        filterFieldsList.setEnabled(false);

        // Add components to form
        JPanel optionsPanel = FormBuilder.createFormBuilder()
            .addComponent(generateDtoCheckbox)
            .addComponent(fieldsScrollPane)
            .addComponent(generateServiceCheckbox)
            .addComponent(generateRepositoryCheckbox)
            .addComponent(generateControllerCheckbox)
            .addComponent(generateFilterCheckbox)
            .addComponent(filterFieldsScrollPane)
            .getPanel();

        optionsPanel.setBorder(JBUI.Borders.empty(10));
        panel.add(optionsPanel, BorderLayout.CENTER);

        return panel;
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