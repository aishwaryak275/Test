package com.teleconnect.iam.controller;

import com.teleconnect.iam.dto.request.UpdateUserRequest;
import com.teleconnect.iam.dto.response.MessageDTO;
import com.teleconnect.iam.dto.response.UserResponseDTO;
import com.teleconnect.iam.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/teleConnect/iam/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // GET /users/me 
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getMe(Principal principal) {
        return ResponseEntity.ok(userService.getOwnProfile(principal.getName()));
    }

    // GET /users/{id} - admin, network ops, CS agent
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('VIEW_SUBSCRIBER','VIEW_ALL_USERS','VIEW_NETWORK_FAULTS')")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // PUT /users/{id} - admin, subscriber(own)
    @PutMapping("/{id}") 
    public ResponseEntity<MessageDTO> updateUser(@PathVariable Long id,
                                                 @RequestBody UpdateUserRequest req,
                                                 Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("VIEW_ALL_USERS"));
        return ResponseEntity.ok(userService.updateUser(id, req, isAdmin));
    }

    // GET /users - admin
    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_ALL_USERS')")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // GET /users/search - admin,CS agent
    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('VIEW_ALL_USERS','VIEW_SUBSCRIBER')")
    public ResponseEntity<List<UserResponseDTO>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role) {
        return ResponseEntity.ok(userService.searchUsers(name, email, phone, status, role));
    }
}
