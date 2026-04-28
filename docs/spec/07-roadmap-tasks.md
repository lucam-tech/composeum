# 07 — Roadmap Task Breakdown

This document converts the product roadmap in `06-product-roadmap.md` into
concrete task groupings. Unlike `05-tasks.md`, which captures the original
implementation sequence, this file is organised by user value and release
priority.

Use this file when deciding what to build next for consumers. Use
`05-tasks.md` when working through the original implementation plan or when
reconciling older work.

**Status markers:** `[ ]` todo · `[x]` done · `[~]` in progress

---

## Phase A — Adoption Baseline

Goal: make first-time integration fast, obvious, and stable.

**Roadmap:** `06-product-roadmap.md §6.6`

### RMAP-001 — Rewrite installation and quick-start docs

**Value:** High | **Risk:** Low | **Effort:** Medium

- [x] Rewrite the README installation section around one canonical Android setup
- [x] Add a short “5-minute setup” path for first-time adopters
- [x] Add a debug-only integration example using `ComposeumBrowserActivity`
- [x] Add a multi-module integration example
- [x] Add a short troubleshooting section for common setup failures
- [x] Ensure all coordinates, repo URLs, and target support statements match the real build

### RMAP-002 — Add a minimal starter sample

**Value:** High | **Risk:** Low | **Effort:** Medium

- [x] Create or trim a sample path that shows only the recommended default integration
- [x] Include one small group hierarchy and a few previews
- [x] Include one parameterised preview
- [x] Keep the starter sample visually simple and code-light
- [x] Document how the starter sample differs from any fuller showcase sample

### RMAP-003 — Reduce setup friction in the API and defaults

**Value:** High | **Risk:** Medium | **Effort:** Medium

- [ ] Review required KSP arguments and remove any that can be inferred safely
- [ ] Review generated registry naming defaults and simplify consumer-facing configuration
- [ ] Review the default browser bootstrapping path and remove unnecessary manual steps
- [ ] Separate advanced configuration from the default install path in docs and samples
- [ ] Add tests for the default integration path where possible

### RMAP-004 — Harden Maven Central publishing and CI release flow

**Value:** High | **Risk:** Medium | **Effort:** Medium

- [ ] Verify all intended artifacts are published for every supported target
- [ ] Verify all intended artifacts are signed correctly
- [ ] Add a documented release checklist for local and CI publication
- [ ] Add CI-friendly signing and publishing instructions
- [ ] Ensure release docs match the actual Gradle property and environment variable inputs
- [ ] Capture known release verification commands in docs

### RMAP-005 — Add target support and adoption guidance docs

**Value:** Medium | **Risk:** Low | **Effort:** Small

- [x] Document the current target support matrix in plain language
- [x] Explain Android-only entry points vs shared/browser composable usage
- [x] Document what is stable vs experimental
- [x] Link adoption docs from the README and relevant spec sections

**Exit criteria**

- [ ] A new Android consumer can reach first browser launch using only the README and sample
- [ ] The documented release flow can be executed without undocumented manual steps
- [ ] The default setup path no longer depends on understanding generated-code internals

---

## Phase B — Compile-Time Trust

Goal: make diagnostics a reliable part of the product.

**Roadmap:** `06-product-roadmap.md §6.7`

### RMAP-006 — Expand KSP validation coverage

**Value:** High | **Risk:** Medium | **Effort:** Medium

- [ ] Audit current validation rules against documented supported usage
- [ ] Add validation for unsupported preview function shapes
- [ ] Add validation for invalid `PreviewGroup` usage
- [ ] Add validation for invalid `@PreviewParam` declarations
- [ ] Add validation for unsupported parameter types
- [ ] Add validation for target-specific misuse in KMP source sets where possible
- [ ] Add validation for duplicate or ambiguous generated keys if not already covered

### RMAP-007 — Redesign diagnostics for actionability

**Value:** High | **Risk:** Low | **Effort:** Medium

- [ ] Review existing processor error messages for vagueness and internal wording
- [ ] Rewrite errors so they name the failing symbol or parameter explicitly
- [ ] Ensure each error explains the required code shape or nearest supported alternative
- [ ] Keep messaging consistent across validation paths
- [ ] Update tests to assert the new message style intentionally

### RMAP-008 — Expand compile-testing coverage

**Value:** High | **Risk:** Low | **Effort:** Medium

- [ ] Add success-path tests for canonical preview declarations
- [ ] Add focused failure tests for each validation rule
- [ ] Add multi-failure tests where multiple issues occur in one declaration
- [ ] Add KMP-related tests for `commonMain` and Android-specific preview patterns
- [ ] Add tests for generated metadata that powers source links or preview keys

### RMAP-009 — Document supported and unsupported preview patterns

**Value:** Medium | **Risk:** Low | **Effort:** Small

- [ ] Add a consumer-facing support matrix for preview declaration shapes
- [ ] Document supported parameter types and limitations
- [ ] Document expected group hierarchy patterns
- [ ] Document custom type behaviour and current extension limitations
- [ ] Link diagnostics docs from the README and API/spec docs where relevant

**Exit criteria**

- [ ] Common onboarding failures can be resolved directly from compiler output
- [ ] Supported and unsupported patterns are explicitly documented
- [ ] Diagnostics are protected by a broad compile-testing suite

---

## Phase C — Parameter UX

Goal: support real-world preview inputs and extension points.

**Roadmap:** `06-product-roadmap.md §6.8`

### RMAP-010 — Add first-class enum support

**Value:** High | **Risk:** Low | **Effort:** Small

- [ ] Detect enum-backed preview parameters automatically
- [ ] Render enum inputs as dropdowns without requiring `options = []`
- [ ] Ensure default value handling works for enum-backed preview state
- [ ] Add tests for enum parsing, rendering, and reset behaviour
- [ ] Update docs and examples to show enum support

### RMAP-011 — Improve nullable parameter UX

**Value:** High | **Risk:** Medium | **Effort:** Medium

- [ ] Define the state model for null vs non-null values
- [ ] Add nullable UX patterns for supported field types
- [ ] Support resetting to null explicitly
- [ ] Preserve sensible previous values where appropriate
- [ ] Add tests for nullable parameter interaction

### RMAP-012 — Design a custom type adapter API

**Value:** High | **Risk:** Medium | **Effort:** Large

- [ ] Define the consumer-facing extension API for custom parameter fields
- [ ] Define how parsing, rendering, defaults, and reset behaviour connect
- [ ] Ensure the design works with generated forms and runtime state
- [ ] Add at least one sample custom type integration
- [ ] Document lifecycle, limitations, and best practices for adapters

### RMAP-013 — Investigate structured and collection inputs

**Value:** Medium | **Risk:** Medium | **Effort:** Medium

- [ ] Identify low-risk collection/structured input cases worth supporting
- [ ] Reject cases that produce unclear or fragile UX
- [ ] Prototype one constrained structured input path if justified
- [ ] Document what is intentionally unsupported

### RMAP-014 — Add parameter presets

**Value:** Medium | **Risk:** Medium | **Effort:** Medium

- [ ] Define a preset model for reusable preview states
- [ ] Support selecting a preset and applying it to current preview state
- [ ] Decide whether presets are generated, declared, or configured in runtime DSL
- [ ] Add docs and examples for common design-system state presets

**Exit criteria**

- [ ] Common design-system previews can be expressed without stringly-typed workarounds
- [ ] Consumers have a documented path for unsupported custom types
- [ ] Parameter interactions remain predictable and test-covered

---

## Phase D — Daily-Use Browser Improvements

Goal: make the browser productive in medium and large catalogues.

**Roadmap:** `06-product-roadmap.md §6.9`

### RMAP-015 — Improve search and filtering

**Value:** High | **Risk:** Medium | **Effort:** Medium

- [ ] Audit current search behaviour and its limitations
- [ ] Add search across preview names, group names, tags, and descriptions
- [ ] Evaluate optional source-file-aware search where metadata exists
- [ ] Add tag and group filtering where it improves discovery
- [ ] Add tests covering medium-sized catalogue search behaviour

### RMAP-016 — Add persistent browser navigation state

**Value:** High | **Risk:** Medium | **Effort:** Medium

- [ ] Persist expanded/collapsed group state
- [ ] Persist last-opened preview where appropriate
- [ ] Persist user-facing browser settings that improve continuity
- [ ] Keep persistence semantics explicit across Android and Web targets

### RMAP-017 — Add favorites and recents

**Value:** High | **Risk:** Low | **Effort:** Medium

- [ ] Add favorite/unfavorite support in the browser UI
- [ ] Add a recent previews list or entry point
- [ ] Decide how favorites and recents are stored per platform
- [ ] Add tests for ordering, deduplication, and persistence

### RMAP-018 — Improve error and empty states

**Value:** Medium | **Risk:** Low | **Effort:** Small

- [ ] Add explicit empty states for no previews discovered
- [ ] Add empty states for search/filter returning no matches
- [ ] Improve messaging when preview rendering fails
- [ ] Improve messaging when a parameter form is unavailable
- [ ] Improve messaging when a source link cannot be resolved

### RMAP-019 — Strengthen source navigation

**Value:** High | **Risk:** Medium | **Effort:** Medium

- [ ] Audit current source-link generation and runtime opening behaviour
- [ ] Verify GitHub URL generation in multi-module repositories
- [ ] Add or refine GitLab-style source URL support if it fits current API design
- [ ] Ensure line numbers and file paths resolve consistently
- [ ] Add tests for link generation where possible

**Exit criteria**

- [ ] Users can find and reopen relevant previews quickly in larger catalogues
- [ ] Search and state persistence work reliably across typical usage flows
- [ ] Source navigation is reliable for the documented repository patterns

---

## Phase E — Team Workflow Features

Goal: make preview states shareable, reproducible, and useful beyond solo browsing.

**Roadmap:** `06-product-roadmap.md §6.10`

### RMAP-020 — Add deep-linkable preview state

**Value:** High | **Risk:** Medium | **Effort:** Large

- [ ] Define the route/state model for serialising preview state
- [ ] Support selected preview, parameter state, theme, locale, font scale, and UI scale
- [ ] Make deep links deterministic and stable enough to share
- [ ] Add tests for route encoding and decoding
- [ ] Document deep-link behaviour and limitations

### RMAP-021 — Add import/export for preview configurations

**Value:** Medium | **Risk:** Medium | **Effort:** Medium

- [ ] Define an export format for saved preview configurations
- [ ] Allow restoring a preview configuration into the current browser session
- [ ] Decide how import/export relates to deep links vs saved local state
- [ ] Add docs for review and bug-reproduction use cases

### RMAP-022 — Explore screenshot workflow hooks

**Value:** Medium | **Risk:** Medium | **Effort:** Large

- [ ] Identify the minimum useful integration point with screenshot testing workflows
- [ ] Decide whether to expose generated metadata, saved states, or test helper APIs
- [ ] Prototype one deterministic flow for consuming saved preview states in tests
- [ ] Document what Composeum does and does not provide in this area

### RMAP-023 — Add registry/debug inspection tools

**Value:** Medium | **Risk:** Low | **Effort:** Medium

- [ ] Expose a developer-facing way to inspect discovered previews
- [ ] Surface skipped previews and reasons where feasible
- [ ] Make generated keys and grouping easier to inspect during debugging
- [ ] Decide whether this belongs in runtime UI, generated output, or diagnostics tooling

**Exit criteria**

- [ ] A team can share and restore meaningful preview states
- [ ] Preview states can be used in debugging, review, or QA workflows
- [ ] Debugging generated registry contents no longer requires ad hoc inspection

---

## Phase F — Multiplatform Maturity

Goal: make target support clear, credible, and consistent.

**Roadmap:** `06-product-roadmap.md §6.11`

### RMAP-024 — Publish a support-level matrix

**Value:** High | **Risk:** Low | **Effort:** Small

- [ ] Define support levels such as stable, beta, experimental, and planned
- [ ] Map each current target to a support level
- [ ] Document supported preview features per target
- [ ] Document known limitations and target-specific caveats

### RMAP-025 — Audit Android vs Web/Wasm behaviour

**Value:** High | **Risk:** Medium | **Effort:** Medium

- [ ] Compare parameter widgets across targets
- [ ] Compare navigation behaviour across targets
- [ ] Compare settings persistence behaviour across targets
- [ ] Compare source-link and logging/error behaviour across targets
- [ ] Turn accidental differences into explicit decisions or fixes

### RMAP-026 — Add target-specific misuse prevention

**Value:** Medium | **Risk:** Medium | **Effort:** Medium

- [ ] Identify cases where Android-only assumptions leak into shared code usage
- [ ] Add validation or runtime guards for target misuse where possible
- [ ] Document target-specific setup mistakes and their remedies

### RMAP-027 — Write a full Web/Wasm integration guide

**Value:** Medium | **Risk:** Low | **Effort:** Medium

- [ ] Document setup for Web/Wasm consumers
- [ ] Document the browser entry-point pattern for Web/Wasm
- [ ] Document storage behaviour and limitations
- [ ] Document current feature gaps vs Android
- [ ] Link the guide from the README and roadmap docs

**Exit criteria**

- [ ] Consumers can tell exactly what target support to expect before adoption
- [ ] Android and Web behaviour differences are documented and intentional
- [ ] Web/Wasm setup no longer depends on reading source or old migration notes

---

## Release Mapping

These release buckets align the roadmap tasks with likely milestones.

### `v0.2`

- [ ] RMAP-001
- [ ] RMAP-002
- [ ] RMAP-003
- [ ] RMAP-004
- [ ] RMAP-005

### `v0.3`

- [ ] RMAP-006
- [ ] RMAP-007
- [ ] RMAP-008
- [ ] RMAP-009

### `v0.4`

- [ ] RMAP-010
- [ ] RMAP-011
- [ ] RMAP-012
- [ ] RMAP-013
- [ ] RMAP-014

### `v0.5`

- [ ] RMAP-015
- [ ] RMAP-016
- [ ] RMAP-017
- [ ] RMAP-018
- [ ] RMAP-019

### `v0.6`

- [ ] RMAP-020
- [ ] RMAP-021
- [ ] RMAP-022
- [ ] RMAP-023

### `v0.7+`

- [ ] RMAP-024
- [ ] RMAP-025
- [ ] RMAP-026
- [ ] RMAP-027

---

## Immediate Recommended Stack

If only a small set of work can be funded next, prioritise:

1. `RMAP-001` Rewrite installation and quick-start docs
2. `RMAP-003` Reduce setup friction in the API and defaults
3. `RMAP-004` Harden Maven Central publishing and CI release flow
4. `RMAP-006` Expand KSP validation coverage
5. `RMAP-007` Redesign diagnostics for actionability
6. `RMAP-010` Add first-class enum support
7. `RMAP-012` Design a custom type adapter API
8. `RMAP-015` Improve search and filtering
9. `RMAP-017` Add favorites and recents
10. `RMAP-020` Add deep-linkable preview state
