package com.example.hazardreporting.domain.exception

import com.example.hazardreporting.domain.model.IncidentDraft

/**
 * Thrown when a draft can't be saved because some of its fields are invalid
 *
 * @property fields the invalid fields; never empty
 *
 * @author Roman Pliskin on 29.10.2026
 */
class ValidationException(val fields: Set<IncidentDraft.Field>) : Exception("Invalid fields: $fields")
