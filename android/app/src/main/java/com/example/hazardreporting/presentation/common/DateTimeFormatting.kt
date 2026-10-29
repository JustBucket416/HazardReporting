package com.example.hazardreporting.presentation.common

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * @author Roman Pliskin on 29.10.2026
 */

/**
 * Formats [epochMillis] as a local date in the device's locale and time zone, e.g. "29 Oct 2026"
 */
fun formatLocalDate(epochMillis: Long): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMillis))

/**
 * Formats [epochMillis] as an absolute local date and time in the device's locale and time zone,
 * e.g. "29 Oct 2026, 14:05"
 */
fun formatLocalDateTime(epochMillis: Long): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMillis))
