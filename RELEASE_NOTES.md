# RO Toolbox - Release Notes

## Unreleased - Security hardening

* Backend access is restricted to loopback and requires a private token generated for each app session.
* Only trusted app origins are accepted; development origins require explicit development mode.
* Account lists and account changes no longer return saved passwords. Leave the password blank when editing to keep the current password.
* Taskbar quick launch now routes account IDs through the native app instead of an unauthenticated temporary script.


## v1.2.2 - Minor ui changes

* Package installs now show an in-app confirmation dialog with the package name and Install/Cancel buttons.
* Installing packages shows an "Installing..." message with a spinner instead of a progress bar.
* Package update buttons appear only when an update is available, unless debug mode is enabled.
* Hover over an available update button, or focus it with the keyboard, to see the version and manifest description in a compact tooltip.

## v1.2.1 - Tags & Package Browser Improvements

* Filter packages by optional tags across all package browsers. Combine tags with name search and favorites.
* Larger package browser modals and flexible, narrower grid columns fit more cards per row.
* Favorites obey search and tag filters. Only the installed package stays visible when it does not match.
* One layout toggle now switches between grid and list views, showing the current layout. Package browser close buttons stay aligned at the top.
* Choose tag filters in a searchable modal, grouped by category with on/off toggles.

## v1.2.0 - UI & UX Overhaul
* Redesigned package browsing with grid/list layouts, favorites, and consistent icon controls.
* Install, uninstall, and preview packages directly in the browser; manage installed loot models there too.
* Simplified service overview: click a row to browse packages, with advanced controls in debug mode.
* Clearer names: Asset replacer and Buffs Animations.
* Clearer loot recommendations with Fix it and Don't show again actions linked to the config warnings setting.
* Fixed overlapping modals and UI package installation removing custom combat text.

## v1.1.6 - UI Improvements

* Improved package browser and live download progress.
* Choose a backup location; restores no longer create extra folders.
