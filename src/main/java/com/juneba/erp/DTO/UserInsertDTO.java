package com.juneba.erp.DTO;

public class UserInsertDTO extends UserDTO{

	private static final long serialVersionUID = 1L;
	
	private String password;
	
	public UserInsertDTO() {
		super();
	}

	 public UserInsertDTO(String firstName, String lastName, String email, String password) {
	        super(firstName, lastName, email); // Call the parent constructor
	        this.password = password;
	    }
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	

}
