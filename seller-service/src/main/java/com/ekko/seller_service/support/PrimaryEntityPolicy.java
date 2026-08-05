package com.ekko.seller_service.support;

import org.springframework.stereotype.Component;

@Component
public class PrimaryEntityPolicy {

    public void promote(Runnable clearAllPrimaries, Runnable markAsPrimary) {
        clearAllPrimaries.run();
        markAsPrimary.run();
    }
}