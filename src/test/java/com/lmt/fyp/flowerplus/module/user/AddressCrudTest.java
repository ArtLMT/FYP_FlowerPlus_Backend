package com.lmt.fyp.flowerplus.module.user;

import com.lmt.fyp.flowerplus.module.user.entity.Address;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** One test per rule in AddressService. See docs/modules/address.md. */
class AddressCrudTest extends AddressIntegrationSupport {

    private static final String OWNER = "owner@example.com";
    private static final String OTHER = "other@example.com";

    @Test
    @DisplayName("the first address a user creates becomes their default unasked")
    void firstAddressIsDefault() throws Exception {
        String token = tokenFor(OWNER);

        mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("Alice", false))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.isDefault").value(true))
                .andExpect(jsonPath("$.receiverName").value("Alice"));
    }

    @Test
    @DisplayName("a later address is not default unless asked for")
    void secondAddressIsNotDefault() throws Exception {
        String token = tokenFor(OWNER);
        UUID first = createAddress(token, "Alice", false);

        mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("Bob", false))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isDefault").value(false));

        assertThat(addressRepository.findById(first))
                .get().extracting(Address::isDefault).isEqualTo(true);
    }

    @Test
    @DisplayName("creating an address as default clears the previous one")
    void creatingAsDefaultClearsThePrevious() throws Exception {
        String token = tokenFor(OWNER);
        UUID first = createAddress(token, "Alice", false);

        UUID second = createAddress(token, "Bob", true);

        assertThat(addressRepository.findById(first))
                .get().extracting(Address::isDefault).isEqualTo(false);
        assertThat(addressRepository.findById(second))
                .get().extracting(Address::isDefault).isEqualTo(true);
    }

    @Test
    @DisplayName("setDefault promotes one address and demotes the other")
    void setDefaultSwitchesTheDefault() throws Exception {
        String token = tokenFor(OWNER);
        UUID first = createAddress(token, "Alice", false);
        UUID second = createAddress(token, "Bob", false);

        mockMvc.perform(put("/api/addresses/" + second + "/default")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault").value(true));

        assertThat(addressRepository.findById(first))
                .get().extracting(Address::isDefault).isEqualTo(false);
    }

    @Test
    @DisplayName("editing the default address does not silently un-default it")
    void updateCannotClearTheDefault() throws Exception {
        String token = tokenFor(OWNER);
        UUID only = createAddress(token, "Alice", false);

        mockMvc.perform(put("/api/addresses/" + only)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("Alice Renamed", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiverName").value("Alice Renamed"))
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    @DisplayName("deleting a non-default address leaves the default alone")
    void deleteNonDefault() throws Exception {
        String token = tokenFor(OWNER);
        UUID first = createAddress(token, "Alice", false);
        UUID second = createAddress(token, "Bob", false);

        mockMvc.perform(delete("/api/addresses/" + second)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(addressRepository.findById(second)).isEmpty();
        assertThat(addressRepository.findById(first))
                .get().extracting(Address::isDefault).isEqualTo(true);
    }

    @Test
    @DisplayName("deleting the default promotes the oldest remaining address")
    void deletingTheDefaultPromotesTheOldest() throws Exception {
        String token = tokenFor(OWNER);
        UUID first = createAddress(token, "Alice", false);   // becomes the default
        UUID second = createAddress(token, "Bob", false);
        UUID third = createAddress(token, "Carol", false);

        mockMvc.perform(delete("/api/addresses/" + first)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(addressRepository.findById(second))
                .get().extracting(Address::isDefault).isEqualTo(true);
        assertThat(addressRepository.findById(third))
                .get().extracting(Address::isDefault).isEqualTo(false);
    }

    @Test
    @DisplayName("reading somebody else's address returns 404, not 403")
    void crossUserReadIsNotFound() throws Exception {
        String otherToken = tokenFor(OTHER);
        UUID theirs = createAddress(otherToken, "Theirs", false);
        String ownerToken = tokenFor(OWNER);

        mockMvc.perform(get("/api/addresses/" + theirs)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ADDRESS_NOT_FOUND"))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("deleting somebody else's address returns 404 and leaves it intact")
    void crossUserDeleteIsNotFound() throws Exception {
        String otherToken = tokenFor(OTHER);
        UUID theirs = createAddress(otherToken, "Theirs", false);
        String ownerToken = tokenFor(OWNER);

        mockMvc.perform(delete("/api/addresses/" + theirs)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound());

        assertThat(addressRepository.findById(theirs)).isPresent();
    }

    @Test
    @DisplayName("the list shows only the caller's addresses, default first")
    void listIsScopedToTheCaller() throws Exception {
        String otherToken = tokenFor(OTHER);
        createAddress(otherToken, "Theirs", false);

        String ownerToken = tokenFor(OWNER);
        createAddress(ownerToken, "Alice", false);
        UUID promoted = createAddress(ownerToken, "Bob", true);

        mockMvc.perform(get("/api/addresses")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(promoted.toString()))
                .andExpect(jsonPath("$.content[0].isDefault").value(true));
    }

    @Test
    @DisplayName("page, size and sort sent with the list request are ignored")
    void listIgnoresPagingParameters() throws Exception {
        String token = tokenFor(OWNER);
        createAddress(token, "Alice", false);
        UUID promoted = createAddress(token, "Bob", true);

        mockMvc.perform(get("/api/addresses")
                        .param("page", "3")
                        .param("size", "1")
                        .param("sort", "nope")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(promoted.toString()));
    }

    @Test
    @DisplayName("a customer can save at most 20 addresses")
    void addressLimitIsEnforced() throws Exception {
        String token = tokenFor(OWNER);
        for (int i = 0; i < 20; i++) {
            createAddress(token, "Receiver " + i, false);
        }

        mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("One too many", false))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ADDRESS_LIMIT_REACHED"));

        assertThat(addressRepository.count()).isEqualTo(20);
    }

    @Test
    @DisplayName("a blank receiver name is rejected as a 400")
    void blankReceiverNameIsRejected() throws Exception {
        String token = tokenFor(OWNER);

        mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("   ", false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.fields[?(@.field == 'receiverName')]").isNotEmpty());

        assertThat(addressRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("an unauthenticated request is rejected")
    void unauthenticatedIsRejected() throws Exception {
        mockMvc.perform(get("/api/addresses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a malformed address id is a 404, not a 500")
    void malformedIdIsNotFound() throws Exception {
        String token = tokenFor(OWNER);

        mockMvc.perform(get("/api/addresses/not-a-uuid")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("exactly one address per user is the default at any time")
    void exactlyOneDefaultPerUser() throws Exception {
        String token = tokenFor(OWNER);
        createAddress(token, "Alice", false);
        createAddress(token, "Bob", true);
        createAddress(token, "Carol", true);

        List<Address> all = addressRepository.findAll();
        assertThat(all).hasSize(3);
        assertThat(all.stream().filter(Address::isDefault).count()).isEqualTo(1L);
    }
}
