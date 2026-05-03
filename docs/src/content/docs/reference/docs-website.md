---
title: Docs Website
description: Local development and deployment for the Starlight docs app.
---

The documentation website lives in the repo-level `docs/` directory and is designed to deploy
cleanly to Vercel.

Production URL: `https://composeum.lucam.tech`

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
- Production domain: `composeum.lucam.tech`
- Custom domain target: `https://composeum.lucam.tech`

The Astro config already defaults `site` to `https://composeum.lucam.tech` for canonical URLs and
metadata. Override `DOCS_SITE_URL` only when you explicitly need a different host.

## GitHub Pages fallback

If you later decide to publish via GitHub Pages, the app is static and can be built there too. The
main tradeoff is that Vercel gives better preview deployments for pull requests and branches.
