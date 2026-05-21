package com.untitled.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class CategoryUpdateRequest {
    @NotBlank
    @Size(max = 32)
    private String key;

    @NotBlank
    @Size(max = 64)
    private String name;

    @Size(max = 20)
    private String status;

    private Integer sort;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
