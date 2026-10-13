package com.eventhub.types.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PageInfoResponse {
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private boolean hasNext;
}
