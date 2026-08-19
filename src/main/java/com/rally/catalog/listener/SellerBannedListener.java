package com.rally.catalog.listener;

import com.rally.catalog.event.SellerBannedEvent;
import com.rally.catalog.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SellerBannedListener {

    private static final Logger log = LoggerFactory.getLogger(SellerBannedListener.class);

    private final ProductService productService;

    public SellerBannedListener(ProductService productService) {
        this.productService = productService;
    }

    @KafkaListener(
            topics = "seller.banned",
            containerFactory = "sellerBannedKafkaListenerContainerFactory"
    )
    public void handleSellerBanned(SellerBannedEvent event) {
        log.warn("Seller banned event received sellerId={} reason={}",
                event.getSellerId(), event.getReason());
        int updated = productService.setSellerProductsInvisible(event.getSellerId());
        log.info("Set {} products to invisible for banned sellerId={}", updated, event.getSellerId());
    }
}
