package com.lcaohoanq.Spring_Snake_Game.controller;

import com.lcaohoanq.Spring_Snake_Game.dto.AbstractResponse;
import com.lcaohoanq.Spring_Snake_Game.dto.request.UserRegisterRequest;
import com.lcaohoanq.Spring_Snake_Game.dto.request.UserUpdatePasswordRequest;
import com.lcaohoanq.Spring_Snake_Game.dto.response.JwtResponse;
import com.lcaohoanq.Spring_Snake_Game.dto.request.UserLoginRequest;
import com.lcaohoanq.Spring_Snake_Game.dto.request.UserRegisterRequestFull;
import com.lcaohoanq.Spring_Snake_Game.dto.response.UserResponse;
import com.lcaohoanq.Spring_Snake_Game.exception.MethodArgumentNotValidException;
import com.lcaohoanq.Spring_Snake_Game.exception.UserNotFoundException;
import com.lcaohoanq.Spring_Snake_Game.entity.User;
import com.lcaohoanq.Spring_Snake_Game.repository.UserRepository;
import com.lcaohoanq.Spring_Snake_Game.util.PBKDF2;
import jakarta.validation.Valid;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class UserController {

    @Autowired
    private UserRepository userRepository;

    private PBKDF2 pbkdf2;

    @GetMapping("/users")
    List<User> all() {
        return userRepository.findAll();
    }

    @GetMapping("/users/{id}")
    User getById(@PathVariable Long id) {

        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
    }

    @PostMapping("/users/register")
    @Async
    public CompletableFuture<ResponseEntity<UserResponse>> createNew(
        @Valid @RequestBody UserRegisterRequest newUser, BindingResult bindingResult) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (bindingResult.hasErrors()) {
                    log.error("Validation failed for user at: {}", newUser.getCreated_at());
                    throw new MethodArgumentNotValidException(bindingResult);
                }

                if ((newUser.getEmail() == null || newUser.getEmail().isEmpty()) &&
                    (newUser.getPhone() == null || newUser.getPhone().isEmpty())) {
                    throw new IllegalArgumentException("Either email or phone must be provided.");
                } else if (newUser.getEmail() != null) {
                    boolean emailExists = userRepository.findAll().stream()
                        .anyMatch(user -> newUser.getEmail().equals(user.getEmail()));
                    if (emailExists) {
                        log.error("Email already registered: {}", newUser.getEmail());
                        return new ResponseEntity<>(new UserResponse("Email already registered"),
                            HttpStatus.BAD_REQUEST);
                    }
                } else {
                    boolean phoneExists = userRepository.findAll().stream()
                        .anyMatch(user -> newUser.getPhone().equals(user.getPhone()));

                    if (phoneExists) {
                        log.error("Phone number already registered: {}", newUser.getPhone());
                        return new ResponseEntity<>(
                            new UserResponse("Phone number already registered"),
                            HttpStatus.BAD_REQUEST);
                    }
                }

                // Hash the password before saving the user
                newUser.setPassword(new PBKDF2().hash(newUser.getPassword().toCharArray()));
                log.info("Creating new user at: {}", newUser.getCreated_at());

                // Save the user to the repository
                User user = new User();
                user.setId(newUser.getId());
                user.setEmail(newUser.getEmail());
                user.setPhone(newUser.getPhone());
                user.setFirstName(newUser.getFirstName());
                user.setLastName(newUser.getLastName());
                user.setPassword(newUser.getPassword());
                user.setAddress(newUser.getAddress());
                user.setBirthday(newUser.getBirthday());
                user.setGender(newUser.getGender());
                user.setRole(newUser.getRole());
                user.setStatus(newUser.getStatus());
                user.setCreated_at(newUser.getCreated_at());
                user.setUpdated_at(newUser.getUpdated_at());
                user.setAvatar_url(newUser.getAvatar_url());
//                user.setSubscription(newUser.getSubscription());

                System.out.println("Data: " + user);

                userRepository.save(user);

                return new ResponseEntity<>(new UserResponse("Register successfully"),
                    HttpStatus.OK);
            }  catch(Exception e){
                log.error("An error occurred while creating a new user: {}", e.getMessage());
                return new ResponseEntity<>(new UserResponse(e.getMessage()),
                    HttpStatus.BAD_REQUEST);
            }
        });
    }

    @PostMapping("/users/login")
    ResponseEntity<AbstractResponse> login(@Valid @RequestBody UserLoginRequest user,
        BindingResult bindingResult) {
        User userFound = null;
        PBKDF2 pbkdf2 = new PBKDF2();
        if (bindingResult.hasErrors()) {
            log.error("Validation failed for user when login");
            throw new MethodArgumentNotValidException(bindingResult);
        }

        // Check email or phone
        if (user.getEmail_phone().contains("@")) {
            userFound = userRepository.findAll().stream()
                .filter(u -> u.getEmail().equals(user.getEmail_phone()))
                .findFirst()
                .orElse(null);

            if (userFound == null) {
                log.error("Email not found: {}", user.getEmail_phone());
                return new ResponseEntity<>(new UserResponse("Email not found"),
                    HttpStatus.BAD_REQUEST);
            }
        } else {
            userFound = userRepository.findAll().stream()
                .filter(u -> u.getPhone().equals(user.getEmail_phone()))
                .findFirst()
                .orElse(null);

            if (userFound == null) {
                log.error("Phone number not found: {}", user.getEmail_phone());
                return new ResponseEntity<>(new UserResponse("Phone number not found"),
                    HttpStatus.BAD_REQUEST);
            }
        }

        if (!pbkdf2.authenticate(user.getPassword().toCharArray(), userFound.getPassword())) {
            log.error("Password not match: {}", user.getEmail_phone());
            return new ResponseEntity<>(new UserResponse("Password not match"),
                HttpStatus.BAD_REQUEST);
        }

        log.info("Login successfully: {}", user.getEmail_phone());

        // Generate tokens
        JwtResponse jwtResponse = new JwtResponse("accessToken", "refreshToken");

        return new ResponseEntity<>(jwtResponse, HttpStatus.OK);
    }


    @DeleteMapping("/users/{id}")
    void delete(@PathVariable Long id) {
        userRepository.deleteById(id);
    }

    @PutMapping("/users/{id}")
    User updateOrCreate(@RequestBody User newUser, @PathVariable Long id) {

        return userRepository.findById(id)
            .map(item -> {
                item.setFirstName(newUser.getFirstName());
                return userRepository.save(item);
            })
            .orElseGet(() -> {
                newUser.setId(id);
                return userRepository.save(newUser);
            });
    }

    //udpate the password of user
    @PatchMapping("/users/updatePassword")
    public ResponseEntity<UserResponse> updatePassword(@RequestBody UserUpdatePasswordRequest user) {
        User userFound = null;
        if (user.getEmail().contains("@")) {
            userFound = userRepository.findAll().stream()
                .filter(u -> u.getEmail().equals(user.getEmail()))
                .findFirst()
                .orElse(null);

            if (userFound == null) {
                log.error("Email not found: {}", user.getEmail());
                return new ResponseEntity<>(new UserResponse("Email not found"),
                    HttpStatus.BAD_REQUEST);
            }
        } else {
            userFound = userRepository.findAll().stream()
                .filter(u -> u.getPhone().equals(user.getEmail()))
                .findFirst()
                .orElse(null);

            if (userFound == null) {
                log.error("Phone number not found: {}", user.getEmail());
                return new ResponseEntity<>(new UserResponse("Phone number not found"),
                    HttpStatus.BAD_REQUEST);
            }
        }

        // Hash the password before saving the user
        user.setNewPassword(new PBKDF2().hash(user.getNewPassword().toCharArray()));
        log.info("Updating password for user: {}", user.getEmail());

        // Save the user to the repository
        userFound.setPassword(user.getNewPassword());
        userRepository.save(userFound);

        return new ResponseEntity<>(new UserResponse("Update password successfully"),
            HttpStatus.OK);
    }

}
