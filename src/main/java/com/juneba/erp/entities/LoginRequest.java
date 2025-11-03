package com.juneba.erp.entities;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class LoginRequest {
	@Email(message="email invalido")
	@NotBlank(message = "email obrigatório")
	private String email;
	@NotBlank(message = "senha é obrigatória")
	@Size(min = 6, max = 128, message = "senha deve ter entre 6 e 128 caracteres")
	@Pattern(regexp = "^(?=(?:.*[A-Za-z]){6,})(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d]+$", message = "senha: mínimo 6 letras, com ao menos 1 maiúscula e 1 número (somente letras/dígitos)")
	private String password;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
