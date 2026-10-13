package com.eventhub.utilities.result;

import com.eventhub.types.response.PageInfoResponse;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DataResult<T> extends Result<T> {
    private T data;
    private PageInfoResponse pageInfo;

    @JsonCreator
    public DataResult(
            @JsonProperty("data") T data,
            @JsonProperty("success") boolean success,
            @JsonProperty("code") int code,
            @JsonProperty("message") String message,
            @JsonProperty("errors") List<T> errors
    ) {
        super(success, code, message, errors);
        this.data = data;
    }

    public DataResult(T data, boolean success, int code) {
        super(success, code);
        this.data = data;
    }

    public DataResult(T data, boolean success) {
        super(success, 0);
        this.data = data;
    }

    public DataResult(boolean success, String message) {
        super(success, 0, message);
        this.data = null;
    }
    public DataResult(boolean success, int code, String message) {
        super(success, code, message);
    }

    public DataResult(T data, boolean success, int code, PageInfoResponse pageInfo) {
        super(success, code);
        this.data = data;
        this.pageInfo = pageInfo;
    }

    public T getData() {
        return data;
    }

    public PageInfoResponse getPageInfo() {
        return pageInfo;
    }
}