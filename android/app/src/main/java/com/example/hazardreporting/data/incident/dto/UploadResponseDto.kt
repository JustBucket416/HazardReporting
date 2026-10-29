package com.example.hazardreporting.data.incident.dto

import kotlinx.serialization.Serializable

/**
 * Response of `POST /uploads`
 *
 * @property url the stored photo's URL, relative to the server, e.g. `"/uploads/ab12.jpg"`
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Serializable
data class UploadResponseDto(
    val url: String,
)
