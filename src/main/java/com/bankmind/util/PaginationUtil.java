package com.bankmind.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public class PaginationUtil {

    private PaginationUtil() {}

    public static PageRequest buildPageRequest(int page, int size, String sortBy, String sortDirection) {
        int limitSize = Math.min(Math.max(size, 1), 100);
        int pageIndex = Math.max(page, 0);

        Sort sort = Sort.unsorted();
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            Sort.Direction direction = Sort.Direction.ASC;
            if (sortDirection != null && "desc".equalsIgnoreCase(sortDirection.trim())) {
                direction = Sort.Direction.DESC;
            }
            sort = Sort.by(direction, sortBy);
        }

        return PageRequest.of(pageIndex, limitSize, sort);
    }
}
