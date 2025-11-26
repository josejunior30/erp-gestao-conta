package com.juneba.erp.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransactionSummaryDto(
        String itemId,
        LocalDate from,
        LocalDate to,
        int totalCount,
        BigDecimal totalInflow,
        BigDecimal totalOutflow,
        BigDecimal net,
        List<UserTransactionDto> transactions
) { }