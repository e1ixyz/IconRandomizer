# IconRandomizer

Simple Velocity plugin that swaps the server icon on every server list ping.

## Features
- Picks a random favicon for each ping (no caching between refreshes).
- Reads icons from a folder or an explicit list of files.
- Lightweight: no commands, no dependencies beyond Velocity API.

## Requirements
- Java 17+
- Velocity 3.1.0

## Installation
1) Build: `mvn package` (jar is produced at `target/iconrandomizer-1.0.0.jar`).
2) Drop the jar into your Velocity `plugins` folder.
3) Start/restart the proxy once to generate `plugins/IconRandomizer/config.properties`.

## Configuration (`plugins/IconRandomizer/config.properties`)
```properties
# icon-folder: folder (relative to your Velocity root) containing .png icons.
# If icon-files is empty, all .png files in icon-folder will be used.
icon-folder=icons

# icon-files: optional comma-separated list of icon paths (relative or absolute).
# When set, only these files are used and icon-folder is ignored.
icon-files=
```

Notes:
- 64x64 PNG icons are required by Velocity; larger images will be skipped.
- If `icon-files` is left empty, every `.png` in `icon-folder` is used.
- Relative paths resolve from the Velocity root (one level above `plugins`).
- If no icons load, the default server icon is used and a warning is logged.

## Usage
- Place your PNG icons in the configured folder (or list them in `icon-files`).
- Each time the server list refreshes, a random icon is served.
- Hot reload: changing files in the folder updates the pool on next startup; dynamic reloading is not implemented.
