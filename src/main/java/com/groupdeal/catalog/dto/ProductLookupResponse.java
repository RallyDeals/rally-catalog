package com.groupdeal.catalog.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProductLookupResponse {

    private List<ProductLookupItem> found;
    private List<String> notFound;
}
