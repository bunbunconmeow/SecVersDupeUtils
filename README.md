📦 SecVers Dupe Utils

A comprehensive and configurable dupe management plugin for Minecraft servers

Paper 1.21+ | [BSD-3 License](https://secvers.org/info/license) | Version 2.1

================================================================================

🌟 FEATURES

6+ Dupe Methods Available:
- Item Frame Dupe - Duplicate items using item frames
- Glow Item Frame Dupe - Enhanced duplication with glow frames
- Donkey Dupe - Kill the Donkey in the given time Frame to dupe the items
- Grindstone Dupe - Grindstone-based item duplication
- Crafter Dupe - Crafter block duplication
- Death Dupe - Duplicate items on death
- ...and more coming soon!


Advanced Configuration System:
- 🎲 Probability System - Set success rates for each dupe method
- ⏱️ Timing Controls - Adjust delay windows for dupes
- 🔧 Per-Dupe Settings - Independent configuration for each method
- 📊 Real-time Adjustments - Changes apply without server restart

Professional Features:
✅ Permission-Based UI Access - Fine-grained control over who can use what

🔄 Live Reload - Update configs without restarting

📝 Telemetry System - Optional usage statistics

🔔 Update Checker - Stay informed about new versions

================================================================================

📥 DOWNLOAD & SUPPORT

Official Download:
https://secvers.org/plugins/dupe-utility

Support Development:
https://secvers.org/info/donate

Dupe FAQ:
https://secvers.org/plugins/dupe-utility

================================================================================

🎮 COMMANDS

/dupe\
Description: Show help menu\
Permission: dupeutils.command

/dupe reload\
Description: Reload configuration\
Permission: dupeutils.reload

/dupe config\
Description: Open configuration GUI\
Permission: dupeutils.configdupes

================================================================================

🔐 PERMISSIONS

- dupeutils.command       - Base permission to use /dupe
- dupeutils.reload        - Permission to reload configs
- dupeutils.configdupes   - Permission to open config GUI

================================================================================

⚙️ CONFIGURATION EXAMPLE

```yaml
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
  Ranks:
    vip:
      Permission: "dupeutils.framedupe.vip"
      Multiplier: 2
      Probability-percentage: 25
    premium:
      Permission: "dupeutils.framedupe.premium"
      Multiplier: 3
      Probability-percentage: 50
    admin:
      Permission: "dupeutils.framedupe.admin"
      Multiplier: 5
      Probability-percentage: 100

GLOW_FrameDupe:
  Enabled: true
  Probability-percentage: 100
  Multiplier: 1
  Ranks:
    vip:
      Permission: "dupeutils.framedupe.vip"
      Multiplier: 2
      Probability-percentage: 25
    premium:
      Permission: "dupeutils.framedupe.premium"
      Multiplier: 3
      Probability-percentage: 50
    admin:
      Permission: "dupeutils.framedupe.admin"
      Multiplier: 5
      Probability-percentage: 100

Settings:
  EnableItemCheck: true
  DisableIllegalItem: true
  # Detailed illegal item detection settings
  IllegalItemDetection:
    # Check for items with Unbreakable tag
    CheckUnbreakable: true
    # Check for stacked items beyond normal stack size
    CheckOverstacked: true
    # Check for enchantments beyond vanilla limits
    CheckIllegalEnchantments: true
    # Check for items not obtainable in survival
    CheckUnobtainableItems: true
    # Allow custom names and lore (for plugins like EssentialsX, etc.)
    AllowCustomNames: true
    AllowCustomLore: true
    # Action to take on illegal items
    # Options: REMOVE, RESET, LOG_ONLY
    Action: REMOVE
    # Automatically fix enchantments instead of removing
    AutoFixEnchantments: true
    # Log illegal items to console
    LogIllegalItems: true
    # Whitelist for specific items that should bypass checks
    Whitelist:
      - "BARRIER"  # Example: Allow barriers even though not survival obtainable
      - "COMMAND_BLOCK"


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
  DropperDupe:
    Enabled: false
    Multiplier: 2
  DeathDupe:
    Enabled: false

ItemBlacklist:
  - Namespace: "minecraft"
    Key: "tnt"
    Names: []
  - Namespace: "minecraft"
    Key: "stone"
    Names: []
  - Namespace: "exampleplugin"
    Key: "specialtype"
    Names:
      - "test_item"
      - "extra_item"

Translation:
  # Enable translation system
  enabled: true
  # Auto-detect language based on player IP
  ipbased: false
  # Default language (fallback)
  default: "English"
  # Available translations (auto-updated on server start)
  translations:
    - "English"
    - "German"

```

# SecVerse Dupe Utils - Community Translations

## How to Create a Translation

1. Copy `english.json` as a template
2. Rename it to your language (e.g., `french.json`, `spanish.json`)
3. Edit the metadata section:
    - `language`: English name of the language
    - `nativeName`: Name in the native language (e.g., "Français", "Español")
    - `version`: Start with "1.0.0"
    - `author`: Your name or username
    - `description`: Brief description
    - `countryCodes`: List of ISO country codes (e.g., ["FR", "BE", "CH"])

4. Translate all strings in the `translations` section
5. Keep color codes (§a, §c, etc.) and placeholders ({0}, {1}, etc.) unchanged
6. Test your translation in-game
7. Share your translation file!

## Supported Color Codes
- §a = Green (success)
- §c = Red (error)
- §e = Yellow (info/value)
- §7 = Gray (description)
- §6 = Gold
- §r = Reset

## Placeholders
- {0}, {1}, etc. = Dynamic values that will be replaced
- Example: "Value set to {0}" → "Wert gesetzt auf {0}"

## File Format
- UTF-8 encoding
- JSON format
- File name: `languagename.json` (lowercase)

## Contributing
Share your translations on our Discord or GitHub!


================================================================================

🎮 GUI CONFIGURATION

Use /dupe config in-game for easy configuration:

• Left Click - Toggle dupe on/off
• Right Click - Open detailed settings
• Adjust Values - Click concrete blocks to increase/decrease
• Live Updates - Changes apply immediately

================================================================================

🤝 CREDITS & ACKNOWLEDGMENTS

Special Thanks:
[@HackThePyramids](https://github.com/HackThePyramids)
Massive help with development, code architecture, and feature
implementation. This plugin wouldn't be where it is without your
contributions! 🙏

Development:\
SecVers Team - Plugin development and maintenance\
Community - Bug reports and feature suggestions

================================================================================

📊 STATISTICS

• 6+ Dupe Methods - More than most premium plugins\
• 100% Free - No paywalls, no limitations\
• Active Development - Regular updates and new features\
• In-Game GUI - No need to edit config files

================================================================================

🐛 BUG REPORTS & FEATURE REQUESTS

Found a bug or have an idea?

1. Check existing issues on GitHub first
2. Create a new issue with:
    - Clear description
    - Steps to reproduce (for bugs)
    - Server version & plugin version
    - Any error messages

GitHub Issues: https://github.com/bunbunconmeow/SecVersDupeUtils/issues

================================================================================

📜 LICENSE

This project is licensed under the [BSD-3 License](https://secvers.org/info/license).


================================================================================

🔮 ROADMAP

Coming Soon:
Version 3 Goals
- [ ] Permissions based Dupes
  - [X] Item Frame Dupe
  - [ ] Grind Stone Dupe
  - [ ] Dropper Dupe
  - [ ] Donkey Dupe
  - [ ] Crafter Duper
  - [ ] Death Dupe
- [X] Multi-Language Support
  - [X] English 
  - [X] German
  - [ ] Russian
  - [ ] Chinese
  - [ ] Korean
  - [ ] Japanese
- [X] Anti-Illegal
  - [X] Whitelist Items
  - [ ] MBT Removals
  - [ ] Custom Replacements
- [ ] Particle Effects & Animations
  - [X] Crafter Dupe
  - [ ] Option for ItemFrame
  - [ ] Option for Grind Stone
- [ ] Economy Integration 


Under Consideration:
- [ ] Crafter Dupe Recipe System
- [ ] Particle Effects & Animations
- [ ] Backup & Rollback System
- [ ] Web Dashboard

================================================================================

⚠️ DISCLAIMER

This plugin is designed for anarchy, creative, or testing servers.\
Use at your own discretion. The developers are not responsible for any
damage to your server economy or gameplay balance.

================================================================================

💬 SUPPORT

Official Website: https://secvers.org <br/>
Plugin Page: https://secvers.org/plugins/dupe-utility <br/>
Donate: https://secvers.org/info/donate <br/>
Email: support@secvers.org

================================================================================

⭐ SHOW YOUR SUPPORT

If you like this plugin, please consider:\
⭐ Starring the GitHub repository\
🐛 Reporting bugs\
💡 Suggesting features\
📣 Sharing with others\
❤️ Donating to support development

================================================================================

Made with ❤️ by SecVers

Download: https://secvers.org/plugins/dupe-utility
Donate: https://secvers.org/info/donate

Want to contribute? Check out our Contributing Guidelines!

================================================================================
