package com.anastasia.Anastasia_BackEnd.model.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.EntityModel;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {

    private PagedModel<EntityModel<T>> data;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int size;
    private boolean isFirst;
    private boolean isLast;
    private int[] pageSizeOptions = {10, 20, 50}; // Add default options
}
