package com.juneba.erp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.juneba.erp.entities.User;

public interface UserRepository extends  JpaRepository<User, Long>{

}
