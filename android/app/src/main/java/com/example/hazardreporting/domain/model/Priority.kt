package com.example.hazardreporting.domain.model

/**
 * How soon the organisation will act on an incident, as decided by an operator; independent of severity
 *
 * @author Roman Pliskin on 29.10.2026
 */
enum class Priority {

    /**
     * Can wait
     */
    Low,

    /**
     * Normal priority
     */
    Medium,

    /**
     * Acted on before normal work
     */
    High,

    /**
     * Acted on immediately
     */
    Urgent,

    /**
     * A priority this app version doesn't know
     */
    Unknown,
}
