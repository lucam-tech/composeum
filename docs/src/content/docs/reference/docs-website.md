---
title: Docs Website
description: Local development and deployment for the Starlight docs app.
---

The documentation website lives in the repo-level `docs/` directory and is designed to deploy
cleanly to Vercel.

## Local development

```bash
cd docs
npm install
npm run dev
```

Build for production:

```bash
npm run build
```

## Vercel

Use `docs/` as the Vercel project root.

Recommended settings:

- Framework preset: `Astro`
- Root directory: `docs`
- Build command: `npm run build`
- Output directory: `dist`

Once you know the final docs domain, add it to `astro.config.mjs` as `site` for canonical URLs and
metadata.

## GitHub Pages fallback

If you later decide to publish via GitHub Pages, the app is static and can be built there too. The
main tradeoff is that Vercel gives better preview deployments for pull requests and branches.
