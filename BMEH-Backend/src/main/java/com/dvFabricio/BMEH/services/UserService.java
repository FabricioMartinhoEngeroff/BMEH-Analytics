package com.dvFabricio.BMEH.services;

import com.dvFabricio.BMEH.domain.DTOs.UserDTO;
import com.dvFabricio.BMEH.domain.DTOs.UserRequestDTO;
import com.dvFabricio.BMEH.domain.user.User;
import com.dvFabricio.BMEH.infra.exception.database.MissingRequiredFieldException;
import com.dvFabricio.BMEH.infra.exception.resource.DuplicateResourceException;
import com.dvFabricio.BMEH.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.BMEH.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserDTO> findAllUsers() {
        return userRepository.findAll().stream().map(UserDTO::new).toList();
    }

    public UserDTO findUserById(UUID userId) {
        return userRepository.findById(userId)
                .map(UserDTO::new)
                .orElseThrow(() -> new ResourceNotFoundExceptions("User not found with id: " + userId));
    }

    @Transactional
    public UserDTO createUser(UserRequestDTO userRequestDTO) {
        validateRequiredFields(userRequestDTO);

        if (userRepository.existsByEmail(userRequestDTO.email())) {
            throw new DuplicateResourceException("email", "A user with this email already exists.");
        }

        if (userRepository.existsByCpf(userRequestDTO.cpf())) {
            throw new DuplicateResourceException("cpf", "A user with this CPF already exists.");
        }

        String encodedPassword = passwordEncoder.encode(userRequestDTO.password());
        User user = new User(
                userRequestDTO.login(),
                userRequestDTO.email(),
                encodedPassword,
                userRequestDTO.cpf(),
                userRequestDTO.telefone(),
                userRequestDTO.endereco()
        );

        user = userRepository.save(user);
        return new UserDTO(user);
    }

    @Transactional
    public UserDTO updateUser(UUID userId, UserRequestDTO userRequestDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundExceptions("User not found with id: " + userId));

        logger.debug("Before update: {}", user);
        updateUserFields(user, userRequestDTO);
        user = userRepository.save(user);
        logger.debug("After update: {}", user);

        return new UserDTO(user);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundExceptions("User not found with id: " + userId));
        userRepository.delete(user);
    }

    private void updateUserFields(User user, UserRequestDTO userRequestDTO) {
        if (!isBlank(userRequestDTO.login())) {
            user.setLogin(userRequestDTO.login());
        }
        if (!isBlank(userRequestDTO.email())) {
            user.setEmail(userRequestDTO.email());
        }
        if (!isBlank(userRequestDTO.password())) {
            user.setPassword(passwordEncoder.encode(userRequestDTO.password()));
        }
        if (!isBlank(userRequestDTO.cpf())) {
            user.setCpf(userRequestDTO.cpf());
        }
        if (!isBlank(userRequestDTO.telefone())) {
            user.setTelefone(userRequestDTO.telefone());
        }
        if (userRequestDTO.endereco() != null) {
            user.setEndereco(userRequestDTO.endereco());
        }
    }

    private void validateRequiredFields(UserRequestDTO userRequestDTO) {
        if (isBlank(userRequestDTO.login())) {
            throw new MissingRequiredFieldException("login", "Login cannot be empty");
        }
        if (isBlank(userRequestDTO.email())) {
            throw new MissingRequiredFieldException("email", "Email cannot be empty");
        }
        if (isBlank(userRequestDTO.password())) {
            throw new MissingRequiredFieldException("password", "Password cannot be empty");
        }
        if (isBlank(userRequestDTO.cpf())) {
            throw new MissingRequiredFieldException("cpf", "CPF cannot be empty");
        }
        if (isBlank(userRequestDTO.telefone())) {
            throw new MissingRequiredFieldException("telefone", "Phone number cannot be empty");
        }
        if (userRequestDTO.endereco() == null) {
            throw new MissingRequiredFieldException("endereco", "Address cannot be empty");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}



