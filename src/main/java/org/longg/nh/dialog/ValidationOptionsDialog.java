package org.longg.nh.dialog;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;
import org.longg.nh.model.ValidationOption;
import org.longg.nh.util.JavaClassAnalyzer.ClassField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.List;

public class ValidationOptionsDialog extends DialogWrapper {
    
    private final ValidationOption validationOption;
    private final String fieldType;
    private final boolean isCollection;
    
    // UI Components
    private JBCheckBox requiredCheckBox;
    private JBCheckBox notBlankCheckBox;
    private JBCheckBox notEmptyCheckBox;
    private JBCheckBox emailCheckBox;
    private JBCheckBox sizeCheckBox;
    private JBTextField minSizeField;
    private JBTextField maxSizeField;
    private JBCheckBox rangeCheckBox;
    private JBTextField minValueField;
    private JBTextField maxValueField;
    private JBCheckBox patternCheckBox;
    private JBTextField patternField;
    private JBCheckBox pastCheckBox;
    private JBCheckBox futureCheckBox;
    private JBCheckBox customMessageCheckBox;
    private JBTextField messageField;
    
    // Message fields for each validation type
    private JBTextField requiredMessageField;
    private JBTextField notBlankMessageField;
    private JBTextField notEmptyMessageField;
    private JBTextField emailMessageField;
    private JBTextField sizeMessageField;
    private JBTextField rangeMessageField;
    private JBTextField patternMessageField;
    private JBTextField pastMessageField;
    private JBTextField futureMessageField;
    
    public ValidationOptionsDialog(ValidationOption validationOption, String fieldType, boolean isCollection) {
        super(true); // Use modal dialog
        this.validationOption = validationOption;
        this.fieldType = fieldType;
        this.isCollection = isCollection;
        
        setTitle("Validation Options for: " + validationOption.getFieldName());
        init();
    }
    
    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(450, 500));
        
        // Create all components
        createComponents();
        
        // Load initial values
        loadValues();
        
        // Build form
        JPanel formPanel = buildForm();
        
        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void createComponents() {
        requiredCheckBox = new JBCheckBox("Required (@NotNull)");
        requiredCheckBox.setToolTipText("Field must not be null");
        
        notBlankCheckBox = new JBCheckBox("Not Blank (@NotBlank)");
        notBlankCheckBox.setToolTipText("String field must contain at least one non-whitespace character");
        notBlankCheckBox.setEnabled(fieldType.equals("String"));
        
        notEmptyCheckBox = new JBCheckBox("Not Empty (@NotEmpty)");
        notEmptyCheckBox.setToolTipText("Collection, Map, Array, or String must not be empty");
        notEmptyCheckBox.setEnabled(isCollection || fieldType.equals("String"));
        
        emailCheckBox = new JBCheckBox("Email (@Email)");
        emailCheckBox.setToolTipText("String must be a valid email address");
        emailCheckBox.setEnabled(fieldType.equals("String"));
        
        sizeCheckBox = new JBCheckBox("Size (@Size)");
        sizeCheckBox.setToolTipText("String length or Collection/Map/Array size must be between min and max");
        sizeCheckBox.setEnabled(fieldType.equals("String") || isCollection);
        
        minSizeField = new JBTextField("0");
        minSizeField.setEnabled(false);
        
        maxSizeField = new JBTextField("255");
        maxSizeField.setEnabled(false);
        
        sizeCheckBox.addItemListener(e -> {
            boolean selected = e.getStateChange() == ItemEvent.SELECTED;
            minSizeField.setEnabled(selected);
            maxSizeField.setEnabled(selected);
            sizeMessageField.setEnabled(selected);
        });
        
        rangeCheckBox = new JBCheckBox("Range (@Min/@Max or @Range)");
        rangeCheckBox.setToolTipText("Numeric value must be between min and max");
        rangeCheckBox.setEnabled(isNumericType(fieldType));
        
        minValueField = new JBTextField("0");
        minValueField.setEnabled(false);
        
        maxValueField = new JBTextField("100");
        maxValueField.setEnabled(false);
        
        rangeCheckBox.addItemListener(e -> {
            boolean selected = e.getStateChange() == ItemEvent.SELECTED;
            minValueField.setEnabled(selected);
            maxValueField.setEnabled(selected);
            rangeMessageField.setEnabled(selected);
        });
        
        patternCheckBox = new JBCheckBox("Pattern (@Pattern)");
        patternCheckBox.setToolTipText("String must match the regular expression pattern");
        patternCheckBox.setEnabled(fieldType.equals("String"));
        
        patternField = new JBTextField("");
        patternField.setEnabled(false);
        
        patternCheckBox.addItemListener(e -> {
            boolean selected = e.getStateChange() == ItemEvent.SELECTED;
            patternField.setEnabled(selected);
            patternMessageField.setEnabled(selected);
        });
        
        pastCheckBox = new JBCheckBox("Past (@Past)");
        pastCheckBox.setToolTipText("Date must be in the past");
        pastCheckBox.setEnabled(isDateType(fieldType));
        
        pastCheckBox.addItemListener(e -> {
            pastMessageField.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
        });
        
        futureCheckBox = new JBCheckBox("Future (@Future)");
        futureCheckBox.setToolTipText("Date must be in the future");
        futureCheckBox.setEnabled(isDateType(fieldType));
        
        futureCheckBox.addItemListener(e -> {
            futureMessageField.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
        });
        
        customMessageCheckBox = new JBCheckBox("Use Custom Message For All Validations");
        customMessageCheckBox.setToolTipText("Use a single custom message for all validations instead of individual messages");
        
        messageField = new JBTextField("");
        messageField.setEnabled(false);
        
        customMessageCheckBox.addItemListener(e -> {
            boolean selected = e.getStateChange() == ItemEvent.SELECTED;
            messageField.setEnabled(selected);
            
            // Enable/disable individual message fields based on custom message checkbox
            setIndividualMessageFieldsEnabled(!selected);
        });
        
        // Create individual message fields
        requiredMessageField = new JBTextField(validationOption.getRequiredMessage());
        requiredMessageField.setEnabled(validationOption.isRequired());
        
        notBlankMessageField = new JBTextField(validationOption.getNotBlankMessage());
        notBlankMessageField.setEnabled(validationOption.isNotBlank() && fieldType.equals("String"));
        
        notEmptyMessageField = new JBTextField(validationOption.getNotEmptyMessage());
        notEmptyMessageField.setEnabled(validationOption.isNotEmpty() && (isCollection || fieldType.equals("String")));
        
        emailMessageField = new JBTextField(validationOption.getEmailMessage());
        emailMessageField.setEnabled(validationOption.isEmail() && fieldType.equals("String"));
        
        sizeMessageField = new JBTextField(validationOption.getSizeMessage());
        sizeMessageField.setEnabled(validationOption.isSize() && (fieldType.equals("String") || isCollection));
        
        rangeMessageField = new JBTextField(validationOption.getRangeMessage());
        rangeMessageField.setEnabled(validationOption.isRange() && isNumericType(fieldType));
        
        patternMessageField = new JBTextField(validationOption.getPatternMessage());
        patternMessageField.setEnabled(validationOption.isPattern() && fieldType.equals("String"));
        
        pastMessageField = new JBTextField(validationOption.getPastMessage());
        pastMessageField.setEnabled(validationOption.isPast() && isDateType(fieldType));
        
        futureMessageField = new JBTextField(validationOption.getFutureMessage());
        futureMessageField.setEnabled(validationOption.isFuture() && isDateType(fieldType));
        
        // Add listeners to enable message fields when validation is enabled
        requiredCheckBox.addItemListener(e -> {
            requiredMessageField.setEnabled(e.getStateChange() == ItemEvent.SELECTED && !customMessageCheckBox.isSelected());
        });
        
        notBlankCheckBox.addItemListener(e -> {
            notBlankMessageField.setEnabled(e.getStateChange() == ItemEvent.SELECTED && !customMessageCheckBox.isSelected());
        });
        
        notEmptyCheckBox.addItemListener(e -> {
            notEmptyMessageField.setEnabled(e.getStateChange() == ItemEvent.SELECTED && !customMessageCheckBox.isSelected());
        });
        
        emailCheckBox.addItemListener(e -> {
            emailMessageField.setEnabled(e.getStateChange() == ItemEvent.SELECTED && !customMessageCheckBox.isSelected());
        });
    }
    
    private void setIndividualMessageFieldsEnabled(boolean enabled) {
        requiredMessageField.setEnabled(enabled && requiredCheckBox.isSelected());
        notBlankMessageField.setEnabled(enabled && notBlankCheckBox.isSelected());
        notEmptyMessageField.setEnabled(enabled && notEmptyCheckBox.isSelected());
        emailMessageField.setEnabled(enabled && emailCheckBox.isSelected());
        sizeMessageField.setEnabled(enabled && sizeCheckBox.isSelected());
        rangeMessageField.setEnabled(enabled && rangeCheckBox.isSelected());
        patternMessageField.setEnabled(enabled && patternCheckBox.isSelected());
        pastMessageField.setEnabled(enabled && pastCheckBox.isSelected());
        futureMessageField.setEnabled(enabled && futureCheckBox.isSelected());
    }
    
    private void loadValues() {
        requiredCheckBox.setSelected(validationOption.isRequired());
        notBlankCheckBox.setSelected(validationOption.isNotBlank());
        notEmptyCheckBox.setSelected(validationOption.isNotEmpty());
        emailCheckBox.setSelected(validationOption.isEmail());
        sizeCheckBox.setSelected(validationOption.isSize());
        minSizeField.setText(String.valueOf(validationOption.getMinSize()));
        maxSizeField.setText(String.valueOf(validationOption.getMaxSize()));
        minSizeField.setEnabled(validationOption.isSize());
        maxSizeField.setEnabled(validationOption.isSize());
        
        rangeCheckBox.setSelected(validationOption.isRange());
        minValueField.setText(validationOption.getMin());
        maxValueField.setText(validationOption.getMax());
        minValueField.setEnabled(validationOption.isRange());
        maxValueField.setEnabled(validationOption.isRange());
        
        patternCheckBox.setSelected(validationOption.isPattern());
        patternField.setText(validationOption.getPatternValue());
        patternField.setEnabled(validationOption.isPattern());
        
        pastCheckBox.setSelected(validationOption.isPast());
        futureCheckBox.setSelected(validationOption.isFuture());
        
        customMessageCheckBox.setSelected(validationOption.isCustomMessage());
        messageField.setText(validationOption.getMessage());
        messageField.setEnabled(validationOption.isCustomMessage());
        
        // Load message values
        requiredMessageField.setText(validationOption.getRequiredMessage());
        notBlankMessageField.setText(validationOption.getNotBlankMessage());
        notEmptyMessageField.setText(validationOption.getNotEmptyMessage());
        emailMessageField.setText(validationOption.getEmailMessage());
        sizeMessageField.setText(validationOption.getSizeMessage());
        rangeMessageField.setText(validationOption.getRangeMessage());
        patternMessageField.setText(validationOption.getPatternMessage());
        pastMessageField.setText(validationOption.getPastMessage());
        futureMessageField.setText(validationOption.getFutureMessage());
        
        // Disable individual message fields if using custom message
        setIndividualMessageFieldsEnabled(!validationOption.isCustomMessage());
    }
    
    private JPanel buildForm() {
        FormBuilder builder = FormBuilder.createFormBuilder();
        
        // Add components to form based on field type
        builder.addComponent(new JBLabel("Field: " + validationOption.getFieldName()));
        builder.addComponent(new JBLabel("Type: " + validationOption.getFieldType()));
        builder.addSeparator();
        
        // Required validation
        builder.addComponent(requiredCheckBox);
        JPanel requiredMessagePanel = new JPanel(new BorderLayout());
        requiredMessagePanel.add(new JBLabel("Message: "), BorderLayout.WEST);
        requiredMessagePanel.add(requiredMessageField, BorderLayout.CENTER);
        builder.addComponentToRightColumn(requiredMessagePanel);
        
        if (fieldType.equals("String")) {
            // String validations
            builder.addComponent(notBlankCheckBox);
            JPanel notBlankMessagePanel = new JPanel(new BorderLayout());
            notBlankMessagePanel.add(new JBLabel("Message: "), BorderLayout.WEST);
            notBlankMessagePanel.add(notBlankMessageField, BorderLayout.CENTER);
            builder.addComponentToRightColumn(notBlankMessagePanel);
            
            builder.addComponent(notEmptyCheckBox);
            JPanel notEmptyMessagePanel = new JPanel(new BorderLayout());
            notEmptyMessagePanel.add(new JBLabel("Message: "), BorderLayout.WEST);
            notEmptyMessagePanel.add(notEmptyMessageField, BorderLayout.CENTER);
            builder.addComponentToRightColumn(notEmptyMessagePanel);
            
            builder.addComponent(emailCheckBox);
            JPanel emailMessagePanel = new JPanel(new BorderLayout());
            emailMessagePanel.add(new JBLabel("Message: "), BorderLayout.WEST);
            emailMessagePanel.add(emailMessageField, BorderLayout.CENTER);
            builder.addComponentToRightColumn(emailMessagePanel);
            
            // Size validation
            builder.addComponent(sizeCheckBox);
            
            JPanel sizePanel = new JPanel(new GridLayout(2, 4, 5, 5));
            sizePanel.add(new JBLabel("Min Size:"));
            sizePanel.add(minSizeField);
            sizePanel.add(new JBLabel("Max Size:"));
            sizePanel.add(maxSizeField);
            
            sizePanel.add(new JBLabel("Message:"));
            sizePanel.add(sizeMessageField);
            sizePanel.add(new JBLabel("")); // Empty cell for alignment
            sizePanel.add(new JBLabel("")); // Empty cell for alignment
            
            builder.addComponentToRightColumn(sizePanel);
            
            // Pattern validation
            builder.addComponent(patternCheckBox);
            JPanel patternPanel = new JPanel(new GridLayout(2, 2, 5, 5));
            patternPanel.add(new JBLabel("Pattern: "));
            patternPanel.add(patternField);
            patternPanel.add(new JBLabel("Message: "));
            patternPanel.add(patternMessageField);
            builder.addComponentToRightColumn(patternPanel);
        }
        
        if (isCollection) {
            // Collection validations
            builder.addComponent(notEmptyCheckBox);
            JPanel notEmptyMessagePanel = new JPanel(new BorderLayout());
            notEmptyMessagePanel.add(new JBLabel("Message: "), BorderLayout.WEST);
            notEmptyMessagePanel.add(notEmptyMessageField, BorderLayout.CENTER);
            builder.addComponentToRightColumn(notEmptyMessagePanel);
            
            // Size validation
            builder.addComponent(sizeCheckBox);
            
            JPanel sizePanel = new JPanel(new GridLayout(2, 4, 5, 5));
            sizePanel.add(new JBLabel("Min Size:"));
            sizePanel.add(minSizeField);
            sizePanel.add(new JBLabel("Max Size:"));
            sizePanel.add(maxSizeField);
            
            sizePanel.add(new JBLabel("Message:"));
            sizePanel.add(sizeMessageField);
            sizePanel.add(new JBLabel("")); // Empty cell for alignment
            sizePanel.add(new JBLabel("")); // Empty cell for alignment
            
            builder.addComponentToRightColumn(sizePanel);
        }
        
        if (isNumericType(fieldType)) {
            // Numeric validations
            builder.addComponent(rangeCheckBox);
            
            JPanel rangePanel = new JPanel(new GridLayout(2, 4, 5, 5));
            rangePanel.add(new JBLabel("Min:"));
            rangePanel.add(minValueField);
            rangePanel.add(new JBLabel("Max:"));
            rangePanel.add(maxValueField);
            
            rangePanel.add(new JBLabel("Message:"));
            rangePanel.add(rangeMessageField);
            rangePanel.add(new JBLabel("")); // Empty cell for alignment
            rangePanel.add(new JBLabel("")); // Empty cell for alignment
            
            builder.addComponentToRightColumn(rangePanel);
        }
        
        if (isDateType(fieldType)) {
            // Date validations
            builder.addComponent(pastCheckBox);
            JPanel pastMessagePanel = new JPanel(new BorderLayout());
            pastMessagePanel.add(new JBLabel("Message: "), BorderLayout.WEST);
            pastMessagePanel.add(pastMessageField, BorderLayout.CENTER);
            builder.addComponentToRightColumn(pastMessagePanel);
            
            builder.addComponent(futureCheckBox);
            JPanel futureMessagePanel = new JPanel(new BorderLayout());
            futureMessagePanel.add(new JBLabel("Message: "), BorderLayout.WEST);
            futureMessagePanel.add(futureMessageField, BorderLayout.CENTER);
            builder.addComponentToRightColumn(futureMessagePanel);
        }
        
        builder.addSeparator();
        builder.addComponent(customMessageCheckBox);
        
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.add(new JBLabel("Custom Message: "), BorderLayout.WEST);
        messagePanel.add(messageField, BorderLayout.CENTER);
        builder.addComponentToRightColumn(messagePanel);
        
        JPanel formPanel = builder.getPanel();
        formPanel.setBorder(JBUI.Borders.empty(10));
        
        return formPanel;
    }
    
    private boolean isNumericType(String type) {
        List<String> numericTypes = Arrays.asList("int", "Integer", "long", "Long", 
                "double", "Double", "float", "Float", "BigDecimal", "short", "Short", 
                "byte", "Byte");
        return numericTypes.contains(type);
    }
    
    private boolean isDateType(String type) {
        return type.contains("Date") || type.contains("LocalDate") || 
               type.contains("LocalDateTime") || type.contains("Calendar");
    }
    
    @Override
    protected void doOKAction() {
        // Save values back to the validation option
        validationOption.setRequired(requiredCheckBox.isSelected());
        validationOption.setNotBlank(notBlankCheckBox.isSelected());
        validationOption.setNotEmpty(notEmptyCheckBox.isSelected());
        validationOption.setEmail(emailCheckBox.isSelected());
        
        validationOption.setSize(sizeCheckBox.isSelected());
        try {
            validationOption.setMinSize(Integer.parseInt(minSizeField.getText()));
            validationOption.setMaxSize(Integer.parseInt(maxSizeField.getText()));
        } catch (NumberFormatException e) {
            // Use default values if parsing fails
            validationOption.setMinSize(0);
            validationOption.setMaxSize(255);
        }
        
        validationOption.setRange(rangeCheckBox.isSelected());
        validationOption.setMin(minValueField.getText());
        validationOption.setMax(maxValueField.getText());
        
        validationOption.setPattern(patternCheckBox.isSelected());
        validationOption.setPatternValue(patternField.getText());
        
        validationOption.setPast(pastCheckBox.isSelected());
        validationOption.setFuture(futureCheckBox.isSelected());
        
        validationOption.setCustomMessage(customMessageCheckBox.isSelected());
        validationOption.setMessage(messageField.getText());
        
        // Save individual validation messages
        validationOption.setRequiredMessage(requiredMessageField.getText());
        validationOption.setNotBlankMessage(notBlankMessageField.getText());
        validationOption.setNotEmptyMessage(notEmptyMessageField.getText());
        validationOption.setEmailMessage(emailMessageField.getText());
        validationOption.setSizeMessage(sizeMessageField.getText());
        validationOption.setRangeMessage(rangeMessageField.getText());
        validationOption.setPatternMessage(patternMessageField.getText());
        validationOption.setPastMessage(pastMessageField.getText());
        validationOption.setFutureMessage(futureMessageField.getText());
        
        super.doOKAction();
    }
} 