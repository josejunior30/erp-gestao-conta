package com.juneba.erp.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserTransactionDto(
        String id,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm", timezone = "America/Sao_Paulo")
        LocalDateTime dateTime,    
        String description,
        String merchantName,
        String category,
        String type,
        BigDecimal amount,
        String amountFormatted,
        String currency,
        String status,
        AccountLiteDto account
) { }