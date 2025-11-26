package com.juneba.erp.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccountLiteDto(
        String id,
        String name,
        String type,
        String last4
) { }