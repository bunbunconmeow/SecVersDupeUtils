SecVersDupeUtils
================

SecVersDupeUtils is a powerful Paper plugin that enables advanced duplication mechanics for Item Frames, Glow Item Frames, Donkeys, and Grindstones, including configurable timing-based dupes and blocked item handling.

Features
--------

- Item Frame Dupe – duplicate items from normal frames
- Glow Item Frame Dupe – duplicate items from glow frames
- Donkey Shulker Dupe – dupe shulker contents based on timing kill exploit
- Grindstone Dupe – experimental dupe exploit
- Blacklist System – prevents duplication of specific custom items
- Timing-based dupes for realistic exploit-like behavior
- Reload Command to apply config changes live

Configuration
-------------

Default config:
```yml
# Telemetry
# We will collect only the current Version, OS, Server Name.
telemetry:
  enabled: true                 # set to false to opt-out
  send_interval_seconds: 3600   # optional for repeated sends

# Check for Update
checkUpdate: true

FrameDupe:
  Enabled: true
  Probability-percentage: 100
  Multiplier: 1

GLOW_FrameDupe:
  Enabled: true
  Probability-percentage: 100
  Multiplier: 1

Settings:
  EnableItemCheck: true

OtherDupes:
  GrindStone:
    Enabled: false
    MinTiming: 1200   # Minimum in ms
    MaxTiming: 2200   # Maximum in ms
    dropNaturally: true
    addToInventory: false
  DonkeyDupe:
    Enabled: false
    MinTiming: 100   # Minimum in ms
    MaxTiming: 5000   # Maximum in ms
  CrafterDupe:
    Enabled: false
    MinTiming: 100     # Minimum in ms
    MaxTiming: 1000    # Maximum in ms
    destroyCrafter: true    # Destroys the Crafter after Dupe
    dropOriginals: false     # Golden Apple, Netherite Block, Torches drop


ItemBlacklist:
  - Namespace: "exampleplugin"
    Key: "specialtype"
    Names:
      - "test_item"
      - "extra_item"
```
Config Explanation:

FrameDupe.Enabled – Enable normal item frame dupe (default: true)

FrameDupe.Probability-percentage – Chance (%) of dupe per break (default: 100)

FrameDupe.Multiplier – Number of duplicated items dropped (default: 1)

GLOW_FrameDupe.Enabled – Enable glow item frame dupe (default: true)

Settings.EnableItemCheck – Enable blocked item check system (default: true)

OtherDupes.GrindStone – Enable Grindstone dupe (default: false)

OtherDupes.DonkeyDupe.Enabled – Enable Donkey dupe (default: false)

OtherDupes.DonkeyDupe.MinTiming – Minimum timing window in ms (default: 100)

OtherDupes.DonkeyDupe.MaxTiming – Maximum timing window in ms (default: 800)

ItemBlacklist – List of blocked items by NamespacedKey & Names

Commands
--------

/duperealod – Reloads the plugin config if config.yml was changed manually.(Permission: dupeutils.reload)
/configdupes - Opens a GUI where you can configure the plugin.

Permissions
-----------

dupeutils.reload – Allows reloading plugin configuration

Maven Integration
-----------------

Add SecVersDupeUtils as a dependency:
```xml
<repositories>
    <repository>
        <id>your-repo</id>
        <url>https://repo.yourdomain.com/repository/maven-public/</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>org.secverse</groupId>
        <artifactId>SecVersDupeUtils</artifactId>
        <version>1.0.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

Replace the repository with your actual Maven repository if published privately.

Setup
-----

1. Place the SecVersDupeUtils.jar in your /plugins folder
2. Start or reload your server
3. Configure with /configdupes as needed


Disclaimer
----------

This plugin is designed for educational and exploit testing purposes. Duplication mechanics may heavily affect server economy and gameplay balance. Use responsibly and at your own risk.

License
-------

BSD-3 License © SecVerse Development
