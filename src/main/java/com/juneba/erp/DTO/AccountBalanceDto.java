package com.juneba.erp.DTO;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccountBalanceDto(
        String id,
        String name,
        String type,
        String currencyCode,
        BigDecimal balance,
        BigDecimal availableBalance
) { }