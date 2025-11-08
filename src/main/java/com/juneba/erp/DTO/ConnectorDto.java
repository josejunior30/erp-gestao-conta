package com.juneba.erp.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConnectorDto(String id, String name, String primaryColor, String institutionUrl, String country,
		String type) {
}
