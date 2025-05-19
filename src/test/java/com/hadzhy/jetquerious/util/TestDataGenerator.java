package com.hadzhy.jetquerious.util;

import net.datafaker.Faker;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class TestDataGenerator {

    private static final Faker faker = new Faker();

    public static UserForm userForm(boolean isActive) {
        LocalDateTime createdAt = generateRandomTimeFrom(Instant.now().minus(3650, ChronoUnit.DAYS));
        LocalDateTime lastUpdatedAt = createdAt.plusMinutes(1);

        return new UserForm(UUID.randomUUID(),
                faker.internet().emailAddress(),
                faker.name().name(),
                faker.internet().password(),
                isActive,
                createdAt,
                lastUpdatedAt);
    }

    private static LocalDateTime generateRandomTimeFrom(Instant yearsAgo) {
        Instant now = Instant.now();
        Instant randomInstant = faker.timeAndDate().between(yearsAgo, now);
        return LocalDateTime.ofInstant(randomInstant, ZoneId.systemDefault());
    }


}
