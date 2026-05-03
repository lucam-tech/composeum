import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

const site = process.env.DOCS_SITE_URL;
const base = process.env.DOCS_BASE_PATH;

export default defineConfig({
  site,
  base,
  integrations: [
    starlight({
      title: 'Composeum',
      description: 'Compile-time Jetpack Compose preview infrastructure for Android-first teams.',
      logo: {
        src: './src/assets/composeum-mark.svg',
        alt: 'Composeum'
      },
      customCss: ['./src/styles/custom.css'],
      social: [
        {
          icon: 'github',
          label: 'GitHub',
          href: 'https://github.com/lucam-tech/composeum'
        }
      ],
      sidebar: [
        {
          label: 'Start Here',
          items: [
            { label: 'Overview', slug: '' },
            { label: 'Installation', slug: 'getting-started/installation' },
            { label: 'Quick Start', slug: 'getting-started/quick-start' }
          ]
        },
        {
          label: 'Guides',
          items: [
            { label: 'Annotations', slug: 'guides/annotations' },
            { label: 'Parameters', slug: 'guides/parameters' },
            { label: 'Browser Hosting', slug: 'guides/browser-hosting' }
          ]
        },
        {
          label: 'Advanced',
          items: [
            { label: 'Manual Registry', slug: 'advanced/manual-registry' },
            { label: 'Configuration', slug: 'advanced/configuration' }
          ]
        },
        {
          label: 'Reference',
          items: [
            { label: 'Modules and Targets', slug: 'reference/modules-and-targets' },
            { label: 'Limitations', slug: 'reference/limitations' },
            { label: 'Docs Website', slug: 'reference/docs-website' }
          ]
        }
      ]
    })
  ]
});
