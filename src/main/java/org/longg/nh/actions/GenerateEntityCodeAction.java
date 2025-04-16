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