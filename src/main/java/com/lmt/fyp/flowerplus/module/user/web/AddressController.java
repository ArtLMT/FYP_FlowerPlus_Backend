package com.lmt.fyp.flowerplus.module.user.web;

import com.lmt.fyp.flowerplus.common.dto.PageResponse;
import com.lmt.fyp.flowerplus.module.user.entity.Address;
import com.lmt.fyp.flowerplus.module.user.entity.User;
import com.lmt.fyp.flowerplus.module.user.service.AddressService;
import com.lmt.fyp.flowerplus.module.user.service.UserService;
import com.lmt.fyp.flowerplus.module.user.web.dto.AddressRequest;
import com.lmt.fyp.flowerplus.module.user.web.dto.AddressResponse;
import com.lmt.fyp.flowerplus.security.SecurityUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * No @PreAuthorize/@PostAuthorize by design — ownership is the scoped query in
 * AddressService, so somebody else's id yields 404 rather than a 403 that
 * confirms the row exists.
 */
@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;
    private final UserService userService;

    private User caller(SecurityUser principal) {
        return userService.getByEmail(principal.getUsername());
    }

    @GetMapping
    public PageResponse<AddressResponse> list(@AuthenticationPrincipal SecurityUser principal) {
        Page<Address> page = addressService.listFor(caller(principal));
        return PageResponse.from(page, AddressResponse::from);
    }

    @GetMapping("/{id}")
    public AddressResponse getById(@AuthenticationPrincipal SecurityUser principal,
                                   @PathVariable UUID id) {
        return AddressResponse.from(addressService.getOwned(caller(principal), id));
    }

    @PostMapping
    public ResponseEntity<AddressResponse> create(
            @AuthenticationPrincipal SecurityUser principal,
            @Valid @RequestBody AddressRequest request,
            UriComponentsBuilder uriBuilder) {

        Address created = addressService.addAddress(
                caller(principal),
                request.receiverName(),
                request.phone(),
                request.address(),
                request.isDefault());

        URI location = uriBuilder.path("/api/addresses/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(AddressResponse.from(created));
    }

    @PutMapping("/{id}")
    public AddressResponse update(@AuthenticationPrincipal SecurityUser principal,
                                  @PathVariable UUID id,
                                  @Valid @RequestBody AddressRequest request) {

        Address updated = addressService.updateAddress(
                caller(principal),
                id,
                request.receiverName(),
                request.phone(),
                request.address(),
                request.isDefault());
        return AddressResponse.from(updated);
    }

    @PutMapping("/{id}/default")
    public AddressResponse setDefault(@AuthenticationPrincipal SecurityUser principal,
                                      @PathVariable UUID id) {
        return AddressResponse.from(addressService.setDefault(caller(principal), id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal SecurityUser principal,
                       @PathVariable UUID id) {
        addressService.deleteAddress(caller(principal), id);
    }
}
