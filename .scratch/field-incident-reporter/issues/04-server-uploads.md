# Server: photo uploads

Status: ready-for-agent
Type: task
Blocked by: 01

## What
- `POST /uploads` (multer, field `file`):
  - requires auth
  - accepts jpeg, png or webp up to 15 MB
  - stores the file under a random name
  - responds 201 with `{url}`, where `url` is relative
- Static serving of `/uploads/*` without auth.
- `npm run reset` deletes every upload except the seed photos.

## Acceptance criteria
- [ ] Uploading a 5 MB JPEG returns `/uploads/<x>.jpg`, and that URL loads without a token.
- [ ] A PDF and a 16 MB image are both rejected with 400.
- [ ] An upload without a token returns 401.

Refs: ADR-0001

## Comments

- Implemented on branch worktree-agent-a3e3d6de7f949a127; verified by npm test.
