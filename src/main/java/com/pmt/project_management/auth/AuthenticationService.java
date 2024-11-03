package com.pmt.project_management.auth;

import com.pmt.project_management.exception.AlreadyExistsException;
import com.pmt.project_management.exception.UserNotFoundException;
import com.pmt.project_management.role.Role;
import com.pmt.project_management.user.UserMapper;
import com.pmt.project_management.role.RoleRepository;
import com.pmt.project_management.security.JwtService;
import com.pmt.project_management.user.User;
import com.pmt.project_management.user.UserRepository;
import com.pmt.project_management.user.UserResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper mapper;


    public Integer register(RegistrationRequest registrationRequest) {


        var user = User.builder()
                .nom(registrationRequest.getNom())
                .prenom(registrationRequest.getPrenom())
                .email(registrationRequest.getEmail())
                .password(passwordEncoder.encode(registrationRequest.getPassword()))
                .build();

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new AlreadyExistsException("L'utilisateur avec l'email " + user.getEmail() + " existe déja");
        }

        return userRepository.save(user).getId();
    }


    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        var claims = new HashMap<String, Object>();
        var user = ((User) auth.getPrincipal());
        claims.put("fullName", user.getFullName());


        var jwtToken = jwtService.generateToken(claims, (User) auth.getPrincipal());
        UserResponse userResponse = mapper.fromUser(user);


        return AuthenticationResponse.builder().token(jwtToken).user(userResponse).build();
    }

    public List<UserResponse> findAllUsers() {
        return this.userRepository.findAll().stream().map(mapper::fromUser).collect(Collectors.toList());
    }

    public UserResponse findById(Integer id) {
        return this.userRepository.findById(id).map(mapper::fromUser).orElseThrow(() -> new UserNotFoundException(String.format("No user found with the provided ID: %s", id)));
    }

    public void deleteUser(Integer id) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé avec l'Id: " + id));
        userRepository.delete(existingUser);
    }

    // Méthode pour récupérer tous les emails des utilisateurs
    public List<String> getAllUserEmails() {
        return userRepository.findAll()
                .stream()
                .map(User::getEmail)
                .collect(Collectors.toList());
    }

}

