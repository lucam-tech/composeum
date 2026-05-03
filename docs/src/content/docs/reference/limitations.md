---
title: Limitations
description: Current scope and where the library is intentionally conservative.
---

Composeum is already useful, but it still has a deliberate scope:

- Android is the primary host target today.
- `@PreviewParam` is basic mode by design, not a full declarative form language.
- Rich parameter surfaces still require the DSL or preview overrides.
- Unsupported host platforms should be treated as roadmap work, not as half-working targets.

Practical implications:

- Start with generated previews and only introduce manual registry code where the preview experience
  genuinely needs it.
- Keep function defaults authoritative for parameter defaults.
- Treat group, tag, and variant types as identity-bearing API, not just display labels.

If a use case feels like it needs reflection or runtime scanning, it is probably pushing against the
library’s intended compile-time model.
