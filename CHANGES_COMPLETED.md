# Functional completion pass

This build exposes the backend features that were previously missing from the browser UI and completes assessment management.

## Added to the UI
- Student creation and limited student editing supported by the existing backend.
- Admissions creation, review, approve and reject actions.
- Academic years, terms, classes, class streams, subjects and student enrolments.
- Subject creation and subject removal for users with `ACADEMICS_MANAGE`.
- Attendance session creation and student attendance recording.
- Assessment/test creation for users with `ASSESSMENT_WRITE`.
- Assessment score listing, creation, editing and removal.
- Assessment removal, including its recorded scores.
- Grading scale creation and grade lookup.
- Result PDF preview/download/print controls for authorised Admin/Super Admin users.
- User creation, role assignment, activation/deactivation.
- Role-permission editor for users with `PERMISSION_ASSIGN`.
- Signed-in user password change page.

## Backend additions
- Subject delete endpoint.
- Assessment delete endpoint.
- Assessment score list/update/delete endpoints.
- Assessment score response DTO.
- Score validation and update support.

## Permissions
Assessment management continues to use the existing backend permissions introduced by the assessment migration:
- `ASSESSMENT_VIEW`
- `ASSESSMENT_WRITE`

Both `SUPER_ADMIN` and `ADMIN` already receive `ASSESSMENT_WRITE` from migration V9.
