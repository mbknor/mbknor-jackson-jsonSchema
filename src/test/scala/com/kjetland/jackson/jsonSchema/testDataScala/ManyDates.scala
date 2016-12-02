package com.kjetland.jackson.jsonSchema.testDataScala

import java.time.{LocalDate, LocalDateTime, OffsetDateTime}

case class ManyDates
(
  javaLocalDateTime:LocalDateTime,
  javaOffsetDateTime:OffsetDateTime,
  javaLocalDate:LocalDate,
  jodaLocalDate:org.joda.time.LocalDate
)
