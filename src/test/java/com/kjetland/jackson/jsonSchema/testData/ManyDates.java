package com.kjetland.jackson.jsonSchema.testData;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.extern.jackson.Jacksonized;

/**
 *
 * @author alex
 */
@AllArgsConstructor
@Jacksonized @Builder
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
@ToString @EqualsAndHashCode
public class ManyDates {
    LocalDateTime javaLocalDateTime;
    OffsetDateTime javaOffsetDateTime;
    LocalDate javaLocalDate;
    org.joda.time.LocalDate jodaLocalDate;
}
