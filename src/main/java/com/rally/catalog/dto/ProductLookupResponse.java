package com.rally.catalog.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class ProductLookupResponse {

    private Map<UUID, ProductLookupItem> found;
    private List<UUID> notFound;
}
