package com.juneba.erp.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.juneba.erp.DTO.ItemDetailsDto;
import com.juneba.erp.DTO.TransactionSummaryDto;
import com.juneba.erp.service.PluggyItemService;
import com.juneba.erp.service.PluggyTransactionsHttpService;

import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/pluggy")
public class ItemTransactionsController {

	private final PluggyTransactionsHttpService service;
	private final PluggyItemService pluggyItemService;
	
	public ItemTransactionsController(PluggyTransactionsHttpService service, PluggyItemService pluggyItemService) {
		this.service = service;
		this.pluggyItemService = pluggyItemService;
	}

	@GetMapping(value = "/accounts/{accountId}/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TransactionSummaryDto> listAllByAccountId(
            @PathVariable @NotBlank String accountId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ResponseEntity.ok(
                service.fetchAllTransactionsByAccountIdPretty(accountId, from, to, status, pageSize)
        );
    }
	 @GetMapping(value = "/items/{itemId}", produces = MediaType.APPLICATION_JSON_VALUE)
	    public ResponseEntity<ItemDetailsDto> getItem(@PathVariable @NotBlank String itemId) {
	        return ResponseEntity.ok(pluggyItemService.fetchItemDetails(itemId));
	    }
	 
	 // itens salvos no banco
	   @GetMapping(value = "/items", produces = MediaType.APPLICATION_JSON_VALUE)
	    public ResponseEntity<List<ItemDetailsDto>> listAllItemsFromDatabase() {
	        return ResponseEntity.ok(pluggyItemService.listAllItemDetailsFromDb());
	    }
	
}
