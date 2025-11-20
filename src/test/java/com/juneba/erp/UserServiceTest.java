package com.juneba.erp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.ArgumentMatchers;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Optional;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.juneba.erp.DTO.RoleDTO;
import com.juneba.erp.DTO.UserDTO;
import com.juneba.erp.DTO.UserInsertDTO;
import com.juneba.erp.entities.Role;
import com.juneba.erp.entities.User;
import com.juneba.erp.repository.RoleRepository;
import com.juneba.erp.repository.UserRepository;
import com.juneba.erp.service.UserService;


public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testFindAll() {
        User user = new User(1L, "John", "Doe", "john.doe@example.com", "password");
        List<User> users = List.of(user);
        when(userRepository.findAll()).thenReturn(users);
        List<UserDTO> result = userService.findAll();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getFirstName());
    }

    @Test
    public void testFindById() {
        User user = new User(1L, "John", "Doe", "john.doe@example.com", "password");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        UserDTO result = userService.findById(1L);
        assertNotNull(result);
        assertEquals("john.doe@example.com", result.getEmail());
    }

    @Test
    public void testInsert() {
        UserInsertDTO dto = new UserInsertDTO("John", "Doe", "john.doe@example.com", "password123"); 
        User user = new User(null, "John", "Doe", "john.doe@example.com", "password123"); 
        when(userRepository.save(ArgumentMatchers.any(User.class))).thenReturn(user);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        UserDTO result = userService.insert(dto); 

        assertNotNull(result);
        assertEquals("john.doe@example.com", result.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }
    
    @Test
    public void testUpdate() {
     
        Long userId = 1L;
        Set<RoleDTO> roles = new HashSet<>();
        roles.add(new RoleDTO(1L, "ROLE_ADMIN")); 
        UserDTO dto = new UserDTO(userId, "John", "Doe", "john.doe@newexample.com", roles); 
        User existingUser = new User(userId, "John", "Doe", "john.doe@example.com", "password"); 
        when(userRepository.getReferenceById(userId)).thenReturn(existingUser); 
        when(userRepository.save(any(User.class))).thenReturn(existingUser); 
        
        Role role = new Role(1L, "ROLE_ADMIN"); 
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));  

    
        UserDTO result = userService.update(userId, dto);  

        assertNotNull(result);  
        assertEquals("john.doe@newexample.com", result.getEmail()); 
        assertEquals("John", result.getFirstName()); 
        assertEquals("Doe", result.getLastName()); 
        assertEquals(1, result.getRoles().size());
        assertEquals("ROLE_ADMIN", result.getRoles().iterator().next().getAuthority()); 
        
     
        verify(userRepository, times(1)).save(any(User.class));
    }


    @Test
    public void testDelete() {
      
        Long userId = 1L;
        doNothing().when(userRepository).deleteById(userId);
        userService.delete(userId);
        verify(userRepository, times(1)).deleteById(userId);
    }
}
