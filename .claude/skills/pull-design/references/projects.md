# AMRO — Claude Design project IDs

Two projects on claude.ai/design, read with the **DesignSync** tool
(`list_files` to discover current paths, then `get_file`). Read-only — never
push. Do not hardcode file paths; always `list_files` first.

## Chatbot Design System — tokens, guidelines, components

- **projectId:** `c5a6030b-52d3-4ecc-ab51-4460eebdc7df`
- Type: design system. Appears in DesignSync `list_projects`.
- Canonical source for design tokens and component contracts.
- The name is shared with another app that renders against the same system. It
  is not a mistake, and this is the right project for AMRO's tokens.

## AMRO designs — app screen mockups

- **projectId:** `ce754ce1-efac-4206-9801-f8136b96493b`
- URL: https://claude.ai/design/p/ce754ce1-efac-4206-9801-f8136b96493b
- Type: **design project, NOT a design system** — so it does *not* appear in
  DesignSync `list_projects`. Address it by ID directly.
- Holds `Screen-TrendingList`, `Screen-MovieDetail`, `Component-TrendingFrame`,
  `Component-DetailFrame`, `Component-TrendingTwoPane`.
