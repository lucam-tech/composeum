---
title: Modules and Targets
description: What each module does and where Composeum is meant to run today.
---

## Modules

- `preview-annotation`
  Exposes `@ComposePreview`, `@PreviewParam`, `PreviewGroup`, `PreviewTag`, and variant-group marker
  types.
- `preview-ksp`
  Scans source during KSP and generates the registry and parameter form code.
- `preview-runtime`
  Hosts the browser UI, config DSL, registry DSL, state model, and platform-specific host APIs.
- `sample`
  Small Android sample showing the default host flow.

## Host targets

- Recommended: Android app or internal tools module
- Supported in shared code: declaring previews from `commonMain`
- Experimental: wasm/browser runtime in `preview-runtime`
- Not currently supported: desktop-native or iOS browser hosts

## Architectural model

1. Annotations describe previews in normal source.
2. KSP generates a registry at build time.
3. The runtime renders the registry and merges config, overrides, and parameter state at runtime.

This keeps preview discovery static while leaving the browser behavior highly configurable.
