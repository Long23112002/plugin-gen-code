package org.longg.nh.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.longg.nh.dialog.EntityCodeGeneratorDialog;
import org.longg.nh.model.ArchitectureConfig;
import org.longg.nh.service.ConfigurationService;
import org.longg.nh.util.JavaClassAnalyzer;

import java.util.Optional;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import java.util.ArrayList;
import java.util.List;

public class GenerateEntityCodeAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        PsiClass psiClass = getPsiClassFromContext(e);
        if (psiClass == null || !JavaClassAnalyzer.isEntity(psiClass)) {
            return;
        }

        // Offer to clean up existing generated files first
        if (offerToCleanupExistingFiles(project, psiClass)) {
            return; // User chose to clean up and not proceed with generation
        }

        // Get configuration or create default
        Optional<ArchitectureConfig> configOpt = ConfigurationService.getInstance().getConfiguration(project);
        ArchitectureConfig config;
        
        if (configOpt.isPresent()) {
            config = configOpt.get();
        } else {
            ConfigurationService.getInstance().saveDefaultConfiguration(project);
            config = ConfigurationService.getInstance().getConfiguration(project).orElse(createDefaultConfig());
        }

        // Show dialog
        EntityCodeGeneratorDialog dialog = new EntityCodeGeneratorDialog(project, psiClass, config);
        dialog.show();
    }

    /**
     * Offers to clean up existing generated files for the selected entity.
     * 
     * @param project The current project
     * @param entityClass The entity class
     * @return true if cleanup was performed and generation should be skipped, false otherwise
     */
    private boolean offerToCleanupExistingFiles(Project project, PsiClass entityClass) {
        String entityName = entityClass.getName();
        List<PsiFile> existingFiles = findGeneratedFilesForEntity(project, entityName);
        
        if (existingFiles.isEmpty()) {
            return false; // No existing files to clean up
        }
        
        StringBuilder message = new StringBuilder();
        message.append("Found existing generated files for entity ")
               .append(entityName)
               .append(":\n\n");
        
        for (PsiFile file : existingFiles) {
            String relativePath = getRelativePath(project, file);
            message.append("• ").append(relativePath).append("\n");
        }
        
        message.append("\nWhat would you like to do?");
        
        String[] options = {
            "Delete All and Start Fresh", 
            "Keep Files and Generate", 
            "Cancel"
        };
        
        int result = Messages.showDialog(
            project,
            message.toString(),
            "Existing Generated Files Found",
            options,
            0, // Default option
            Messages.getQuestionIcon()
        );
        
        if (result == 0) { // Delete All and Start Fresh
            WriteCommandAction.runWriteCommandAction(project, () -> {
                for (PsiFile file : existingFiles) {
                    file.delete();
                }
                
                Messages.showInfoMessage(
                    existingFiles.size() + " files have been deleted. You can now regenerate the code.",
                    "Cleanup Complete"
                );
                
                // Refresh the project view
                ProjectView.getInstance(project).refresh();
            });
            
            return false; // Proceed with generation
        } else if (result == 1) { // Keep Files and Generate
            return false; // Proceed with generation
        } else { // Cancel
            return true; // Skip generation
        }
    }
    
    /**
     * Finds all generated files for a specific entity.
     * 
     * @param project The current project
     * @param entityName The name of the entity
     * @return A list of files related to the entity
     */
    private List<PsiFile> findGeneratedFilesForEntity(Project project, String entityName) {
        List<PsiFile> result = new ArrayList<>();
        
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
        ApplicationManager.getApplication().runReadAction((Computable<Void>) () -> {
            PsiManager psiManager = PsiManager.getInstance(project);
            
            // Find the source directory
            VirtualFile sourceDir = project.getBaseDir().findFileByRelativePath("src/main/java");
            if (sourceDir != null && sourceDir.exists()) {
                searchForFiles(psiManager.findDirectory(sourceDir), filePatterns, result);
            }
            
            return null;
        });
        
        return result;
    }
    
    /**
     * Recursively searches for files matching the given patterns.
     * 
     * @param dir The directory to search in
     * @param filePatterns Patterns to match against file names
     * @param result The list to add matching files to
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
     * Gets the relative path of a file from the project root.
     * 
     * @param project The current project
     * @param file The file to get the path for
     * @return The relative path as a string
     */
    private String getRelativePath(Project project, PsiFile file) {
        VirtualFile vFile = file.getVirtualFile();
        if (vFile == null) {
            return file.getName();
        }
        
        String projectPath = project.getBasePath();
        String filePath = vFile.getPath();
        
        if (projectPath != null && filePath.startsWith(projectPath)) {
            return filePath.substring(projectPath.length() + 1);
        }
        
        return filePath;
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        PsiClass psiClass = getPsiClassFromContext(e);
        boolean isEnabled = psiClass != null && JavaClassAnalyzer.isEntity(psiClass);
        e.getPresentation().setEnabledAndVisible(isEnabled);
    }

    private PsiClass getPsiClassFromContext(AnActionEvent e) {
        // From editor
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        
        if (editor != null && psiFile instanceof PsiJavaFile) {
            int offset = editor.getCaretModel().getOffset();
            PsiElement element = psiFile.findElementAt(offset);
            return PsiTreeUtil.getParentOfType(element, PsiClass.class);
        }
        
        // From project view
        PsiElement psiElement = e.getData(CommonDataKeys.PSI_ELEMENT);
        if (psiElement instanceof PsiClass) {
            return (PsiClass) psiElement;
        }
        
        return null;
    }

    private ArchitectureConfig createDefaultConfig() {
        ArchitectureConfig config = new ArchitectureConfig();
        config.setName("Default");
        config.setDescription("Default architecture pattern");
        config.setDtoPackage("dto");
        config.setServicePackage("service");
        config.setRepositoryPackage("repository");
        config.setControllerPackage("controller");
        config.setFilterPackage("filter");
        config.setUseLombok(true);
        
        return config;
    }
} 