# Future Implementation Plan

## Goal

Make the local spreadsheet-to-orders-to-print workflow predictable and safe to repeat, while keeping the application a small single-process Spring Boot tool.

## Current Context

- The committed `application.properties` has an empty `beddingplants.geolocation.googleApiKey` value. The working tree currently has a non-empty local value; that edit is not committed. Do not copy the key into this plan or commit it.
- The app is used locally and infrequently. Do not introduce hosted services, multi-user infrastructure, or full authentication as part of this work.
- Delivery route planning is incomplete: proximity grouping is a TODO, the configured radius is unused, and grouped routes can exceed the plant limit.
- Route grouping does not include delivery day, while the resulting route takes its day from one order. Printing reads saved routes and accepts a sort parameter that is not applied.
- Spreadsheet import persists sale, plant, and customer/order changes in separate steps, so a later failure can leave partial data.
- Spring Modulith is not a required refactor. Reconsider it only if the team needs enforceable module boundaries or module-level documentation; prefer incremental package cleanup for now.

## Tasks

- [ ] **Keep credentials out of commits.** Move the Maps key to an environment variable or an ignored local override; keep the tracked default empty. Remove or externalize committed database passwords where they are not needed. Verify the key is absent from the staged diff and committed config without printing it.
- [ ] **Specify route invariants.** Define compatible delivery days, whether the radius is straight-line or road distance, the per-route plant-count rule, treatment of missing geolocations, and whether manually assigned routes are immutable during generation.
- [ ] **Complete route planning.** Group only compatible orders, enforce plant-count and radius constraints for every route, and leave ungroupable orders visible for manual assignment. Assign stable route numbers and ordering. Add tests for mixed days, over-capacity groups, missing coordinates, existing routes, and repeated generation.
- [ ] **Connect generation to printing.** Provide a clear local workflow to generate or update routes before printing. Apply the requested sort order and verify printed routes are ordered by delivery day and route number.
- [ ] **Make imports atomic.** Parse and validate both workbook sheets before persistence, then save within one transaction. Add tests proving invalid workbooks do not leave a partial sale and that repeat imports behave as intended.
- [ ] **Tighten local defaults.** Bind the server to loopback by default and disable the H2 console and unnecessary actuator exposure outside development. Keep these controls proportionate to a single-user local app.
- [ ] **Refactor only along touched boundaries.** Use constructor injection, keep import parsing/mapping separate from persistence orchestration, and move repositories/models toward their owning feature incrementally. Add a lightweight architecture test if package boundaries need enforcement; do not split into Maven modules or add Modulith unless a concrete need emerges.
- [ ] **Verify the end-to-end workflow.** Run Maven tests and exercise spreadsheet import, order output, route generation, and printed route output against representative data. Record unresolved assumptions before making route rules implicit in code.

## Done When

- Orders cannot be placed on a generated route for the wrong day, and every generated route satisfies documented capacity/radius rules or is explicitly flagged for manual review.
- Invalid imports leave persisted data unchanged; repeat import behaviour is covered by tests.
- The local user can generate and print routes without relying on undocumented API call ordering, and print sorting is deterministic.
- The Maps key is external to tracked files, and the local-only exposure defaults match the intended use.
- The test suite covers route generation and the import-to-print workflow.
