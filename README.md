# GeoMOTD

Ultra-fast, dynamic GeoIP multi-language MOTD system for Minecraft servers and proxies.

[![Build Status](https://img.shields.io/badge/Build%20Status-passing-brightgreen?style=flat-square)](https://github.com/Axxtrial/GeoMotd/actions)
[![License](https://img.shields.io/badge/License-BSL%201.1-blue?style=flat-square)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21%2B-informational?style=flat-square)](https://adoptium.net)

GeoMOTD inspects player ping connections and resolves their country of origin using an offline MaxMind database. Messages are retrieved from memory via Caffeine Cache in under 0.05 milliseconds, ensuring zero impact on server tick rates.

---

## Features

- **Dynamic Geo-Routing:** Displays native-language MOTDs based on the player's detected country.
- **In-Memory Caching:** Caffeine CHM cache maintains high-throughput ping resolution under 0.05ms.
- **Offline Resolution:** Local database lookups without external HTTP requests during pings.
- **MiniMessage & Legacy Formatting:** Native Adventure RGB hex gradients and standard color codes.
- **Multiplatform Support:** Universal core with dedicated adapters for Paper, Folia, Velocity, and BungeeCord.
- **Hot-Reloading:** In-game configuration and database reload without restarting proxies or servers.
- **Telemetry:** Anonymous bStats integration (toggleable via configuration).

---

## Supported Platforms

| Platform | Supported Versions | Notes |
| :--- | :--- | :--- |
| **Paper / Purpur** | 1.20 - 1.26+ | Synchronous and asynchronous ping events |
| **Folia** | 1.20 - 1.26+ | Native regionized multi-threading (`folia-supported: true`) |
| **Velocity** | 3.3.0 - 3.4.0+ | Native Adventure MiniMessage support |
| **BungeeCord / Waterfall** | 1.20+ | Component-to-JSON serialization bridge |

---

## Installation

1. Download the required JAR for your platform from the [Releases](https://github.com/Axxtrial/GeoMotd/releases) page:
   - `GeoMOTD-Paper-1.0.0.jar`
   - `GeoMOTD-Velocity-1.0.0.jar`
   - `GeoMOTD-Bungee-1.0.0.jar`
2. Place the JAR into your server or proxy `plugins` directory and restart.
3. Edit `plugins/GeoMOTD/config.yml` to define your localized messages.
4. Execute `/geomotd reload` to apply changes.

---

## Configuration

```yaml
default-language: "en"
local-language: "en"
check-updates: true
metrics: true

motds:
  en:
    - line1: "<gradient:#00FFAA:#00AAFF><bold>Minecraft Server</bold></gradient> <dark_gray>●</dark_gray> <gray>1.20 - 1.26</gray>"
      line2: "<yellow>Welcome to our multiplayer community!</yellow>"

  es:
    - line1: "<gradient:#00FFAA:#00AAFF><bold>Servidor de Minecraft</bold></gradient> <dark_gray>●</dark_gray> <gray>1.20 - 1.26</gray>"
      line2: "<yellow>¡Bienvenido a nuestra comunidad multijugador!</yellow>"

  pt:
    - line1: "<gradient:#00FFAA:#00AAFF><bold>Servidor de Minecraft</bold></gradient> <dark_gray>●</dark_gray> <gray>1.20 - 1.26</gray>"
      line2: "<yellow>Bem-vindo à nossa comunidade multijugador!</yellow>"
```

---

## Commands & Permissions

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/geomotd reload` | `geomotd.admin` | Reloads configurations, language maps, and database |
| `/geomotd status` | `geomotd.admin` | Displays cache metrics, database state, and latency |
| `/geomotd help` | `geomotd.admin` | Displays command usage |

---

## Placeholders (PlaceholderAPI)

When [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) is installed on Paper/Spigot, the following placeholders are available:

| Placeholder | Example Output | Description |
| :--- | :--- | :--- |
| `%geomotd_country%` | `Spain`, `Mexico`, `United States` | Player's full country name |
| `%geomotd_country_code%` | `ES`, `MX`, `US` | Player's ISO-2 country code |
| `%geomotd_language%` | `es`, `en`, `pt` | Player's resolved language |
| `%geomotd_cache_size%` | `142` | Total cached IP addresses in RAM |
| `%geomotd_cache_hit_rate%` | `99.85%` | RAM cache hit rate percentage |
| `%geomotd_countries_count%` | `223` | Total mapped countries in database |

---

## Building from Source

Requires JDK 21 or higher and Maven:

```bash
git clone https://github.com/Axxtrial/GeoMotd.git
cd GeoMotd
mvn clean package
```

Compiled JAR files are output to `geomotd-*/target/`.

---

## License

This project is licensed under the **Business Source License 1.1 (BSL 1.1)**.

- Free for personal, educational, hobby, and non-commercial community usage.
- Commercial usage by registered corporations and commercial networks requires a commercial license.
- Automatically converts to the **MIT License** on **November 6, 2028**.

See [LICENSE](LICENSE) for full legal text.
