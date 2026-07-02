# Torchanger

Torchanger is a desktop application for Ubuntu that starts, monitors, and manages multiple Tor connection profiles from a single window.

## Features

- Run several Tor profiles in parallel.
- Start direct Tor, vanilla bridge, obfs4, Snowflake, and WebTunnel profiles.
- Expose local SOCKS5 and HTTP proxy endpoints for each profile.
- Monitor status, connection progress, ping, and average latency.
- Review separate logs for every profile.
- Minimize to tray and restore active sessions quickly.

## Screenshots

### Main Window

![Torchanger main window](!files/screenshots/torchanger000.png)

### Compact View

![Torchanger compact view](!files/screenshots/torchanger001.png)

## Debian Package

The repository includes a `.deb` packaging workflow based on `jpackage` with additional Debian metadata for Ubuntu-compatible repositories.

Build locally:

```bash
./packaging/deb/build-deb.sh
```

The package will be created in `packaging/deb/dist/`.

## Runtime Dependencies

Torchanger does not bundle the Tor network tools themselves. Install these on the target Ubuntu system:

- `tor`
- `curl`
- `obfs4proxy`
- `snowflake-client`
- `lyrebird`

## License

This project is released under the [Unlicense](UNLICENSE).
