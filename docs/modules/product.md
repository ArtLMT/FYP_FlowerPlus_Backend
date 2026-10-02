---
type: module
module: product
phase: "01c"
status: approved
tables:
  - category
  - product
  - product_category
  - product_image
  - product_recipe
aliases:
  - Product module
tags:
  - flowerplus/module
---

# Module: Product — approved Design

> **Approval state: behavior decisions approved 2026-09-29; Design remains proposed.** The developer
> approved the listed Product behavior recommendations. Cross-module schema, error catalog, and
> Design consistency work still need to be completed before this Design can be approved. On
> 2026-09-30 the developer confirmed that the current development database is empty and disposable;
> no legacy-row mapping is needed for this target. No task status changes follow from these decisions.

Product owns the customer-facing catalog definition: product type, base price, lifecycle, category
membership, ordered images, and recipe. Inventory owns material batches, production, finished stock,
availability, and reservations. Order owns carts, order snapshots, fulfillment, and cancellation.
Promotion owns sales and discount calculations. A boundary call between modules must not make Product
duplicate their mutable data.

This proposal follows BR-PROD-01…21, BR-MAT-05/06/11/13/14, BR-INV-13/16/18/19, BR-SALE-01…07,
Permissions, ADR 0001, ADR 0004, ADR 0005, ADR 0006, and the error contract. It makes no code or
schema changes.

## Recommended behavior

### Category rules

- Names are trimmed, nonblank, at most 100 characters, and unique ignoring case after trimming.
- Categories are a flat list. Guests and signed-in users may list categories; only Staff and Admin
  may create, rename, or delete them.
- Categories are versioned management resources (initial version `0`) under ADR 0006. The public
  category DTO remains `{id, name}`; management category reads and successful creates/renames include
  `version`, and rename/delete commands supply the version the caller read.
- Deletion is a real delete. Unlink the category from its products unless doing so would leave an
  Active product with no category; in that case reject the whole delete and return the affected Active
  product IDs in details.productIds (PRODUCT_CATEGORY_IN_USE). Draft and Deactivated products may
  become categoryless; activation checks for one.
- Public category responses are `{id, name}`. Management category responses include `{id, name,
  version}`. Both lists sort by normalized name, then ID.

Reason: a hidden Draft or Deactivated product can be reworked without being sellable. An Active
product must remain complete; staff must recategorize it before deleting its final category.

### Product validity and lifecycle

- A saved product requires a nonblank trimmed name (maximum 255 characters) and an immutable type (`PRE_ORDER` or `PRE_MADE`). Description (maximum 5,000 characters), positive whole-VND base  price, category, recipe, and image may be missing while Draft or Deactivated. Supplied values are validated immediately; each supplied recipe line must be valid.
- Product names need not be unique. Price accepts positive whole VND only, following Material
  selling-price practice. `NUMERIC(12,0)` gives an explicit upper bound of 999,999,999,999 VND.
  Reject excess precision rather than silently rounding.
- Recipe quantity is positive, has no more than two decimal places, and may be fractional only for
  `METRE`. Reject duplicate material IDs within one recipe.
- Draft is unpublished and may be incomplete. Only name and type are required to save it. It is
  never public.
- Active requires name, description, positive whole-VND price, at least one category, a valid recipe with at least one line, and one to five valid images. It is public even when out of stock.
- Deactivated is hidden and cannot be bought. Like Draft, it may have missing description, price,
  category, recipe or images; it retains its history.
- Allowed transitions: Draft → Active; Active → Deactivated or Draft; Deactivated → Active or
  Draft. Reject other transitions, including repeating the current state. Every transition to
  Active runs the full completeness check; stock is not required.
- Staff may edit a hidden product into an incomplete state. An Active product must remain complete after every edit; reject a change that would make it incomplete. Product type is immutable after
  creation; a type mistake requires a new product.

Reason: Draft means a product exists but is not yet published. Requiring all sale fields at Draft
creation would prevent staff from saving early work. Activation is the explicit publication gate;
stock remains derived and does not block publication.

### Material references and recipes

- A newly added recipe material must be Active. A retained Deactivated material may remain, have its quantity changed, or be removed. If removed and later re-added, it is a new addition and must be Active.
- A material's unit and type cannot change while any current recipe or inventory batch refers to
  it. Check references in the Material update transaction. Do not add a permanent “ever used” lock.
- PreOrder availability is zero if a recipe material is Deactivated, consistent with Material's
  rules. PreMade stock remains sellable after a source material is Deactivated because that material was consumed at production.
- Replace a submitted recipe as a complete set, keyed by material ID, in one transaction. Validate
  all lines before applying the diff; never leave a partial recipe after a failed edit.
- Refuse a PreMade recipe change while it has any remaining or reserved finished units. Staff can
  sell or explicitly write off those units, then edit; for a materially different bouquet, create a
  new product.

Reason: a recipe describes the bouquet being sold. Changing it while old units remain would make
the current catalog description disagree with the physical stock. Inventory owns the stock guard;
Product must call it within a transaction/locking boundary.

### Images and file storage

- Keep one to five images for Active products; Draft and Deactivated products may have zero to five. Refuse removal of the final Active image. Activation/reactivation requires at least one image.
- Define the 2 MB limit as 2,000,000 bytes, inclusive. Accept JPG, PNG, and WebP by validating the actual file format; do not trust the extension or client-provided content type.
- Add appends to the end. Reordering supplies the complete current set of image IDs exactly once; first in order is the main image. Do not store a second `is_main` flag.
- **Approved 2026-09-29:** Spring saves and serves images from a configurable
  persistent filesystem directory. While Spring runs on the host, use a host folder outside source
  and build directories. If Spring is containerized, mount a named `product_images` Docker volume into it. Nginx is optional for later image delivery; it is not the storage mechanism or an upload
  service. Persist relative media keys in PostgreSQL, not host paths or localhost URLs. Backups must include both the database and the image files. This recommendation assumes one deployment host; several application hosts or hosting without persistent disk calls for shared object storage.
- Store uploaded files privately, outside any static web root. Stage uploads, validate them, and
  coordinate a short product lock before attaching them. A failed database change cleans the
  staged/orphaned file. For deletion,
  remove the database reference first and then the file; failed filesystem cleanup is retried and
  logged. The public media handler serves an image only while its Product is Active; a previously
  public key stops resolving when the Product becomes Draft or Deactivated. Hidden-product images
  are served only through a Staff/Admin-protected management endpoint. **Recommendation pending
  Design approval:** use `Cache-Control: no-store` for public media responses so an intermediary
  does not keep serving a cached image after deactivation. A file already downloaded by a visitor
  cannot be recalled.
- Media keys are opaque, server-generated single-segment identifiers. Resolve only keys referenced
  by image records, normalize the resulting path, and reject paths outside the configured private
  media root. Never accept a host path or arbitrary URL from a request; access control governs future
  requests.

Reason: persistent filesystem storage is an established approach supported by current frameworks.
The host-run Spring process can use a configured local directory; a containerized Spring process
can use the same filesystem operations with a mounted Docker volume. A container's own writable
layer is not persistent storage across container replacement. A local Docker volume also does not
share files automatically between separate servers. If the deployment requires a separate storage
API or multiple application hosts, review an object-storage service instead. No provider has been
selected. Nginx remains optional and is not a storage service. Use shared object storage only if
persistent local storage no longer fits the deployment topology.

References checked during the storage review:
[Docker volumes](https://docs.docker.com/engine/storage/volumes/),
[Spring resource serving](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/static-resources.html),
[Django filesystem storage](https://docs.djangoproject.com/en/5.2/topics/files/).

### PreMade production and inventory boundary

- Inventory should track each PreMade production run as a finished-stock lot. A Staff/Admin action selects an Active PreMade product and a positive whole-number quantity. Inventory snapshots the current recipe, consumes required material batches, records the material cost, and creates the lot atomically.
- A customer order reserves finished units from lots; it does not consume the recipe materials
  again. Order line cost is derived from the production lots reserved for that line.
- Every finished lot requires a staff-entered expiry. Its expiry cannot be later than the earliest
  expiry among consumed flower batches; decoration batches have no expiry and do not constrain the lot.
- Derive sellable finished stock from lot balances. Do not keep a separately editable Product stock
  counter that can drift from Inventory.
- Inventory production is tracked by T-INV-08. Inventory Design still decides stored cost precision, finished-lot write-off mechanics, and exact ledger vocabulary.

Reason: production-run lots preserve the source and cost of finished stock. A lone count cannot
explain different production costs or identify stock to write off. Product owns the recipe, while
Inventory owns movements and quantity contention.

### Price, promotion, and orders

- Product base price is independent from Material selling price and production cost. Promotion owns  sale schedules and the effective price shown to customers.
- Permit base-price changes during running or scheduled sales only when every affected sale remains valid and its final price remains positive. Reject an invalidating price edit; staff must change the sale explicitly. Do not end a sale automatically (BR-SALE-07).
- Sale windows for one product cannot overlap, whether scheduled or active. Windows are
  start-inclusive/end-exclusive. Percentage prices use HALF_UP rounding to whole VND, and the
  resulting sale price must remain positive. These decisions are in BR-SALE-03.
- Moving an Active product to Draft or Deactivated hides it and blocks new cart additions and new orders. Keep an existing cart line and show it as unavailable/out of stock; checkout refuses it. Do not silently discard it. Existing unpaid and paid orders keep their accepted terms and follow Order's deadlines and status rules. Do not cancel, refund, or release reservations as a side effect of Product deactivation.
- Deactivation or return to Draft leaves any sale schedule intact. If the product is reactivated
  during the sale window, the sale applies; after its window, base price applies.
- Order snapshots the product identity/name/type, quantity, paid unit price/discount, and recipe/material
  information needed to fulfill the order. Inventory allocations retain lot/batch references and
  costs. Historical display and fulfillment never depend on the live Product recipe.
- Preserve the separate Custom Order model. Product types remain only PreOrder and PreMade.

Reason: a placed order is a customer commitment; a cart is not. BR-PROD-14 promises historical
orders survive edits, while the current schema does not yet capture every value fulfillment needs.
Order and Promotion designs must carry their part of these recommendations.

### Public catalog and availability

- Public DTOs do not contain recipe lines, material costs, inventory lot details, audit fields, or
  edit versions. Public list returns ID, name, type, main image, categories, base/effective price,
  and an availability boolean. Detail adds description and all ordered images.
- Public list/detail show Active products only. Draft and Deactivated return 404 on public paths,
  including when the caller is Staff. Staff use management paths for non-public records.
- Search is case-insensitive name containment. Support one category filter and inclusive price
  minimum/maximum against effective price. Sorting is `CREATED_NEWEST` (default), `PRICE_ASC`, or
  `PRICE_DESC`, with ID ascending as a final tie-breaker. Page defaults to 20 and caps at 100.
  Missing/unsupported sort falls back to `CREATED_NEWEST`; malformed values fail. A valid filter
  with no matches returns an empty page.
- Out-of-stock Active products remain in results. A public availability flag means “usable stock
  observed now,” not a promise for a future preparation date. Cart rechecks availability on add;
  Order rechecks and atomically reserves stock at order creation. Catalog availability does not reserve.
- For PreOrder, capacity is the minimum whole quantity supported by each recipe material using
  stock available for preparation today. For PreMade, it is unreserved, unexpired lot quantity.
  Inventory performs grouped/bulk availability queries for one page; Product must not issue one
  inventory query per product or recipe line. Do not add Redis caching initially.
- The public catalog reports usable stock now. Cart rechecks availability on add but reserves
  nothing. Order captures the selected preparation date and atomically rechecks and reserves stock
  usable through that date. Inventory owns the lot/batch locking; Product does not claim future
  availability from a current catalog read.

Reason: a visible stock check can become stale before checkout, and products may share materials.
Only the atomic Order/Inventory reservation prevents overselling.

### Permissions, concurrency, and errors

- Public category/product GET paths are whitelisted in `SecurityConfig`; management paths remain
  under `/api/manage/**` and require Staff/Admin. Method-level authorization is also required.
  Guest management is 401; Customer management is 403, before resource lookup.
- Product is an ADR 0006 aggregate root with a numeric version starting at 0. Every update, state
  change, recipe change, category membership change, and image mutation carries the version read by
  the client. V1 never permanently deletes Products, including those with no historical orders;
  the API has no Product DELETE endpoint (BR-PROD-11).
  Successful changes return the new version. Stale input returns `409 CONCURRENT_MODIFICATION`; no
  automatic retry.
- Recipe, category-membership, and image mutations advance the parent Product version explicitly.
  Child rows are protected through the parent; do not add independent child versions.
- Use short row locks only for concrete cross-record races (category deletion versus membership changes,
  recipe update versus material deactivation/reference edits, recipe change versus PreMade
  production, price versus sales, and order reservation versus deactivation). Sort IDs consistently
  when multiple rows are locked. Never hold locks while a person edits a form or a file uploads.
- **Approved transaction approach (2026-09-30):** keep each invariant check and its database write
  in one short transaction; lock only the shared rows that the competing operations need, then
  recheck the invariant after acquiring the lock. A transaction either commits all of its database
  changes or none. The lock acquisition order serializes overlapping operations; it is not a fixed
  business priority or a guarantee based on request arrival time. After the earlier transaction
  commits, the waiting operation re-reads and rechecks the committed state: it may also commit if its
  invariant still holds, or fail atomically if it does not. If the earlier transaction rolls back,
  the waiting operation proceeds against the last committed state. Product `version` still detects
  stale manager updates; it does not coordinate requests from Order, Inventory or Promotion that did
  not submit that Product version.
- **Examples; exact ownership/order still needs cross-module agreement:** Order owns order placement
  and its outer transaction; it locks/rechecks the Product as Active before Inventory reserves
  stock in that same transaction. Product deactivation uses the same Product lock. If order gets it
  first, the order and reservation commit before deactivation; if deactivation gets it first, the
  order sees a hidden Product and fails. For category deletion, lock the category and affected
  Products, recheck the Active-last-category rule, then either return the affected IDs without
  unlinking or unlink hidden Products and advance their versions. Product price edits must coordinate
  with Promotion sale changes, and Product recipe edits with Inventory production/material
  references. The owning module and all participants must agree the row set, transaction boundary,
  and global lock order; sort same-kind rows by ID and never guess an order in Product alone.
- Retain the existing ErrorResponse frame. Use `VALIDATION_FAILED` with field details for bad
  user input; `404` for missing or publicly hidden products; `409 CONCURRENT_MODIFICATION` for
  stale edits; distinct catalogued 409 codes for category-in-use, invalid state, last image,
  image limit, `PRODUCT_RECIPE_MATERIAL_INACTIVE`, `MATERIAL_IN_USE`, PreMade stock conflict, and
  sale/price conflict; `413` for oversized files. `PRODUCT_CATEGORY_IN_USE` includes
  `details.productIds`, the UUIDs of Active products that would lose their last category. Add a typed
  ErrorDetails variant when implementing T-PROD-01; multipart uploads retain the standard error frame.
- Management lists use `PageResponse<T>` and stable sorting. Management read DTOs include version,
  state, type, recipe with material unit/status, categories, images in order, timestamps and audit
  actors. Public DTOs remain separate.

## Proposed API surface

All management endpoints use `/api/manage/**`, Staff/Admin authorization, record lookup after
authorization, and ADR 0006 versions for mutations.

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/categories` | Public category list |
| GET | `/media/{key}` | Public delivery only while the owning Product is Active; otherwise 404 |
| GET | `/api/manage/products/{id}/images/{imageId}/content` | Staff/Admin delivery of an image in any Product state |
| GET/POST | `/api/manage/categories` | List/create category; management DTO includes version |
| GET/PUT/DELETE | `/api/manage/categories/{id}` | Read/rename/guarded delete; rename/delete supply category version |
| GET/POST | `/api/manage/products` | Search management products/create Draft |
| GET/PUT | `/api/manage/products/{id}` | Read/edit a product |
| POST | `/api/manage/products/{id}/activate` | Activate or reactivate |
| POST | `/api/manage/products/{id}/deactivate` | Deactivate |
| POST | `/api/manage/products/{id}/images` | Multipart image upload plus version metadata |
| PUT | `/api/manage/products/{id}/images/order` | Set the complete ordered image ID list |
| DELETE | `/api/manage/products/{id}/images/{imageId}` | Remove image with product version |
| GET | `/api/products` | Public paged catalog |
| GET | `/api/products/{id}` | Public product detail |

Creation returns `201 Created` and `Location`; updates/state/image changes return the updated
management representation. Category deletion and image removal return `204 No Content`. There is no
Product DELETE endpoint in V1. Inventory production/availability, cart, order, and promotion endpoints
belong to their respective module Designs.

## Proposed schema and migration plan

Keep current table names; `@Table(name=...)` values are frozen. Product design starts after Material
V9, so the first Product migration is V10. Exact migration numbering is checked against the repo at
implementation time.

| Table | Proposed changes |
|---|---|
| `category` | Add normalized unique-name enforcement; timestamps, audit actor IDs; `version BIGINT NOT NULL DEFAULT 0` |
| `product` | Add immutable type; constrain statuses to Draft/Active/Deactivated; whole-VND base price; name/type required; description and price may be absent while hidden; timestamps, audit actors; `version BIGINT NOT NULL DEFAULT 0` |
| `product_recipe` | Reference `material(id)`; `NUMERIC(12,2)` positive quantity; unique `(product_id, material_id)`; recipe completeness is required only for Active products |
| `product_category` | Keep composite primary key; ensure deletion/membership constraints support atomic guarded operations |
| `product_image` | Store relative media key; constrain nonnegative ordered slots and unique `(product_id, display_order)` |
| Inventory tables | Finished production lots, movements, and reservation support, designed in Inventory |
| Order tables | Product/order-line snapshots needed for fulfillment, designed in Order |
| Promotion tables | Sale schedule integrity and calculation data, designed in Promotion |

`product` and `category` are the versioned aggregate roots. The child tables `product_recipe`,
`product_category`, and `product_image` have no independent version column; their mutations advance
the parent Product version. This matches ADR 0006 and the existing Material pattern (`Long` / SQL
`BIGINT`).

The developer confirmed on 2026-09-30 that the current development database contains no data and may
be reset; no existing Product, category, image, or recipe rows need mapping for this target. The V10
migration still creates/alters the schema described above; no database reset/drop was performed as
part of this documentation update. If the migration is later aimed at any populated environment, stop
and inspect its Product, category, image, and recipe rows first. Review explicit status/type/price/
category/media/recipe mappings and remediation before migrating; never infer Product type, round
prices silently, collapse categories, discard media, or guess recipe mappings.

## Proposed error outcomes

Use the project's standard `ErrorResponse` body and catalog every new code in
`docs/error-codes.md` before its endpoint is built.

| Condition | Outcome |
|---|---|
| Missing or malformed product/category ID | 404 |
| Guest on management path | 401 `UNAUTHENTICATED` |
| Customer on management path | 403 |
| Invalid fields, recipe quantities or filter bounds | 400 `VALIDATION_FAILED` with field details |
| Unsupported/corrupt image content | 400 `PRODUCT_IMAGE_INVALID` |
| Oversized image | 413 `PRODUCT_IMAGE_TOO_LARGE` |
| Hidden Draft/Deactivated on public path | 404 |
| Stale management version | 409 `CONCURRENT_MODIFICATION` |
| Duplicate category name | 409 `CATEGORY_NAME_EXISTS` |
| Category deletion would orphan an Active product | 409 `PRODUCT_CATEGORY_IN_USE`, with `details.productIds` containing the affected Active product UUIDs |
| Invalid lifecycle transition or immutable-type update | 409 `PRODUCT_INVALID_STATE` / `PRODUCT_TYPE_IMMUTABLE` |
| Image count limit/final Active image conflict | 409 `PRODUCT_IMAGE_LIMIT_REACHED` / `PRODUCT_IMAGE_REQUIRED` |
| Newly added inactive recipe material or PreMade stock guard | 409 `PRODUCT_RECIPE_MATERIAL_INACTIVE` / `PRODUCT_RECIPE_STOCK_CONFLICT` |
| Base-price change conflicts with an existing sale | 409 `SALE_PRICE_CONFLICT` |

The error catalog defines multipart handling for `POST /api/manage/products/{id}/images`: it keeps
the standard `ErrorResponse` frame and names malformed request, invalid image, and size-limit errors.

## Backlog order and ownership

Approved sequence: Product management T-PROD-01…06 → Inventory production/reservation/availability
T-INV-08/06/07 → public Product T-PROD-07/08. Both backlog routes now record this boundary; public
availability is not stubbed. Inventory T-INV-08 was added after checking the Inventory route.

Product records its side of cross-module behavior. Later Order tasks prove order snapshots,
preparation-date reservations, and retained cart lines; later Promotion tasks prove sale price
calculation/display and sale-schedule behavior. Those proof points are named in the affected tasks.
Product's related-rule list includes Auth, Cart, Order, Inventory, Promotion and Permissions. The
custom-versus-catalog boundary stays separate: Product has only PreOrder and PreMade; Order owns
Custom Orders.

## Approval record — decisions approved 2026-09-29

The developer approved the recommendations in the prior checklist. The behavior choices are now
recorded in Product, Inventory, Order, Cart and Promotion rules, the SRS decision log, Permissions,
and the affected task notes. This approval does not approve the technical Designs or change task
statuses. No task was marked ready or done.

| # | Approved decision | Recorded in |
|---|---|---|
| 1 | Category deletion may leave Draft/Deactivated products categoryless; protect Active products and recheck on activation. | BR-PROD-15; T-PROD-01/04 |
| 2 | Draft requires name and type only; validate any supplied data; activation requires the complete saleable product. | BR-PROD-02/04/07; T-PROD-02/04 |
| 3 | Product type is immutable; price is positive whole VND; Active and Deactivated may return to Draft for rework. | BR-PROD-06/17; T-PROD-02/05/06 |
| 4 | Retained inactive recipe materials may remain/change/remove; newly added materials must be Active; guard current references. | BR-MAT-06/11; T-PROD-02/05 |
| 5 | Block PreMade recipe edits while any finished quantity remains available or reserved. | BR-PROD-20; T-PROD-05; T-INV-08 |
| 6 | Production creates traceable finished lots, stores recipe/batch cost provenance, requires a staff-entered expiry, and caps it at the earliest consumed flower batch expiry. | BR-INV-14/18/19; T-INV-08 |
| 7 | Validate actual image format and a 2,000,000-byte limit; hidden products may have zero images; store files privately; serve Active-product images publicly and hidden-product images only to Staff/Admin; use persistent filesystem storage and a named Docker volume when containerized; Nginx is optional. | BR-PROD-16; T-PROD-03; Permissions |
| 8 | Reject base-price edits that invalidate sales; prohibit overlapping sale windows; use HALF_UP whole-VND percentage prices and require a positive result; preserve schedules through deactivation/Draft. | BR-PROD-21; BR-SALE-03/07; T-PROD-05/06; T-PROM-06/07 |
| 9 | Orders snapshot product and fulfillment data; cart lines for hidden products remain and show unavailable/out of stock; checkout refuses them. | BR-ORD-01; BR-CART-01; T-PROD-06; T-ORD-01/11 |
| 10 | Keep Material/Product domain enums endpoint-specific; public DTOs omit recipe/cost/audit/version; Staff/Admin management views include necessary recipe/version/audit data; use explicit filtering and sorting defaults. | Permissions; T-PROD-01/02/07/08 |
| 11 | Batch availability queries per page report usable stock now; Cart holds none; Order rechecks and reserves atomically for the selected preparation date. | BR-PROD-19; BR-ORD-11; T-INV-06/07; T-ORD-01/11 |
| 12 | Whitelist public category/product GET and Active-product media reads; hidden-image content uses a role-protected management route; frontend uses same-origin API/media proxy. | Permissions; T-PROD-07/08 |
| 13 | Product version covers aggregate edits; only concrete cross-record races get short locks; Inventory/Order own stock locks; Product conflict codes are added to the error catalog. | ADR 0006; `docs/error-codes.md`; T-PROD-01…06 |
| 14 | Route Product management → Inventory production/availability → public Product; add Inventory T-INV-08. | Product and Inventory backlog routes |
| 15 | V1 retains every Product and exposes no permanent Product DELETE endpoint, even if no historical orders exist. | BR-PROD-11; T-PROD-06; ADR 0005 |
| 16 | A category-deletion conflict returns the affected Active Product UUIDs in typed error details. | BR-PROD-15; T-PROD-01; docs/error-codes.md |

### Remaining design work

- **Resolved for current development database, 2026-09-30:** developer confirms there are no rows
  to preserve and the database is disposable. No legacy `inventory_item`→`material` row mapping is
  needed for this target. If another migration target contains data, inspect it and review explicit
  mappings before migration.
- **Approved by the developer 2026-09-30:** Product Design and the category locking contract for
  T-PROD-01. This authorizes work on the first Product backlog item; other items retain their
  existing statuses and must meet their own readiness/dependency gates.
- **Recorded 2026-09-30:** categories are versioned management resources; management DTOs expose the
  version and rename/delete commands supply it under ADR 0006. Public category DTOs remain `{id,name}`.
- **Approved by the developer 2026-09-30.** T-PROD-01 category operations lock Category rows
  before affected Product rows, sorting IDs within each kind. Category deletion and future Product
  category-membership writes must follow that order and recheck after locking. Remaining technical
  review concerns other cross-module transaction ownership and lock ordering. The
  shared method was approved 2026-09-30: keep each invariant check and write in one short database
  transaction; lock only rows shared workflows race on, then recheck the condition after acquiring
  the lock. Agree one global lock order with Inventory/Order/Promotion and sort rows of the same kind
  by ID; do not guess the order in Product alone. Inventory/Order own stock locks. The current storage recommendation assumes
  one application host; the developer still needs to confirm whether the target hosting environment
  has persistent local storage or needs shared object storage.
- **Resolved 2026-09-29:** V1 retains every Product and exposes no permanent Product DELETE endpoint,
  including for Products with no historical orders (BR-PROD-11).
- **Inventory Design:** settle finished-lot schema and transaction vocabulary, cost precision and
  rounding, expiry/write-off mechanics, and cost allocation during reservation. T-INV-08 records
  these as open questions.
- **Order Design:** settle the schema for immutable product/recipe snapshots, where the selected
  preparation date comes from, and its coordination with atomic stock reservation.
- **Promotion Design:** carry the approved BR-SALE-03 calculation and overlap behavior into its
  persistence/API contract. No sale schedule is edited automatically by Product.

### Original developer responses — preserved for context

The prior responses remain below verbatim as historical context. The approval record above is the
current decision source.

1. Category deletion guard and whether it applies to every product state: No, DRAFT product state doesn't account to this, tho it's still need a category if want to be ACTIVATE.
2. Complete Draft semantics and image exception: I'm not sure what do you mean by this. but this is my prediction: A draft is like a product but it's not published yet. That's my definition for draft product. Because of that it's not need the require fields, only when it's activate then it's treat as a complete product and have to fulfill every filed that's required
3. Immutable Product type; whole-VND price; validation length limits: I aggree with your suggestion on this. But the states changes, I need you to look at the new definition I made, Is it normal and understandable to make an Active Product to become draft again or deactive product to be draft?
4. Retained inactive recipe materials and reference-guard behavior: I agree with your suggestion
5. PreMade recipe edit restriction while stock remains: Yes
6. Production-run lots, cost ownership, and finished-lot expiry rule: Yes
7. Image byte limit, lifecycle limits, validation, storage location, and public asset access: I need to be explained and enlighted more.
8. Sale/base-price conflicts, overlap, rounding, and deactivation behavior: Yes, But I feel like there's cons of this
9. Cart and existing-order behavior after deactivation; exact order snapshots: I didn't see the Cart mention, but Order itself is a snapshots, the changes doesn't effect it. while cart does, an deactived product should be marked as out of stocks
10. Public recipe visibility, DTO fields, permissions, filters, sort fallbacks, and page limits: Aggree, can the same ENUM use for filters in Material/Product?
11. Bulk stock calculation boundary, preparation date, and reservation coordination: I do not know which references is this in the proposed design
12. Public `SecurityConfig` routes and same-origin frontend/media proxy: Agree
13. Product version coverage, locks for cross-module invariants, error catalog additions: Now it doesn't seem like product have invariants, I need you to check about this if I forgot about this
14. Product route placement after Inventory for public browsing, plus a new Inventory production task: I do not understand
15. Legacy `inventory_item`→`material` mapping and populated-database migration strategy: I do not understand which problem this is references to
