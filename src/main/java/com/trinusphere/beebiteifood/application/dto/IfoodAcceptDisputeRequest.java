package com.trinusphere.beebiteifood.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record IfoodAcceptDisputeRequest(
        String reason,
        String detailReason
) {}
