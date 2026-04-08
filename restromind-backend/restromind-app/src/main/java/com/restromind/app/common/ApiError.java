package com.restromind.app.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiError {
    private int status;
    private String message;
    private List<String> fieldErrors;

    public ApiError(int status, String message) {
        this.status = status;
        this.message = message;
    }
}
