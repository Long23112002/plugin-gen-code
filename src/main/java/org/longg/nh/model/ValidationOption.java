package org.longg.nh.model;

import java.io.Serializable;

/**
 * Represents validation options for a field.
 */
public class ValidationOption implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String fieldName;
    private String fieldType;
    
    // General validation
    private boolean required;
    private String requiredMessage;
    
    // String validations
    private boolean notBlank;
    private String notBlankMessage;
    
    private boolean notEmpty;
    private String notEmptyMessage;
    
    private boolean email;
    private String emailMessage;
    
    // Size validations
    private boolean size;
    private int minSize;
    private int maxSize;
    private String sizeMessage;
    
    // Range validations
    private boolean range;
    private String min;
    private String max;
    private String rangeMessage;
    
    // Pattern validation
    private boolean pattern;
    private String patternValue;
    private String patternMessage;
    
    // Date validations
    private boolean past;
    private String pastMessage;
    
    private boolean future;
    private String futureMessage;
    
    // Custom message
    private boolean customMessage;
    private String message;
    
    /**
     * Constructs a new ValidationOption with default values.
     */
    public ValidationOption() {
        this.minSize = 0;
        this.maxSize = 255;
        this.min = "0";
        this.max = "100";
        
        // Default messages
        this.requiredMessage = "Trường này là bắt buộc";
        this.notBlankMessage = "Trường này không được để trống";
        this.notEmptyMessage = "Trường này không được để trống";
        this.emailMessage = "Địa chỉ email không hợp lệ";
        this.sizeMessage = "Độ dài phải từ {min} đến {max} ký tự";
        this.rangeMessage = "Giá trị phải nằm trong khoảng từ {min} đến {max}";
        this.patternMessage = "Giá trị không đúng định dạng";
        this.pastMessage = "Ngày phải là ngày trong quá khứ";
        this.futureMessage = "Ngày phải là ngày trong tương lai";
        this.message = "";
    }
    
    /**
     * Constructs a new ValidationOption with field name and type.
     * 
     * @param fieldName the name of the field
     * @param fieldType the type of the field
     */
    public ValidationOption(String fieldName, String fieldType) {
        this();
        this.fieldName = fieldName;
        this.fieldType = fieldType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isNotBlank() {
        return notBlank;
    }

    public void setNotBlank(boolean notBlank) {
        this.notBlank = notBlank;
    }

    public boolean isNotEmpty() {
        return notEmpty;
    }

    public void setNotEmpty(boolean notEmpty) {
        this.notEmpty = notEmpty;
    }

    public boolean isEmail() {
        return email;
    }

    public void setEmail(boolean email) {
        this.email = email;
    }

    public boolean isSize() {
        return size;
    }

    public void setSize(boolean size) {
        this.size = size;
    }

    public int getMinSize() {
        return minSize;
    }

    public void setMinSize(int minSize) {
        this.minSize = minSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public boolean isRange() {
        return range;
    }

    public void setRange(boolean range) {
        this.range = range;
    }

    public String getMin() {
        return min;
    }

    public void setMin(String min) {
        this.min = min;
    }

    public String getMax() {
        return max;
    }

    public void setMax(String max) {
        this.max = max;
    }

    public boolean isPattern() {
        return pattern;
    }

    public void setPattern(boolean pattern) {
        this.pattern = pattern;
    }

    public String getPatternValue() {
        return patternValue;
    }

    public void setPatternValue(String patternValue) {
        this.patternValue = patternValue;
    }

    public boolean isPast() {
        return past;
    }

    public void setPast(boolean past) {
        this.past = past;
    }

    public boolean isFuture() {
        return future;
    }

    public void setFuture(boolean future) {
        this.future = future;
    }

    public boolean isCustomMessage() {
        return customMessage;
    }

    public void setCustomMessage(boolean customMessage) {
        this.customMessage = customMessage;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
    // Getters and setters for validation-specific messages
    
    public String getRequiredMessage() {
        return requiredMessage;
    }

    public void setRequiredMessage(String requiredMessage) {
        this.requiredMessage = requiredMessage;
    }

    public String getNotBlankMessage() {
        return notBlankMessage;
    }

    public void setNotBlankMessage(String notBlankMessage) {
        this.notBlankMessage = notBlankMessage;
    }

    public String getNotEmptyMessage() {
        return notEmptyMessage;
    }

    public void setNotEmptyMessage(String notEmptyMessage) {
        this.notEmptyMessage = notEmptyMessage;
    }

    public String getEmailMessage() {
        return emailMessage;
    }

    public void setEmailMessage(String emailMessage) {
        this.emailMessage = emailMessage;
    }

    public String getSizeMessage() {
        return sizeMessage;
    }

    public void setSizeMessage(String sizeMessage) {
        this.sizeMessage = sizeMessage;
    }

    public String getRangeMessage() {
        return rangeMessage;
    }

    public void setRangeMessage(String rangeMessage) {
        this.rangeMessage = rangeMessage;
    }

    public String getPatternMessage() {
        return patternMessage;
    }

    public void setPatternMessage(String patternMessage) {
        this.patternMessage = patternMessage;
    }

    public String getPastMessage() {
        return pastMessage;
    }

    public void setPastMessage(String pastMessage) {
        this.pastMessage = pastMessage;
    }

    public String getFutureMessage() {
        return futureMessage;
    }

    public void setFutureMessage(String futureMessage) {
        this.futureMessage = futureMessage;
    }
    
    /**
     * Gets the appropriate message for the given validation type.
     * If custom message is enabled, the general message is returned.
     * Otherwise, the validation-specific message is returned.
     * 
     * @param validationType the validation type
     * @return the appropriate message
     */
    public String getMessageForValidation(String validationType) {
        if (customMessage) {
            return message;
        }
        
        switch (validationType) {
            case "required":
                return requiredMessage;
            case "notBlank":
                return notBlankMessage;
            case "notEmpty":
                return notEmptyMessage;
            case "email":
                return emailMessage;
            case "size":
                String sizeMsg = sizeMessage;
                sizeMsg = sizeMsg.replace("{min}", String.valueOf(minSize));
                sizeMsg = sizeMsg.replace("{max}", String.valueOf(maxSize));
                return sizeMsg;
            case "range":
                String rangeMsg = rangeMessage;
                rangeMsg = rangeMsg.replace("{min}", min);
                rangeMsg = rangeMsg.replace("{max}", max);
                return rangeMsg;
            case "pattern":
                return patternMessage;
            case "past":
                return pastMessage;
            case "future":
                return futureMessage;
            default:
                return message;
        }
    }
} 