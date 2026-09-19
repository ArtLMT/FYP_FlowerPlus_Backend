package com.lmt.fyp.flowerplus.module.user.web;

import com.lmt.fyp.flowerplus.module.user.service.AddressService;
import com.lmt.fyp.flowerplus.module.user.web.dto.AddressResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin-only address lookups (BR-ADDR-13: an Admin may see any customer's saved
 * address; Staff may not). Behind the {@code /api/admin/**} URL rule plus
 * @PreAuthorize as a second layer. The lookup is unscoped — {@code getById} —
 * because ownership is not the guard here; the role is. A missing address is a
 * 404, so the endpoint never confirms which ids exist.
 */
@RestController
@RequestMapping("/api/admin/addresses")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAddressController {

    private final AddressService addressService;

    @GetMapping("/{id}")
    public AddressResponse getById(@PathVariable UUID id) {
        return AddressResponse.from(addressService.getById(id));
    }
}
