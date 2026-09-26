package com.lmt.fyp.flowerplus.module.material;

import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.module.material.entity.MaterialType;
import com.lmt.fyp.flowerplus.module.material.entity.UnitOfMeasure;
import com.lmt.fyp.flowerplus.module.material.repository.MaterialRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Covers database constraints and competing requests for the same normalized name. */
class MaterialIntegrityTest extends MaterialIntegrationSupport {

    @MockitoSpyBean
    private MaterialRepository repositorySpy;

    @Test
    @DisplayName("T-MAT-05: Database constraints reject invalid material values")
    void databaseConstraintsRejectInvalidMaterialValues() {
        assertThrows(DataIntegrityViolationException.class, () -> insert("Invalid price", "FLOWER", "STEM", 0, "ACTIVE"));
        assertThrows(DataIntegrityViolationException.class, () -> insert("Invalid type", "OTHER", "STEM", 1, "ACTIVE"));
        assertThrows(DataIntegrityViolationException.class, () -> insert("Invalid unit", "FLOWER", "BOX", 1, "ACTIVE"));
        assertThrows(DataIntegrityViolationException.class, () -> insert("Invalid status", "FLOWER", "STEM", 1, "REMOVED"));

        insert("Unique material", "FLOWER", "STEM", 1, "ACTIVE");
        assertThrows(DataIntegrityViolationException.class,
                () -> insert("unique material", "FLOWER", "STEM", 1, "ACTIVE"));
    }

    @Test
    @DisplayName("T-MAT-05/08: Concurrent creates cannot persist the same normalized name")
    void concurrentCreatesReturnOneConflict() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        CyclicBarrier barrier = new CyclicBarrier(2);
        doAnswer(invocation -> {
            boolean result = Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM material WHERE LOWER(name) = LOWER(?))",
                    Boolean.class,
                    invocation.getArgument(0, String.class)));
            barrier.await(5, TimeUnit.SECONDS);
            return result;
        }).when(repositorySpy).existsByNormalizedNameIgnoreCase(anyString());

        List<MvcResult> results = runConcurrently(
                () -> mockMvc.perform(post("/api/manage/materials")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(request(
                                        "Race Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000))))
                        .andReturn(),
                () -> mockMvc.perform(post("/api/manage/materials")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(request(
                                        "Race Rose", MaterialType.DECORATION, UnitOfMeasure.PIECE, 16000))))
                        .andReturn());

        assertOneSuccessAndOneNameConflict(results, 201);
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM material WHERE LOWER(TRIM(name)) = LOWER(TRIM(?))",
                Integer.class,
                "Race Rose"));
    }

    @Test
    @DisplayName("T-MAT-05/08: Concurrent edits cannot persist the same normalized name")
    void concurrentEditsReturnOneConflict() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID firstId = createMaterialViaApi(
                token, "First Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);
        UUID secondId = createMaterialViaApi(
                token, "Second Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 16000);

        CyclicBarrier barrier = new CyclicBarrier(2);
        doAnswer(invocation -> {
            boolean result = Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                            SELECT EXISTS (
                                SELECT 1 FROM material
                                WHERE LOWER(name) = LOWER(?) AND id != ?
                            )
                            """,
                    Boolean.class,
                    invocation.getArgument(0, String.class),
                    invocation.getArgument(1, UUID.class)));
            barrier.await(5, TimeUnit.SECONDS);
            return result;
        }).when(repositorySpy).existsByNormalizedNameIgnoreCaseAndIdNot(anyString(), any(UUID.class));

        List<MvcResult> results = runConcurrently(
                () -> mockMvc.perform(put("/api/manage/materials/" + firstId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(updateRequest(
                                        "Shared Name", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000, 0))))
                        .andReturn(),
                () -> mockMvc.perform(put("/api/manage/materials/" + secondId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(updateRequest(
                                        "Shared Name", MaterialType.FLOWER, UnitOfMeasure.STEM, 16000, 0))))
                        .andReturn());

        assertOneSuccessAndOneNameConflict(results, 200);
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM material WHERE LOWER(TRIM(name)) = LOWER(TRIM(?))",
                Integer.class,
                "Shared Name"));
    }

    private void insert(String name, String type, String unit, int price, String status) {
        jdbcTemplate.update("""
                INSERT INTO material (id, name, type, unit_of_measure, selling_price, status, created_at, updated_at)
                VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, name, type, unit, price, status);
    }

    @SafeVarargs
    private List<MvcResult> runConcurrently(Callable<MvcResult>... requests) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(requests.length);
        try {
            List<Future<MvcResult>> futures = Arrays.stream(requests).map(executor::submit).toList();
            return futures.stream().map(future -> {
                try {
                    return future.get(15, TimeUnit.SECONDS);
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            }).toList();
        } finally {
            executor.shutdownNow();
        }
    }

    private void assertOneSuccessAndOneNameConflict(List<MvcResult> results, int successStatus) throws Exception {
        assertEquals(List.of(successStatus, 409), results.stream()
                .map(result -> result.getResponse().getStatus())
                .sorted()
                .toList());
        MvcResult conflict = results.stream()
                .filter(result -> result.getResponse().getStatus() == 409)
                .findFirst()
                .orElseThrow();
        assertEquals("MATERIAL_NAME_EXISTS",
                objectMapper.readTree(conflict.getResponse().getContentAsString()).get("errorCode").asText());
    }
}
