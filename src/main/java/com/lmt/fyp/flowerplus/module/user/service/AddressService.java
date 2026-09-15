package com.lmt.fyp.flowerplus.module.user.service;

import com.lmt.fyp.flowerplus.module.user.entity.Address;
import com.lmt.fyp.flowerplus.module.user.entity.User;
import com.lmt.fyp.flowerplus.module.user.exception.AddressLimitReachedException;
import com.lmt.fyp.flowerplus.module.user.exception.AddressNotFoundException;
import com.lmt.fyp.flowerplus.module.user.repository.AddressRepository;
import com.lmt.fyp.flowerplus.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Rules and rationale: docs/modules/address.md */
@Service
@RequiredArgsConstructor
public class AddressService {

    // Also the list's page size. The list is always its first page, so the two
    // must stay equal or addresses past that page could never be shown.
    private static final int MAX_ADDRESSES_PER_USER = 20;

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<Address> listFor(User owner) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(
                owner.getId(), PageRequest.of(0, MAX_ADDRESSES_PER_USER));
    }

    @Transactional(readOnly = true)
    public Address getOwned(User owner, UUID addressId) {
        return addressRepository.findByIdAndUserId(addressId, owner.getId())
                .orElseThrow(() -> new AddressNotFoundException(
                        "Address not found with id: " + addressId));
    }

    @Transactional
    public Address addAddress(User owner, String receiverName, String phone,
                              String address, boolean makeDefault) {
        UUID ownerId = owner.getId();
        if (addressRepository.countByUserId(ownerId) >= MAX_ADDRESSES_PER_USER) {
            throw new AddressLimitReachedException(
                    "A customer can save at most " + MAX_ADDRESSES_PER_USER + " addresses.");
        }

        boolean isDefault = makeDefault || !addressRepository.existsByUserId(ownerId);

        if (isDefault) {
            addressRepository.clearDefaultFor(ownerId);
        }

        return addressRepository.save(Address.builder()
                // Re-fetched: clearDefaultFor() detaches the argument. Issues no SELECT.
                .user(userRepository.getReferenceById(ownerId))
                .receiverName(receiverName)
                .phone(phone)
                .address(address)
                .isDefault(isDefault)
                .build());
    }

    @Transactional
    public Address updateAddress(User owner, UUID addressId, String receiverName,
                                 String phone, String address, boolean makeDefault) {
        Address existing = getOwned(owner, addressId);

        if (makeDefault && !existing.isDefault()) {
            addressRepository.clearDefaultFor(owner.getId());
            // Reloaded: the bulk update cleared the persistence context.
            existing = getOwned(owner, addressId);
            existing.markDefault();
        }

        existing.edit(receiverName, phone, address);
        return existing;
    }

    @Transactional
    public Address setDefault(User owner, UUID addressId) {
        getOwned(owner, addressId);

        // Two statements, in this order. The V5 partial unique index is checked
        // per row and cannot be deferred, so no moment may have two rows true.
        addressRepository.clearDefaultFor(owner.getId());

        Address target = getOwned(owner, addressId);
        target.markDefault();
        return target;
    }

    @Transactional
    public void deleteAddress(User owner, UUID addressId) {
        Address target = getOwned(owner, addressId);
        boolean wasDefault = target.isDefault();

        addressRepository.delete(target);

        if (wasDefault) {
            // Hibernate orders updates ahead of deletes, so the promotion below
            // would hit the unique index while the old default still exists.
            addressRepository.flush();
            addressRepository.findFirstByUserIdOrderByCreatedAtAsc(owner.getId())
                    .ifPresent(Address::markDefault);
        }
    }
}
