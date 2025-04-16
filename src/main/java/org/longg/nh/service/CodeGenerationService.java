package org.longg.nh.service;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.ide.highlighter.JavaFileType;
import org.longg.nh.model.ArchitectureConfig;
import org.longg.nh.util.JavaClassAnalyzer;
import org.longg.nh.util.JavaClassAnalyzer.ClassField;

import java.util.List;
import java.util.Set;

public class CodeGenerationService {
    private final Project project;
    private final ArchitectureConfig config;
    private final PsiClass entityClass;
    private final List<ClassField> allFields;
    private final String basePackage;
    private final String entityName;

    public CodeGenerationService(Project project, ArchitectureConfig config, PsiClass entityClass) {
        this.project = project;
        this.config = config;
        this.entityClass = entityClass;
        this.allFields = JavaClassAnalyzer.getClassFields(entityClass);
        this.entityName = entityClass.getName();
        this.basePackage = ((PsiJavaFile) entityClass.getContainingFile()).getPackageName();
    }

    public PsiClass generateDto(Set<String> selectedFields) {
        String dtoPackage = JavaClassAnalyzer.derivePackageName(basePackage, config.getDtoPackage());
        String dtoName = entityName + "Dto";
        
        // Tạo toàn bộ mã nguồn
        StringBuilder code = new StringBuilder();
        code.append("package ").append(dtoPackage).append(";\n\n");
        
        if (config.isUseLombok()) {
            code.append("import lombok.Data;\n");
            code.append("import lombok.NoArgsConstructor;\n");
            code.append("import lombok.AllArgsConstructor;\n\n");
            
            code.append("@Data\n");
            code.append("@NoArgsConstructor\n");
            code.append("@AllArgsConstructor\n");
        }
        
        code.append("public class ").append(dtoName).append(" {\n\n");
        
        // Thêm các trường được chọn
        for (ClassField field : allFields) {
            if (selectedFields.contains(field.getName())) {
                code.append("    private ").append(field.getType()).append(" ").append(field.getName()).append(";\n");
            }
        }
        
        // Thêm getter và setter nếu không sử dụng Lombok
        if (!config.isUseLombok()) {
            code.append("\n");
            for (ClassField field : allFields) {
                if (selectedFields.contains(field.getName())) {
                    // Generate getter
                    String getterName = "get" + capitalizeFirstLetter(field.getName());
                    code.append("    public ").append(field.getType()).append(" ").append(getterName)
                        .append("() {\n")
                        .append("        return ").append(field.getName()).append(";\n")
                        .append("    }\n\n");
                    
                    // Generate setter
                    String setterName = "set" + capitalizeFirstLetter(field.getName());
                    code.append("    public void ").append(setterName)
                        .append("(").append(field.getType()).append(" ").append(field.getName()).append(") {\n")
                        .append("        this.").append(field.getName()).append(" = ").append(field.getName()).append(";\n")
                        .append("    }\n\n");
                }
            }
        }
        
        code.append("}");
        
        // Tạo file
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        PsiJavaFile dtoFile = (PsiJavaFile) PsiFileFactory.getInstance(project)
                .createFileFromText(dtoName + ".java", JavaFileType.INSTANCE, code.toString());
        
        JavaCodeStyleManager.getInstance(project).optimizeImports(dtoFile);
        
        return dtoFile.getClasses()[0];
    }
    
    public PsiClass generateRepository(Set<String> selectedFilterFields) {
        String repositoryPackage = JavaClassAnalyzer.derivePackageName(basePackage, config.getRepositoryPackage());
        String repositoryName = entityName + "Repository";
        
        // Tạo toàn bộ mã nguồn cho file repository
        StringBuilder code = new StringBuilder();
        code.append("package ").append(repositoryPackage).append(";\n\n")
            .append("import org.springframework.data.jpa.repository.JpaRepository;\n")
            .append("import org.springframework.data.domain.Page;\n")
            .append("import org.springframework.data.domain.Pageable;\n")
            .append("import ").append(((PsiJavaFile) entityClass.getContainingFile()).getPackageName()).append(".")
            .append(entityName).append(";\n")
            .append("import java.util.List;\n\n")
            .append("public interface ").append(repositoryName).append(" extends JpaRepository<")
            .append(entityName).append(", ");
        
        // Tìm loại ID
        String idType = "Long"; // Mặc định
        for (ClassField field : allFields) {
            if (field.getAnnotations().stream().anyMatch(a -> a.endsWith("Id"))) {
                idType = field.getType();
                break;
            }
        }
        
        code.append(idType).append("> {\n\n");
        
        // Chỉ tạo các phương thức tìm kiếm cho các trường được chọn
        if (!selectedFilterFields.isEmpty()) {
            List<ClassField> selectedFields = allFields.stream()
                .filter(field -> selectedFilterFields.contains(field.getName()))
                .toList();
            
            // Phương thức tìm kiếm đơn lẻ cho mỗi trường
            for (ClassField field : selectedFields) {
                String fieldName = field.getName();
                String fieldType = field.getType();
                
                // Finder method tiêu chuẩn
                code.append("    List<").append(entityName).append("> findBy")
                    .append(capitalizeFirstLetter(fieldName))
                    .append("(").append(fieldType).append(" ").append(fieldName).append(");\n\n");
                
                // Finder method với phân trang
                code.append("    Page<").append(entityName).append("> findBy")
                    .append(capitalizeFirstLetter(fieldName))
                    .append("(").append(fieldType).append(" ").append(fieldName).append(", Pageable pageable);\n\n");
                
                // Thêm các phương thức tìm kiếm đặc biệt dựa vào kiểu dữ liệu
                if (fieldType.equals("String")) {
                    code.append("    List<").append(entityName).append("> findBy")
                        .append(capitalizeFirstLetter(fieldName))
                        .append("ContainingIgnoreCase(String ").append(fieldName).append(");\n\n");
                    
                    code.append("    Page<").append(entityName).append("> findBy")
                        .append(capitalizeFirstLetter(fieldName))
                        .append("ContainingIgnoreCase(String ").append(fieldName).append(", Pageable pageable);\n\n");
                }
                else if (isNumericType(fieldType) || fieldType.contains("Date") || fieldType.contains("LocalDate")) {
                    String minMaxPrefix = isNumericType(fieldType) ? "" : fieldType.contains("Date") ? "Date" : "Time";
                    String gtOperator = minMaxPrefix.isEmpty() ? "GreaterThanEqual" : "After";
                    String ltOperator = minMaxPrefix.isEmpty() ? "LessThanEqual" : "Before";
                    
                    code.append("    List<").append(entityName).append("> findBy")
                        .append(capitalizeFirstLetter(fieldName))
                        .append(gtOperator).append("(").append(fieldType).append(" min")
                        .append(capitalizeFirstLetter(fieldName)).append(");\n\n");
                    
                    code.append("    List<").append(entityName).append("> findBy")
                        .append(capitalizeFirstLetter(fieldName))
                        .append(ltOperator).append("(").append(fieldType).append(" max")
                        .append(capitalizeFirstLetter(fieldName)).append(");\n\n");
                    
                    code.append("    Page<").append(entityName).append("> findBy")
                        .append(capitalizeFirstLetter(fieldName))
                        .append("Between(").append(fieldType).append(" min")
                        .append(capitalizeFirstLetter(fieldName)).append(", ")
                        .append(fieldType).append(" max")
                        .append(capitalizeFirstLetter(fieldName)).append(", Pageable pageable);\n\n");
                }
            }
            
            // Tạo một phương thức tìm kiếm kết hợp tất cả các trường được chọn
            if (selectedFields.size() > 1) {
                // Xây dựng tên method
                StringBuilder methodName = new StringBuilder("findBy");
                StringBuilder parameters = new StringBuilder();
                
                for (int i = 0; i < selectedFields.size(); i++) {
                    ClassField field = selectedFields.get(i);
                    if (i > 0) {
                        methodName.append("And");
                        parameters.append(", ");
                    }
                    
                    methodName.append(capitalizeFirstLetter(field.getName()));
                    parameters.append(field.getType())
                        .append(" ")
                        .append(field.getName());
                }
                
                // Phương thức list
                code.append("    List<").append(entityName).append("> ")
                    .append(methodName)
                    .append("(").append(parameters).append(");\n\n");
                
                // Phương thức với phân trang
                code.append("    Page<").append(entityName).append("> ")
                    .append(methodName)
                    .append("(").append(parameters).append(", Pageable pageable);\n\n");
            }
        }
        
        code.append("}");
        
        // Tạo file
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        PsiJavaFile repositoryFile = (PsiJavaFile) PsiFileFactory.getInstance(project)
                .createFileFromText(repositoryName + ".java", JavaFileType.INSTANCE, code.toString());
        
        JavaCodeStyleManager.getInstance(project).optimizeImports(repositoryFile);
        
        return repositoryFile.getClasses()[0];
    }
    
    public PsiClass generateService(boolean hasRepository) {
        String servicePackage = JavaClassAnalyzer.derivePackageName(basePackage, config.getServicePackage());
        String serviceName = entityName + "Service";
        
        StringBuilder code = new StringBuilder();
        code.append("package ").append(servicePackage).append(";\n\n");
        
        if (hasRepository) {
            // Imports
            code.append("import org.springframework.stereotype.Service;\n")
                .append("import org.springframework.data.domain.Page;\n")
                .append("import org.springframework.data.domain.PageRequest;\n")
                .append("import org.springframework.data.domain.Pageable;\n")
                .append("import java.util.List;\n")
                .append("import java.util.Optional;\n");
            
            // Entity import
            String entityFQN = ((PsiJavaFile) entityClass.getContainingFile()).getPackageName() + "." + entityName;
            code.append("import ").append(entityFQN).append(";\n");
            
            // Repository import
            String repositoryPackage = JavaClassAnalyzer.derivePackageName(basePackage, config.getRepositoryPackage());
            String repositoryName = entityName + "Repository";
            code.append("import ").append(repositoryPackage).append(".").append(repositoryName).append(";\n");
            
            // Param import
            String dtoPackage = JavaClassAnalyzer.derivePackageName(basePackage, config.getDtoPackage());
            String paramPackage = dtoPackage + ".filter";
            String paramName = entityName + "Param";
            code.append("import ").append(paramPackage).append(".").append(paramName).append(";\n\n");
            
            // Class definition
            code.append("@Service\n")
                .append("public class ").append(serviceName).append(" {\n\n");
            
            // Repository field
            String repositoryFieldName = lcFirst(repositoryName);
            code.append("    private final ").append(repositoryName).append(" ").append(repositoryFieldName).append(";\n\n");
            
            // Constructor
            code.append("    public ").append(serviceName).append("(").append(repositoryName).append(" ")
                .append(repositoryFieldName).append(") {\n")
                .append("        this.").append(repositoryFieldName).append(" = ").append(repositoryFieldName).append(";\n")
                .append("    }\n\n");
            
            // CRUD methods
            // Find by ID - return entity
            code.append("    public ").append(entityName).append(" findById(Long id) {\n")
                .append("        return ").append(repositoryFieldName).append(".findById(id).orElse(null);\n")
                .append("    }\n\n");
            
            // Find by ID - return optional entity
            code.append("    public Optional<").append(entityName).append("> findOptionalById(Long id) {\n")
                .append("        return ").append(repositoryFieldName).append(".findById(id);\n")
                .append("    }\n\n");
            
            // Find all - return entity list
            code.append("    public List<").append(entityName).append("> findAll() {\n")
                .append("        return ").append(repositoryFieldName).append(".findAll();\n")
                .append("    }\n\n");
            
            // Tìm kiếm với Param
            code.append("    public Page<").append(entityName).append("> search(").append(paramName).append(" param) {\n")
                .append("        Pageable pageable = PageRequest.of(param.getPage(), param.getSize());\n")
                .append("        // Sử dụng các trường có trong param để xây dựng câu truy vấn\n")
                .append("        // TODO: Thêm xử lý tìm kiếm dựa trên các trường trong param\n")
                .append("        // Ví dụ:\n")
                .append("        // if (param.getName() != null && !param.getName().isEmpty()) {\n")
                .append("        //     return ").append(repositoryFieldName).append(".findByNameContainingIgnoreCase(param.getName(), pageable);\n")
                .append("        // }\n")
                .append("        return ").append(repositoryFieldName).append(".findAll(pageable);\n")
                .append("    }\n\n");
            
            // Save - return entity
            code.append("    public ").append(entityName).append(" save(").append(entityName).append(" entity) {\n")
                .append("        return ").append(repositoryFieldName).append(".save(entity);\n")
                .append("    }\n\n");
            
            // Update - return entity
            code.append("    public ").append(entityName).append(" update(").append(entityName).append(" entity) {\n")
                .append("        return ").append(repositoryFieldName).append(".save(entity);\n")
                .append("    }\n\n");
            
            // Delete by ID
            code.append("    public void deleteById(Long id) {\n")
                .append("        ").append(repositoryFieldName).append(".deleteById(id);\n")
                .append("    }\n\n");
            
            // Delete entity
            code.append("    public void delete(").append(entityName).append(" entity) {\n")
                .append("        ").append(repositoryFieldName).append(".delete(entity);\n")
                .append("    }\n");
            
        } else {
            // Simple service without repository
            code.append("import org.springframework.stereotype.Service;\n\n")
                .append("@Service\n")
                .append("public class ").append(serviceName).append(" {\n");
        }
        
        code.append("}");
        
        // Tạo file
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        PsiJavaFile serviceFile = (PsiJavaFile) PsiFileFactory.getInstance(project)
                .createFileFromText(serviceName + ".java", JavaFileType.INSTANCE, code.toString());
        
        JavaCodeStyleManager.getInstance(project).optimizeImports(serviceFile);
        
        return serviceFile.getClasses()[0];
    }
    
    public PsiClass generateController(boolean hasService) {
        String controllerPackage = JavaClassAnalyzer.derivePackageName(basePackage, config.getControllerPackage());
        String controllerName = entityName + "Controller";
        
        // Tạo toàn bộ mã nguồn với tất cả các import cần thiết
        StringBuilder code = new StringBuilder();
        code.append("package ").append(controllerPackage).append(";\n\n");
        
        // Thêm import cần thiết
        if (hasService) {
            code.append("import java.util.List;\n")
                .append("import org.springframework.data.domain.Page;\n")
                .append("import org.springframework.http.HttpStatus;\n")
                .append("import org.springframework.http.ResponseEntity;\n")
                .append("import org.springframework.web.bind.annotation.*;\n");
            
            // Import entity
            String entityFQN = ((PsiJavaFile) entityClass.getContainingFile()).getPackageName() + "." + entityName;
            code.append("import ").append(entityFQN).append(";\n");
            
            // Import service
            String servicePackage = JavaClassAnalyzer.derivePackageName(basePackage, config.getServicePackage());
            String serviceName = entityName + "Service";
            code.append("import ").append(servicePackage).append(".").append(serviceName).append(";\n");
            
            // Import param
            String dtoPackage = JavaClassAnalyzer.derivePackageName(basePackage, config.getDtoPackage());
            String paramPackage = dtoPackage + ".filter";
            String paramName = entityName + "Param";
            code.append("import ").append(paramPackage).append(".").append(paramName).append(";\n\n");
            
            // Tạo class với annotation
            code.append("@RestController\n")
                .append("@RequestMapping(\"/").append(lcFirst(entityName)).append("s\")\n")
                .append("public class ").append(controllerName).append(" {\n\n");
            
            // Service field
            String serviceFieldName = lcFirst(serviceName);
            code.append("    private final ").append(serviceName).append(" ").append(serviceFieldName).append(";\n\n");
            
            // Constructor
            code.append("    public ").append(controllerName).append("(").append(serviceName).append(" ")
                .append(serviceFieldName).append(") {\n")
                .append("        this.").append(serviceFieldName).append(" = ").append(serviceFieldName).append(";\n")
                .append("    }\n\n");
            
            // Endpoints
            // GET all entities
            code.append("    @GetMapping\n")
                .append("    public List<").append(entityName).append("> getAll() {\n")
                .append("        return ").append(serviceFieldName).append(".findAll();\n")
                .append("    }\n\n");
            
            // Search with Param
            code.append("    @PostMapping(\"/search\")\n")
                .append("    public Page<").append(entityName).append("> search(@RequestBody ")
                .append(paramName).append(" param) {\n")
                .append("        return ").append(serviceFieldName).append(".search(param);\n")
                .append("    }\n\n");
            
            // GET by ID - Entity
            code.append("    @GetMapping(\"/{id}\")\n")
                .append("    public ResponseEntity<").append(entityName).append("> getById(@PathVariable Long id) {\n")
                .append("        ").append(entityName).append(" entity = ").append(serviceFieldName).append(".findById(id);\n")
                .append("        return entity != null ? ResponseEntity.ok(entity) : ResponseEntity.notFound().build();\n")
                .append("    }\n\n");
            
            // POST Entity
            code.append("    @PostMapping\n")
                .append("    public ResponseEntity<").append(entityName).append("> create(@RequestBody ")
                .append(entityName).append(" entity) {\n")
                .append("        return ResponseEntity.status(HttpStatus.CREATED).body(")
                .append(serviceFieldName).append(".save(entity));\n")
                .append("    }\n\n");
            
            // PUT Entity
            code.append("    @PutMapping(\"/{id}\")\n")
                .append("    public ResponseEntity<").append(entityName).append("> update(@PathVariable Long id, @RequestBody ")
                .append(entityName).append(" entity) {\n")
                .append("        return ResponseEntity.ok(").append(serviceFieldName).append(".update(entity));\n")
                .append("    }\n\n");
            
            // DELETE
            code.append("    @DeleteMapping(\"/{id}\")\n")
                .append("    public ResponseEntity<Void> delete(@PathVariable Long id) {\n")
                .append("        ").append(serviceFieldName).append(".deleteById(id);\n")
                .append("        return ResponseEntity.noContent().build();\n")
                .append("    }\n");
            
        } else {
            // Simple controller if service is not available
            code.append("import org.springframework.web.bind.annotation.*;\n\n")
                .append("@RestController\n")
                .append("@RequestMapping(\"/").append(lcFirst(entityName)).append("s\")\n")
                .append("public class ").append(controllerName).append(" {\n");
        }
        
        code.append("}");
        
        // Tạo file
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        PsiJavaFile controllerFile = (PsiJavaFile) PsiFileFactory.getInstance(project)
                .createFileFromText(controllerName + ".java", JavaFileType.INSTANCE, code.toString());
        
        JavaCodeStyleManager.getInstance(project).optimizeImports(controllerFile);
        
        return controllerFile.getClasses()[0];
    }
    
    public PsiClass generateFilter(Set<String> selectedFields) {
        String dtoPackage = JavaClassAnalyzer.derivePackageName(basePackage, config.getDtoPackage());
        String filterPackage = dtoPackage + ".filter";
        String paramName = entityName + "Param";
        
        StringBuilder code = new StringBuilder();
        code.append("package ").append(filterPackage).append(";\n\n");
        
        // Thêm import Lombok nếu cần
        if (config.isUseLombok()) {
            code.append("import lombok.Data;\n")
                .append("import lombok.NoArgsConstructor;\n")
                .append("import lombok.AllArgsConstructor;\n\n")
                .append("@Data\n")
                .append("@NoArgsConstructor\n")
                .append("@AllArgsConstructor\n");
        }
        
        code.append("public class ").append(paramName).append(" {\n\n");
        
        // Thêm các trường lọc
        for (ClassField field : allFields) {
            if (selectedFields.contains(field.getName())) {
                // Cho trường String
                if (field.getType().equals("String")) {
                    code.append("    private String ").append(field.getName()).append(";\n");
                }
                // Cho trường số
                else if (isNumericType(field.getType())) {
                    code.append("    private ").append(field.getType()).append(" min")
                        .append(capitalizeFirstLetter(field.getName())).append(";\n");
                    code.append("    private ").append(field.getType()).append(" max")
                        .append(capitalizeFirstLetter(field.getName())).append(";\n");
                }
                // Cho trường ngày tháng
                else if (field.getType().contains("Date") || field.getType().contains("LocalDate")) {
                    code.append("    private ").append(field.getType()).append(" from")
                        .append(capitalizeFirstLetter(field.getName())).append(";\n");
                    code.append("    private ").append(field.getType()).append(" to")
                        .append(capitalizeFirstLetter(field.getName())).append(";\n");
                }
                // Cho trường boolean
                else if (field.getType().equals("boolean") || field.getType().equals("Boolean")) {
                    code.append("    private Boolean ").append(field.getName()).append(";\n");
                }
                // Cho kiểu enum
                else if (field.getType().startsWith(entityClass.getQualifiedName() + ".")) {
                    code.append("    private ").append(field.getType()).append(" ").append(field.getName()).append(";\n");
                }
            }
        }
        
        // Thêm getter và setter nếu không sử dụng Lombok
        if (!config.isUseLombok()) {
            code.append("\n");
            
            // Duyệt qua các trường và tạo getter, setter
            for (ClassField field : allFields) {
                if (selectedFields.contains(field.getName())) {
                    // Cho trường String hoặc boolean
                    if (field.getType().equals("String") || field.getType().equals("boolean") 
                            || field.getType().equals("Boolean") || field.getType().startsWith(entityClass.getQualifiedName() + ".")) {
                        
                        String fieldName = field.getName();
                        String fieldType = field.getType();
                        
                        // Getter
                        code.append("    public ").append(fieldType).append(" get")
                            .append(capitalizeFirstLetter(fieldName)).append("() {\n")
                            .append("        return ").append(fieldName).append(";\n")
                            .append("    }\n\n");
                        
                        // Setter
                        code.append("    public void set").append(capitalizeFirstLetter(fieldName))
                            .append("(").append(fieldType).append(" ").append(fieldName).append(") {\n")
                            .append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n")
                            .append("    }\n\n");
                    }
                    // Cho trường số và ngày tháng (min/max hoặc from/to)
                    else if (isNumericType(field.getType()) || field.getType().contains("Date") 
                            || field.getType().contains("LocalDate")) {
                        
                        String fieldName = field.getName();
                        String fieldType = field.getType();
                        
                        if (isNumericType(field.getType())) {
                            String minName = "min" + capitalizeFirstLetter(fieldName);
                            String maxName = "max" + capitalizeFirstLetter(fieldName);
                            
                            // Min getter/setter
                            code.append("    public ").append(fieldType).append(" get")
                                .append(capitalizeFirstLetter(minName)).append("() {\n")
                                .append("        return ").append(minName).append(";\n")
                                .append("    }\n\n");
                            
                            code.append("    public void set").append(capitalizeFirstLetter(minName))
                                .append("(").append(fieldType).append(" ").append(minName).append(") {\n")
                                .append("        this.").append(minName).append(" = ").append(minName).append(";\n")
                                .append("    }\n\n");
                            
                            // Max getter/setter
                            code.append("    public ").append(fieldType).append(" get")
                                .append(capitalizeFirstLetter(maxName)).append("() {\n")
                                .append("        return ").append(maxName).append(";\n")
                                .append("    }\n\n");
                            
                            code.append("    public void set").append(capitalizeFirstLetter(maxName))
                                .append("(").append(fieldType).append(" ").append(maxName).append(") {\n")
                                .append("        this.").append(maxName).append(" = ").append(maxName).append(";\n")
                                .append("    }\n\n");
                        } else {
                            String fromName = "from" + capitalizeFirstLetter(fieldName);
                            String toName = "to" + capitalizeFirstLetter(fieldName);
                            
                            // From getter/setter
                            code.append("    public ").append(fieldType).append(" get")
                                .append(capitalizeFirstLetter(fromName)).append("() {\n")
                                .append("        return ").append(fromName).append(";\n")
                                .append("    }\n\n");
                            
                            code.append("    public void set").append(capitalizeFirstLetter(fromName))
                                .append("(").append(fieldType).append(" ").append(fromName).append(") {\n")
                                .append("        this.").append(fromName).append(" = ").append(fromName).append(";\n")
                                .append("    }\n\n");
                            
                            // To getter/setter
                            code.append("    public ").append(fieldType).append(" get")
                                .append(capitalizeFirstLetter(toName)).append("() {\n")
                                .append("        return ").append(toName).append(";\n")
                                .append("    }\n\n");
                            
                            code.append("    public void set").append(capitalizeFirstLetter(toName))
                                .append("(").append(fieldType).append(" ").append(toName).append(") {\n")
                                .append("        this.").append(toName).append(" = ").append(toName).append(";\n")
                                .append("    }\n\n");
                        }
                    }
                }
            }
        }
        
        code.append("    // Phương thức trả về trang số\n");
        code.append("    private Integer page = 0;\n");
        code.append("    private Integer size = 20;\n\n");
        
        if (!config.isUseLombok()) {
            // Getter/setter cho page và size
            code.append("    public Integer getPage() {\n")
                .append("        return page;\n")
                .append("    }\n\n");
            
            code.append("    public void setPage(Integer page) {\n")
                .append("        this.page = page;\n")
                .append("    }\n\n");
            
            code.append("    public Integer getSize() {\n")
                .append("        return size;\n")
                .append("    }\n\n");
            
            code.append("    public void setSize(Integer size) {\n")
                .append("        this.size = size;\n")
                .append("    }\n\n");
        }
        
        code.append("}");
        
        // Tạo file
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        PsiJavaFile paramFile = (PsiJavaFile) PsiFileFactory.getInstance(project)
                .createFileFromText(paramName + ".java", JavaFileType.INSTANCE, code.toString());
        
        JavaCodeStyleManager.getInstance(project).optimizeImports(paramFile);
        
        return paramFile.getClasses()[0];
    }
    
    private boolean isNumericType(String type) {
        return type.equals("int") || type.equals("Integer") || 
               type.equals("long") || type.equals("Long") ||
               type.equals("float") || type.equals("Float") || 
               type.equals("double") || type.equals("Double") || 
               type.equals("BigDecimal");
    }
    
    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    private String lcFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }
}