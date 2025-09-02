package com.carddemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Data Transfer Object for batch job parameters and configuration.
 * 
 * This DTO replaces JCL job parameter passing mechanism for COBOL batch programs
 * converted to Spring Batch jobs. It handles parameter names, values, and types
 * with validation rules for batch job execution.
 * 
 * Supports batch jobs converted from COBOL programs like:
 * - CBTRN01C (Daily Transaction Processing)
 * - CBACT01C (Account Data Processing)
 * - Other batch processing jobs
 */
public class BatchJobParametersDto {

    /**
     * Map containing all job parameters with their names and values.
     * Replaces JCL parameter specifications from mainframe batch jobs.
     */
    private Map<String, JobParameterValue> parameters;

    /**
     * Job name for identification and validation purposes.
     */
    @NotBlank(message = "Job name is required")
    private String jobName;

    /**
     * Run date for the batch job execution.
     */
    @NotNull(message = "Run date is required")
    private LocalDate runDate;

    /**
     * Constructor initializes empty parameter map.
     */
    public BatchJobParametersDto() {
        this.parameters = new HashMap<>();
    }

    /**
     * Constructor with job name and run date.
     */
    public BatchJobParametersDto(String jobName, LocalDate runDate) {
        this();
        this.jobName = jobName;
        this.runDate = runDate;
    }

    /**
     * Gets all job parameters as a map.
     * 
     * @return Map containing all parameter names and their values
     */
    public Map<String, JobParameterValue> getParameters() {
        return new HashMap<>(parameters);
    }

    /**
     * Adds a string parameter to the job parameters.
     * 
     * @param name Parameter name (e.g., "inputFile", "outputPath")
     * @param value Parameter value
     * @param identifying Whether this parameter identifies the job instance
     */
    public void addParameter(String name, String value, boolean identifying) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Parameter name cannot be null or empty");
        }
        parameters.put(name, new JobParameterValue(value, String.class, identifying));
    }

    /**
     * Adds a long parameter to the job parameters.
     * 
     * @param name Parameter name
     * @param value Parameter value
     * @param identifying Whether this parameter identifies the job instance
     */
    public void addParameter(String name, Long value, boolean identifying) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Parameter name cannot be null or empty");
        }
        parameters.put(name, new JobParameterValue(value, Long.class, identifying));
    }

    /**
     * Adds a double parameter to the job parameters.
     * 
     * @param name Parameter name
     * @param value Parameter value
     * @param identifying Whether this parameter identifies the job instance
     */
    public void addParameter(String name, Double value, boolean identifying) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Parameter name cannot be null or empty");
        }
        parameters.put(name, new JobParameterValue(value, Double.class, identifying));
    }

    /**
     * Adds a date parameter to the job parameters.
     * 
     * @param name Parameter name
     * @param value Parameter value
     * @param identifying Whether this parameter identifies the job instance
     */
    public void addParameter(String name, LocalDateTime value, boolean identifying) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Parameter name cannot be null or empty");
        }
        parameters.put(name, new JobParameterValue(value, LocalDateTime.class, identifying));
    }

    /**
     * Removes a parameter from the job parameters.
     * 
     * @param name Parameter name to remove
     * @return true if parameter was removed, false if it didn't exist
     */
    public boolean removeParameter(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        return parameters.remove(name) != null;
    }

    /**
     * Gets the value of a specific parameter.
     * 
     * @param name Parameter name
     * @return Parameter value or null if not found
     */
    public Object getParameterValue(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        JobParameterValue paramValue = parameters.get(name);
        return paramValue != null ? paramValue.getValue() : null;
    }

    /**
     * Gets the type of a specific parameter.
     * 
     * @param name Parameter name
     * @return Parameter type class or null if not found
     */
    public Class<?> getParameterType(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        JobParameterValue paramValue = parameters.get(name);
        return paramValue != null ? paramValue.getType() : null;
    }

    /**
     * Checks if a parameter is identifying.
     * 
     * @param name Parameter name
     * @return true if parameter is identifying, false otherwise
     */
    public boolean isParameterIdentifying(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        JobParameterValue paramValue = parameters.get(name);
        return paramValue != null && paramValue.isIdentifying();
    }

    /**
     * Validates the batch job parameters.
     * 
     * Performs validation including:
     * - Required parameters are present
     * - Parameter values are valid for their types
     * - Special validation for converted COBOL batch jobs
     * 
     * @throws IllegalStateException if validation fails
     */
    public void validate() {
        if (jobName == null || jobName.trim().isEmpty()) {
            throw new IllegalStateException("Job name is required");
        }
        
        if (runDate == null) {
            throw new IllegalStateException("Run date is required");
        }

        // Validate specific parameters based on job type
        if (jobName.contains("CBTRN01C") || jobName.toLowerCase().contains("transaction")) {
            validateTransactionJobParameters();
        } else if (jobName.contains("CBACT01C") || jobName.toLowerCase().contains("account")) {
            validateAccountJobParameters();
        }

        // Validate parameter values are not null
        for (Map.Entry<String, JobParameterValue> entry : parameters.entrySet()) {
            if (entry.getValue().getValue() == null) {
                throw new IllegalStateException("Parameter '" + entry.getKey() + "' has null value");
            }
        }
    }

    /**
     * Converts this DTO to Spring Batch JobParameters.
     * 
     * @return JobParameters instance for Spring Batch execution
     */
    public JobParameters toJobParameters() {
        JobParametersBuilder builder = new JobParametersBuilder();
        
        // Add run date as an identifying parameter
        builder.addLocalDateTime("runDate", runDate.atStartOfDay(), true);
        
        // Add job name as identifying parameter
        builder.addString("jobName", jobName, true);
        
        // Add all configured parameters
        for (Map.Entry<String, JobParameterValue> entry : parameters.entrySet()) {
            JobParameterValue paramValue = entry.getValue();
            Object value = paramValue.getValue();
            boolean identifying = paramValue.isIdentifying();
            
            if (value instanceof String) {
                builder.addString(entry.getKey(), (String) value, identifying);
            } else if (value instanceof Long) {
                builder.addLong(entry.getKey(), (Long) value, identifying);
            } else if (value instanceof Double) {
                builder.addDouble(entry.getKey(), (Double) value, identifying);
            } else if (value instanceof LocalDateTime) {
                builder.addLocalDateTime(entry.getKey(), (LocalDateTime) value, identifying);
            }
        }
        
        return builder.toJobParameters();
    }

    /**
     * Creates a BatchJobParametersDto from Spring Batch JobParameters.
     * 
     * @param jobParameters Spring Batch JobParameters
     * @param jobName Job name
     * @return BatchJobParametersDto instance
     */
    public static BatchJobParametersDto fromJobParameters(JobParameters jobParameters, String jobName) {
        BatchJobParametersDto dto = new BatchJobParametersDto();
        dto.setJobName(jobName);
        
        // Extract run date if present
        if (jobParameters.getLocalDateTime("runDate") != null) {
            dto.setRunDate(jobParameters.getLocalDateTime("runDate").toLocalDate());
        } else {
            dto.setRunDate(LocalDate.now());
        }
        
        // Convert all parameters
        for (Map.Entry<String, JobParameter<?>> entry : jobParameters.getParameters().entrySet()) {
            String paramName = entry.getKey();
            JobParameter<?> jobParam = entry.getValue();
            
            // Skip system parameters
            if ("runDate".equals(paramName) || "jobName".equals(paramName)) {
                continue;
            }
            
            Object value = jobParam.getValue();
            boolean identifying = jobParam.isIdentifying();
            
            if (value instanceof String) {
                dto.addParameter(paramName, (String) value, identifying);
            } else if (value instanceof Long) {
                dto.addParameter(paramName, (Long) value, identifying);
            } else if (value instanceof Double) {
                dto.addParameter(paramName, (Double) value, identifying);
            } else if (value instanceof LocalDateTime) {
                dto.addParameter(paramName, (LocalDateTime) value, identifying);
            }
        }
        
        return dto;
    }

    /**
     * Gets parameter names as a set.
     * 
     * @return Set of parameter names
     */
    public Set<String> getParameterNames() {
        return parameters.keySet();
    }

    /**
     * Checks if parameters map is empty.
     * 
     * @return true if no parameters are set
     */
    public boolean isEmpty() {
        return parameters.isEmpty();
    }

    /**
     * Gets the number of parameters.
     * 
     * @return parameter count
     */
    public int size() {
        return parameters.size();
    }

    // Private validation methods

    /**
     * Validates parameters specific to transaction processing jobs (CBTRN01C).
     */
    private void validateTransactionJobParameters() {
        // Transaction jobs typically need input file path
        if (!parameters.containsKey("inputFilePath") && !parameters.containsKey("dailyTransactionFile")) {
            throw new IllegalStateException("Transaction processing jobs require input file parameter");
        }
    }

    /**
     * Validates parameters specific to account processing jobs (CBACT01C).
     */
    private void validateAccountJobParameters() {
        // Account jobs typically need account file path or date range
        if (!parameters.containsKey("accountFilePath") && !parameters.containsKey("processDate")) {
            throw new IllegalStateException("Account processing jobs require file path or process date parameter");
        }
    }

    // Getters and Setters

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public LocalDate getRunDate() {
        return runDate;
    }

    public void setRunDate(LocalDate runDate) {
        this.runDate = runDate;
    }

    /**
     * Inner class to hold parameter value, type, and identifying flag.
     */
    private static class JobParameterValue {
        private final Object value;
        private final Class<?> type;
        private final boolean identifying;

        public JobParameterValue(Object value, Class<?> type, boolean identifying) {
            this.value = value;
            this.type = type;
            this.identifying = identifying;
        }

        public Object getValue() {
            return value;
        }

        public Class<?> getType() {
            return type;
        }

        public boolean isIdentifying() {
            return identifying;
        }
    }

    @Override
    public String toString() {
        return "BatchJobParametersDto{" +
                "jobName='" + jobName + '\'' +
                ", runDate=" + runDate +
                ", parameters=" + parameters.keySet() +
                '}';
    }
}