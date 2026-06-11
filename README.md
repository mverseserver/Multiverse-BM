# Multiverse Blackmarket Plugin

## Cara Build
Requires: Java 21+, Maven 3.6+

```bash
mvn clean package
```

JAR output: `target/MultiverseBlackmarket-1.0.0.jar`

## Dependencies (Auto-downloaded via Maven)
- Paper API 1.21.1
- Vault API
- PlayerPoints 3.x
- PlaceholderAPI 2.11.6

## Setup
1. Build atau download JAR
2. Taruh di folder `plugins/`
3. Install Vault + economy plugin (EssentialsX/CMI dll)
4. Install PlayerPoints (opsional)
5. Restart server

## Commands
| Command | Permission | Deskripsi |
|---------|-----------|-----------|
| /bm atau /blackmarket | multiversebm.use | Buka Black Market |
| /bmadmin reload | multiversebm.admin | Reload config |
| /bmadmin restock | multiversebm.admin | Force restock |
| /bmadmin opengui <player> | multiversebm.admin | Buka market untuk player |

## Fitur Utama
- ✅ Support Vault (uang) & PlayerPoints (poin) per item
- ✅ 5 Tier Rarity: Common, Uncommon, Rare, Epic, Legendary
- ✅ Harga dinamis berubah setiap restock (±30% by default)
- ✅ Random item pool setiap restock berdasarkan weight rarity
- ✅ Broadcast gradient hitam keren saat restock
- ✅ Title animation saat restock
- ✅ Sound effect saat restock & purchase
- ✅ PlaceholderAPI support (%mbm_time_remaining%)
- ✅ Tab completion pada commands
- ✅ Configurable semuanya via YAML

## File Konfigurasi
- `config.yml` - Settings utama, economy, broadcast
- `market.yml` - Rarity definitions, GUI layout, reward pool
- `messages.yml` - Semua pesan & warna
