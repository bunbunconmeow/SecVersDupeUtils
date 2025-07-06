package org.secverse.SecVerseDupeUtils.helper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class EventsKeys {
    private final Plugin plugin;
    private final List<BlacklistEntry> cachedBlacklist;

    public EventsKeys(Plugin plugin) {
        this.plugin = plugin;
        this.cachedBlacklist = loadBlacklist();
    }

    private List<BlacklistEntry> loadBlacklist() {
        List<Map<?, ?>> blacklists = plugin.getConfig().getMapList("ItemBlacklist");
        List<BlacklistEntry> entries = new ArrayList<>();

        for (Map<?, ?> entry : blacklists) {
            String namespace = (String) entry.get("Namespace");
            String key = (String) entry.get("Key");
            List<String> names = (List<String>) entry.get("Names");

            if (namespace == null || key == null || names == null) continue;

            NamespacedKey nk = new NamespacedKey(namespace, key);
            entries.add(new BlacklistEntry(nk, names));
        }
        return entries;
    }

    public boolean isBlockedItem(ItemStack item) {
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        for (BlacklistEntry entry : cachedBlacklist) {
            if (container.has(entry.key, PersistentDataType.STRING)) {
                String type = container.get(entry.key, PersistentDataType.STRING);
                if (type != null && entry.names.stream().anyMatch(s -> s.equalsIgnoreCase(type))) {
                    return true;
                }
            }
        }
        return false;
    }

    public void reload() {
        cachedBlacklist.clear();
        cachedBlacklist.addAll(loadBlacklist());
    }

    private static class BlacklistEntry {
        public final NamespacedKey key;
        public final List<String> names;

        public BlacklistEntry(NamespacedKey key, List<String> names) {
            this.key = key;
            this.names = names;
        }
    }
}