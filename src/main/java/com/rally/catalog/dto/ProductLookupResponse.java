package com.rally.catalog.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ProductLookupResponse {

    private Map<String, ProductLookupItem> found;
    private List<String> notFound;
}
