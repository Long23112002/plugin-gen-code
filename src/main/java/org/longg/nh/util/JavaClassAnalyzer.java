package org.longg.nh.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtil;

import java.util.*;
import java.util.stream.Collectors;

public class JavaClassAnalyzer {

    public static class ClassField {
        private final String name;
        private final String type;
        private final String qualifiedType;
        private final boolean isCollection;
        private final boolean isPrimitive;
        private final boolean isFinal;
        private final List<String> annotations;

        public ClassField(String name, String type, String qualifiedType, 
                          boolean isCollection, boolean isPrimitive, boolean isFinal,
                          List<String> annotations) {
            this.name = name;
            this.type = type;
            this.qualifiedType = qualifiedType;
            this.isCollection = isCollection;
            this.isPrimitive = isPrimitive;
            this.isFinal = isFinal;
            this.annotations = annotations;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String getQualifiedType() {
            return qualifiedType;
        }

        public boolean isCollection() {
            return isCollection;
        }

        public boolean isPrimitive() {
            return isPrimitive;
        }

        public boolean isFinal() {
            return isFinal;
        }

        public List<String> getAnnotations() {
            return annotations;
        }
    }

    public static List<ClassField> getClassFields(PsiClass psiClass) {
        return Arrays.stream(psiClass.getAllFields())
            .filter(field -> !field.hasModifierProperty(PsiModifier.STATIC))
            .map(field -> {
                PsiType type = field.getType();
                String typeName = type.getPresentableText();
                String qualifiedTypeName = type.getCanonicalText();
                boolean isCollection = isCollectionType(type);
                boolean isPrimitive = type instanceof PsiPrimitiveType;
                boolean isFinal = field.hasModifierProperty(PsiModifier.FINAL);
                List<String> annotations = Arrays.stream(field.getAnnotations())
                        .map(a -> a.getQualifiedName())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                
                return new ClassField(
                    field.getName(),
                    typeName,
                    qualifiedTypeName,
                    isCollection,
                    isPrimitive,
                    isFinal,
                    annotations
                );
            })
            .collect(Collectors.toList());
    }

    private static boolean isCollectionType(PsiType type) {
        PsiClass psiClass = PsiUtil.resolveClassInClassTypeOnly(type);
        if (psiClass == null) {
            return false;
        }

        String qualifiedName = psiClass.getQualifiedName();
        if (qualifiedName == null) {
            return false;
        }

        return qualifiedName.equals("java.util.Collection") 
            || qualifiedName.equals("java.util.List")
            || qualifiedName.equals("java.util.Set")
            || qualifiedName.equals("java.util.Map")
            || qualifiedName.startsWith("java.util.")
               && hasCollectionInHierarchy(psiClass);
    }

    private static boolean hasCollectionInHierarchy(PsiClass psiClass) {
        Queue<PsiClass> toCheck = new LinkedList<>();
        toCheck.add(psiClass);

        while (!toCheck.isEmpty()) {
            PsiClass current = toCheck.poll();
            
            // Check interfaces
            for (PsiClass iface : current.getInterfaces()) {
                String ifaceName = iface.getQualifiedName();
                if (ifaceName != null && (
                    ifaceName.equals("java.util.Collection") ||
                    ifaceName.equals("java.util.List") ||
                    ifaceName.equals("java.util.Set") ||
                    ifaceName.equals("java.util.Map")
                )) {
                    return true;
                }
                toCheck.add(iface);
            }
            
            // Check superclass
            PsiClass superClass = current.getSuperClass();
            if (superClass != null) {
                String superClassName = superClass.getQualifiedName();
                if (superClassName != null && (
                    superClassName.equals("java.util.ArrayList") ||
                    superClassName.equals("java.util.LinkedList") ||
                    superClassName.equals("java.util.HashSet") ||
                    superClassName.equals("java.util.HashMap")
                )) {
                    return true;
                }
                toCheck.add(superClass);
            }
        }
        
        return false;
    }

    public static boolean isEntity(PsiClass psiClass) {
        return Arrays.stream(psiClass.getAnnotations())
                .anyMatch(annotation -> {
                    String qualifiedName = annotation.getQualifiedName();
                    return qualifiedName != null && (
                        qualifiedName.equals("javax.persistence.Entity") ||
                        qualifiedName.equals("jakarta.persistence.Entity")
                    );
                });
    }

    public static String getPackageName(PsiJavaFile javaFile) {
        return javaFile.getPackageName();
    }

    public static String derivePackageName(String basePackage, String componentType) {
        String packageName = basePackage;
        
        // Remove entity/model portion if it exists
        if (packageName.endsWith(".entity") || packageName.endsWith(".entities") || 
            packageName.endsWith(".model") || packageName.endsWith(".models")) {
            packageName = packageName.substring(0, packageName.lastIndexOf('.'));
        }
        
        return packageName + "." + componentType;
    }
} 