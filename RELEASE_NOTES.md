# RO Toolbox – Release Notes

## v0.4.4 – Buffs overhaul and manifest fixes

### ✨ Improvements

- Added the full Buffs texture replacer flow with profile discovery, install/clear actions, and .default restore support.
- Fixed Buffs profile discovery to read downloaded subfolders containing `manifestBuffAnimations.json` and ignore non-profile folders.
- Hardened the Buffs status path to avoid crashes when no Buffs profile is installed yet.
- Kept the UX consistent with the other texture replacer services and preserved the app’s existing resource-management flow.

## v0.4.3 – Visual refresh and quick launch polish

### ✨ Improvements

- Fixed quick profile launch so it no longer shows the blocking loading modal when launching the game.
- Refreshed texture replacer service headers with cleaner package status badges, tighter typography, and improved hierarchy.
- Redesigned the package browser modal with a more polished ROSE-style header, richer package selector, and upgraded preview cards.
- Brought login manager and config editor visuals in line with the refreshed blue panel style used across the app.
- Updated typography to better match the official ROSE Online website using Josefin Sans for body text and Spectral SC for display headings.
