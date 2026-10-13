package com.eventhub.utilities.result;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {
    private boolean success;
    private int code;
    private String message;
    private List<T> errors;
    private double amount;

    @JsonCreator
    public Result(
            @JsonProperty("success") boolean success,
            @JsonProperty("code") int code,
            @JsonProperty("message") String message,
            @JsonProperty("errors") List<T> errors
    ) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.errors = errors;
        this.amount = 0;
    }
    public Result(boolean success) {
        this(success, 0, null, null);
    }
    public Result(boolean success, int code) {
        this(success, code, null, null);
    }
    public Result(boolean success, int code, String message) {
        this(success, code, message, null);
    }
    public Result(boolean success, int code, List<T> errors) {
        this(success, code, null, errors);
    }
    public Result(boolean success, int code, String message, double amount) {
        this(success, code, message, null);
        this.amount = amount;
    }
    public Result(boolean success, double amount) {
        this(success, 200, null, null);
        this.amount = amount;
    }
}