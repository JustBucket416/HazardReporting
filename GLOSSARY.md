# Field Incident Reporting

Field technicians of a facility maintenance organisation report physical problems they find on site; operators triage those reports and see them through to resolution.

## People

**Technician**:
A field worker who reports incidents from a mobile device and can see only the incidents they reported.
_Avoid_: Reporter (except as the role a technician plays on one incident), field user, worker

**Operator**:
A back-office person who triages, manages and resolves incidents reported by any technician.
_Avoid_: Admin, dispatcher, reviewer

**Reporter**:
The technician who reported a given incident.
_Avoid_: Author, owner, creator

## Incidents

**Incident**:
A physical problem on a site (broken equipment, a road hazard, a leak) as reported by a technician and tracked until it is closed.
_Avoid_: Hazard, ticket, issue, report (as a noun for the record)

**Report** (verb):
To submit a new incident.
_Avoid_: Create, file, raise

**Incident Summary**:
The short form of an incident a technician sees in their list: title, category, severity, status and when it was reported.
_Avoid_: Preview, list item

**Incident Details**:
The rest of an incident beyond its summary: description, site, location, photos and priority.
_Avoid_: Full incident, extended info

**Photo**:
A picture of the problem attached to an incident; every incident has one to three.
_Avoid_: Image, attachment

**Category**:
The kind of problem an incident describes (Equipment, Electrical, Plumbing, HVAC, Structural, Road/Grounds Hazard, Other).
_Avoid_: Type, kind

**Severity**:
How dangerous or damaging the problem is, as judged by the technician (Low, Medium, High, Critical).
_Avoid_: Urgency, priority, level

**Priority**:
How soon the organisation will act on an incident, as decided by an operator; independent of severity.
_Avoid_: Severity, urgency

## Location

**Site**:
A named facility or area the organisation maintains (e.g. "North Plant"); every incident belongs to exactly one.
_Avoid_: Facility, location, building

**Location Description**:
Free text pinpointing the problem within a site (e.g. "Pump room B2").
_Avoid_: Address, place

**Position**:
The device's geographic coordinates and accuracy captured when the incident was reported; may be absent.
_Avoid_: GPS, location, coordinates (as the term name)

## Lifecycle

**Status**:
Where an incident stands in its lifecycle: Open, Acknowledged, In Progress, Resolved, Rejected or Withdrawn.
_Avoid_: State, stage

**Open**:
Reported and not yet looked at by an operator; the only status in which the reporter may edit or withdraw the incident.
_Avoid_: New, submitted, pending

**Acknowledged**:
An operator has accepted the incident as actionable but work has not started.
_Avoid_: Triaged, accepted, confirmed

**In Progress**:
Work to fix the problem is under way.
_Avoid_: Active, assigned

**Resolved**:
The problem has been fixed; final.
_Avoid_: Closed, done, fixed

**Rejected**:
An operator decided the incident needs no action (not actionable, duplicate, invalid); final.
_Avoid_: Cancelled, dismissed, closed

**Withdrawn**:
The reporter took the incident back while it was still Open; final, and kept for the record rather than erased.
_Avoid_: Deleted, removed, cancelled

**Triage**:
An operator's work of reviewing an incident and setting its status, priority, assignee and notes.
_Avoid_: Review, processing

**Assignee**:
The crew or team an operator has put in charge of an incident; visible to operators only.
_Avoid_: Owner, handler, worker

**Note**:
An operator's internal, append-only remark on an incident; never shown to technicians.
_Avoid_: Comment, message

## On the technician's device

**Pending Change**:
A report, edit or withdrawal the technician has made on their device that the server has not yet accepted.
_Avoid_: Draft, unsynced item, queued item

**Status Change Notice**:
The message a technician sees when a pending change was refused because an operator had already moved the incident out of Open.
_Avoid_: Conflict, error, sync failure
