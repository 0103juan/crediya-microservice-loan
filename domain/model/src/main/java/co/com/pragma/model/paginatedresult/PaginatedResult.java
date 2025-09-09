package co.com.pragma.model.paginatedresult;

import java.util.List;

public record PaginatedResult<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize
) {}