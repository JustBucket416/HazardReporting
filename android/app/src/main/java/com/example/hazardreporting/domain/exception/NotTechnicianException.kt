package com.example.hazardreporting.domain.exception

import com.example.hazardreporting.domain.model.UserRole

/**
 * Thrown when someone whose role isn't [UserRole.Technician] signs in: the app is for technicians only
 *
 * @param role the role of the user who tried to sign in
 *
 * @author Roman Pliskin on 29.10.2026
 */
class NotTechnicianException(role: UserRole) : Exception("Only technicians can use the app, not $role")
