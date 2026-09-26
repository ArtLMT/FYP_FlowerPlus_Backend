---
type: module
module: material
phase: "01b"
status: ready
tables:
  - material
aliases:
  - Material module
tags:
  - flowerplus/module
---

# Module: Material

Catalog of raw materials (cut flowers and decorations) used in product recipes and custom orders.
Staff and Admin manage materials; customers interact with active materials through custom orders.

Requirements & Rules: [[Material rules]] (BR-MAT-01…18), [[Permissions]], [[0001-no-response-envelope|ADR 0001]], [[0004-role-model-and-management-prefixes|ADR 0004]], [[0005-removal-policy|ADR 0005]], [[0006-optimistic-locking-for-management-edits|ADR 0006]].

## Lifecycle & Statuses

```
[Created] ──▶ ACTIVE ──deactivate()──▶ DEACTIVATED
               ACTIVE ◀──reactivate()── DEACTIVATED
```

- **Starting status:** A newly created material starts as `ACTIVE` (BR-MAT-06).
- **Removal policy:** Materials are never deleted. Removal from use is done by setting status to `DEACTIVATED` (BR-MAT-08, ADR 0005).
- **Idempotency:** Deactivating an already `DEACTIVATED` material, or reactivating an `ACTIVE` material, returns `200 OK` without state change (BR-MAT-17).
- **Editing:** Both `ACTIVE` and `DEACTIVATED` materials can be edited (BR-MAT-16).

## Data Model & Schema (V7–V9 migrations)

The `material` table is created in migration `V7__create_material_table.sql`. The existing V1 `inventory_item` table remains untouched for legacy references until Product and Inventory modules migrate.

V8 hardens the existing Material table: it adds database constraints for its enumerated values and
positive whole-VND selling price. V9 adds the explicit optimistic-locking version used by Material
mutations (ADR 0006). Product's formerly planned V8 migration becomes V10.

### Table: `material`

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY DEFAULT gen_random_uuid() | Primary key |
| `name` | VARCHAR(100) | NOT NULL | Material name (saved trimmed, max 100 chars) |
| `type` | VARCHAR(50) | NOT NULL | Enum: `FLOWER`, `DECORATION` (BR-MAT-02) |
| `unit_of_measure` | VARCHAR(50) | NOT NULL | Enum: `STEM`, `PIECE`, `METRE`, `SHEET` (BR-MAT-14) |
| `selling_price` | NUMERIC(12, 0) | NOT NULL | Price per unit in whole VND (> 0) (BR-MAT-13) |
| `status` | VARCHAR(50) | NOT NULL | Enum: `ACTIVE`, `DEACTIVATED` (BR-MAT-06) |
| `created_at` | TIMESTAMP WITH TIME ZONE | NOT NULL | Creation timestamp |
| `updated_at` | TIMESTAMP WITH TIME ZONE | NOT NULL | Last update timestamp |
| `created_by` | UUID | REFERENCES user_account(id) ON DELETE SET NULL | Author user ID |
| `updated_by` | UUID | REFERENCES user_account(id) ON DELETE SET NULL | Last modifier user ID |
| `version` | BIGINT | NOT NULL, starts at 0 | Optimistic-locking version (V9, ADR 0006) |

### Indexes

- Unique case-insensitive index on trimmed name (BR-MAT-04, BR-MAT-15):
  `CREATE UNIQUE INDEX idx_material_unique_name ON material (LOWER(TRIM(name)));`

## Management Endpoints

All endpoints sit under `/api/manage/materials/**` and require `hasRole('STAFF')` (Staff and Admin permitted, Customer 403, Guest 401 per ADR 0004).

| Method | Path | Request Body / Params | Success Response | Error Responses |
|---|---|---|---|---|
| `POST` | `/api/manage/materials` | `CreateMaterialRequest` | `201 Created` + `Location`, `MaterialResponse` | `400 VALIDATION_FAILED`, `409 MATERIAL_NAME_EXISTS` |
| `PUT` | `/api/manage/materials/{id}` | `UpdateMaterialRequest` including `version` | `200 OK`, `MaterialResponse` | `400 VALIDATION_FAILED`, `404 MATERIAL_NOT_FOUND`, `409 MATERIAL_NAME_EXISTS`, `409 CONCURRENT_MODIFICATION` |
| `PUT` | `/api/manage/materials/{id}/deactivate` | `MaterialVersionRequest` (`version`) | `200 OK`, `MaterialResponse` | `400 VALIDATION_FAILED`, `404 MATERIAL_NOT_FOUND`, `409 CONCURRENT_MODIFICATION` |
| `PUT` | `/api/manage/materials/{id}/reactivate` | `MaterialVersionRequest` (`version`) | `200 OK`, `MaterialResponse` | `400 VALIDATION_FAILED`, `404 MATERIAL_NOT_FOUND`, `409 CONCURRENT_MODIFICATION` |
| `GET` | `/api/manage/materials/{id}` | None | `200 OK`, `MaterialResponse` | `404 MATERIAL_NOT_FOUND` |
| `GET` | `/api/manage/materials` | `?page=0&size=20&search=rose&type=FLOWER&status=ACTIVE&sort=NAME_ASC` | `200 OK`, `PageResponse<MaterialResponse>` | `400 VALIDATION_FAILED` (invalid page or size) |

`sort` is one of `NAME_ASC` (default), `NAME_DESC`, `PRICE_ASC`, `PRICE_DESC`, `CREATED_NEWEST`,
or `CREATED_OLDEST`. A missing or unsupported value uses `NAME_ASC`; id ascending is appended as a
tie-breaker for every option, so pagination remains stable.

## DTOs & Validation

### `CreateMaterialRequest` / `UpdateMaterialRequest`
- `name`: `@NotBlank`, `@Size(max = 100)` — trimmed before persistence (BR-MAT-15)
- `type`: `@NotNull` — must be `FLOWER` or `DECORATION` (BR-MAT-02)
- `unitOfMeasure`: `@NotNull` — must be `STEM`, `PIECE`, `METRE`, `SHEET` (BR-MAT-14)
- `sellingPrice`: `@NotNull`, `@Min(1)`, `@Digits(fraction = 0)` — whole number of VND (BR-MAT-13)
- `version`: `UpdateMaterialRequest` only; `@NotNull`, `@PositiveOrZero` — version returned by the read being edited (ADR 0006)

### `MaterialVersionRequest`
- `version`: `@NotNull`, `@PositiveOrZero` — required by deactivate and reactivate commands (ADR 0006)

### `MaterialResponse`
Fields: `id`, `name`, `type`, `unitOfMeasure`, `sellingPrice`, `status`, `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `version`.

## Optimistic Locking

Material is a human-edited aggregate root protected by ADR 0006. A client retains the `version`
returned by a read and sends it with every edit, deactivate or reactivate command. The service first
compares that value with the current row, while JPA's `@Version` check protects a genuinely
simultaneous database update. A successful state change returns the new version. A stale command is
rolled back as `409 CONCURRENT_MODIFICATION`; the server never retries it automatically. The client
reloads the Material and lets the Staff member decide whether to apply the intended change again.

## Error Codes Catalogued

- `MATERIAL_NAME_EXISTS` (409 CONFLICT): Material name already exists (case-insensitive trimmed comparison).
- `MATERIAL_NOT_FOUND` (404 NOT_FOUND): No material matches the given ID.
- `CONCURRENT_MODIFICATION` (409 CONFLICT): The submitted version is stale; reload before retrying the intended change.

## Auditing

`Material` entity extends `AuditableEntity` (`TimestampEntity` + `createdBy` / `updatedBy`). Auditing fields are populated automatically via `SecurityAuditorAware`.

Indexed in [[Modules]].
