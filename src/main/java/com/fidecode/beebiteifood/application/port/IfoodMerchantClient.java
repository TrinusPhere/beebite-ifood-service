package com.fidecode.beebiteifood.application.port;

import com.fidecode.beebiteifood.application.dto.IfoodInterruptionResponse;
import com.fidecode.beebiteifood.application.dto.IfoodMerchantStatusResponse;

import java.util.List;

public interface IfoodMerchantClient {
    List<IfoodMerchantStatusResponse> getMerchantStatus(String accessToken, String merchantId);
    List<IfoodInterruptionResponse> getInterruptions(String accessToken, String merchantId);
    void createInterruption(String accessToken, String merchantId, String description, String start, String end);
    void deleteInterruption(String accessToken, String merchantId, String interruptionId);
}
