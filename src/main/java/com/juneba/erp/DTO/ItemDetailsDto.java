package com.juneba.erp.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ItemDetailsDto(String id, ConnectorDto connector) {
	
}
