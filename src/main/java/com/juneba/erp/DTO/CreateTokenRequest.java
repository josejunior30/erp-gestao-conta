package com.juneba.erp.DTO;

public record CreateTokenRequest(String clientUserId, Boolean avoidDuplicates, String itemId, String oauthRedirectUri) {

}