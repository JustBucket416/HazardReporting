# Technicians withdraw incidents; nothing is hard-deleted

The requirement says technicians can "remove" an incident. We implement that as setting `status: Withdrawn`, not as a `DELETE`. An operator may already be acting on the incident, and in maintenance a record that disappears is worse than one marked as taken back. `DELETE` is disabled on the server for everyone. Withdrawn incidents are hidden from the technician's list but stay visible on the dashboard.
