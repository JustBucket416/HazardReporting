package com.example.hazardreporting.domain.exception

import com.example.hazardreporting.domain.model.IncidentDraft

/**
 * Thrown when a photo is larger than [IncidentDraft.MAX_PHOTO_BYTES], so the server would refuse its upload
 *
 * @author Roman Pliskin on 29.10.2026
 */
class PhotoTooLargeException : Exception("The photo is larger than ${IncidentDraft.MAX_PHOTO_BYTES} bytes")
