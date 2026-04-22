package com.a308.cutline.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    
    private String status;
    private T data;
    
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", data);
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>("error", null);
    }
    
    public static ApiResponse<String> errorWithMessage(String message) {
        return new ApiResponse<>("error", message);
    }
}