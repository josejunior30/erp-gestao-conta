package com.juneba.erp.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.juneba.erp.DTO.ItemDetailsDto;
import com.juneba.erp.service.PluggyItemService;
import com.juneba.erp.service.PluggyTransactionsHttpService;

@RestController
@RequestMapping("/api/pluggy")
public class ItemTransactionsController {

	private final PluggyTransactionsHttpService service;
	private final PluggyItemService pluggyItemService;
	
	public ItemTransactionsController(PluggyTransactionsHttpService service, PluggyItemService pluggyItemService) {
		this.service = service;
		this.pluggyItemService = pluggyItemService;
	}


	@GetMapping(value = "/items/{itemId}/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
	  public ResponseEntity<JsonNode> listAllByItemId(
	      @PathVariable String itemId,
	      @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate from,
	      @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate to,
	      @RequestParam(required = false) String status,
	      @RequestParam(required = false) Integer pageSize
	  ) {
	    return ResponseEntity.ok(
	        service.fetchAllTransactionsByItemId(itemId, from, to, status, pageSize)
	    );
	  }
	  @GetMapping(value = "/items/{itemId}", produces = MediaType.APPLICATION_JSON_VALUE)
	  public ResponseEntity<ItemDetailsDto> getItem(@PathVariable String itemId) {
	    return ResponseEntity.ok(pluggyItemService.fetchItemDetails(itemId));
	  }
}
