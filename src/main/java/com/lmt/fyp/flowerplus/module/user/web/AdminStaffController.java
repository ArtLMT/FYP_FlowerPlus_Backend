package com.lmt.fyp.flowerplus.module.user.web;

import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.dto.PageResponse;
import com.lmt.fyp.flowerplus.module.user.entity.User;
import com.lmt.fyp.flowerplus.module.user.entity.UserProfile;
import com.lmt.fyp.flowerplus.module.user.service.StaffAccountService;
import com.lmt.fyp.flowerplus.module.user.service.UserService;
import com.lmt.fyp.flowerplus.module.user.web.dto.CreateStaffRequest;
import com.lmt.fyp.flowerplus.module.user.web.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

/**
 * The Admin's Staff-account management surface. Behind the {@code /api/admin/**}
 * URL rule (Admin only) with @PreAuthorize as a second layer, so the role is
 * checked before any lookup — Staff and customers get 403, a non-Staff target id
 * is 404. Roles and prefixes: docs/modules/user.md; decision: ADR 0004.
 */
@RestController
@RequestMapping("/api/admin/staff")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminStaffController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final StaffAccountService staffAccountService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateStaffRequest request,
                                               UriComponentsBuilder uriBuilder) {
        User staff = staffAccountService.createStaff(request.email(), request.fullName());

        URI location = uriBuilder.path("/api/admin/users/{id}")
                .buildAndExpand(staff.getId())
                .toUri();
        return ResponseEntity.created(location).body(toResponse(staff));
    }

    @PutMapping("/{id}/deactivate")
    public UserResponse deactivate(@PathVariable UUID id) {
        return toResponse(staffAccountService.deactivate(id));
    }

    @PutMapping("/{id}/reactivate")
    public UserResponse reactivate(@PathVariable UUID id) {
        return toResponse(staffAccountService.reactivate(id));
    }

    @GetMapping
    public PageResponse<UserResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
            @RequestParam(required = false) UserAccountStatus status) {

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<User> staff = staffAccountService.listStaff(status, pageable);
        Map<UUID, UserProfile> profiles = userService.getProfiles(staff.getContent());
        return PageResponse.from(staff, user -> UserResponse.from(user, profiles.get(user.getId())));
    }

    private UserResponse toResponse(User staff) {
        return UserResponse.from(staff, userService.getProfile(staff));
    }
}
