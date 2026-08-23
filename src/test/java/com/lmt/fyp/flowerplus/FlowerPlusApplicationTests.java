package com.lmt.fyp.flowerplus;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Full-context smoke test. Disabled because @SpringBootTest boots the whole
 * application, which needs Postgres AND Redis reachable — so it fails on any
 * checkout where the docker-compose stack isn't running (e.g. a fresh clone
 * running `mvn test`). Re-enable once there is a test profile that stands up
 * those dependencies (Testcontainers) or points at embedded substitutes.
 */
@Disabled("Requires Postgres + Redis; enable with a Testcontainers profile")
@SpringBootTest
class FlowerPlusApplicationTests {

    @Test
    void contextLoads() {
    }

}
