package com.juneba.erp.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.juneba.erp.DTO.RoleDTO;
import com.juneba.erp.DTO.UserDTO;
import com.juneba.erp.DTO.UserInsertDTO;
import com.juneba.erp.entities.Role;
import com.juneba.erp.entities.User;
import com.juneba.erp.repository.RoleRepository;
import com.juneba.erp.repository.UserRepository;


@Service
public class UserService {
	
	
	@Autowired
	private UserRepository repository;
	
	@Autowired
	private RoleRepository roleRepository;
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	
	@Transactional(readOnly = true)
	public List<UserDTO> findAll() {
		List<User> list = repository.findAll();
		return list.stream()
		           .map(x -> new UserDTO(x))
		           .collect(Collectors.toList());
	}
	
	@Transactional(readOnly = true)
    public UserDTO findById(Long id) {
    	Optional<User> user = repository.findById(id);
    	User entity = user.get();
    	return new UserDTO(entity);
    }
	 @Transactional
	    public UserDTO insert(UserInsertDTO dto) {
	    	User entity = new User();
	    	copyDtoToEntity(dto, entity);
	    	entity.setPassword(passwordEncoder.encode(dto.getPassword()));
	    	entity = repository.save(entity);
	    	return new UserDTO(entity);
	    }
	    
	  @Transactional
	    public UserDTO update(Long id, UserDTO dto) {
	    	User entity = repository.getReferenceById(id);
	    	copyDtoToEntity(dto, entity);
	    	entity = repository.save(entity);
			return new UserDTO(entity);
	    }

	
	    
    public void delete(Long id) {
    	repository.deleteById(id);
    }
    private void copyDtoToEntity(UserDTO dto, User entity) {
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setEmail(dto.getEmail());
        Set<RoleDTO> roleDTOs = dto.getRoles();
        Set<Role> roles = roleDTOs.stream()
                .map(roleDto -> roleRepository.findById(roleDto.getId()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        entity.setRoles(roles);

        entity.setRoles(roles);
    }



}
