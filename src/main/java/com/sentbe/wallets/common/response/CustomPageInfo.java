package com.sentbe.wallets.common.response;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomPageInfo {

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static CustomPageInfo from(Page<?> page) {
        return CustomPageInfo.builder()
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .build();
    }
}
