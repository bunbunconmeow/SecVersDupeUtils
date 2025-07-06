package org.secverse.SecVerseDupeUtils;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.secverse.SecVerseDupeUtils.ItemFrame.ItemFrameDupe;
import org.secverse.SecVerseDupeUtils.Donkey.DonkeyShulkerDupe;
import org.secverse.SecVerseDupeUtils.GrindStone.GrindStoneDupe;

public final class SecVersDupe extends JavaPlugin {
    private ItemFrameDupe frameDupe;
    private DonkeyShulkerDupe donkeyDupe;
    private  GrindStoneDupe grindstoneDupe;


    @Override
    public void onEnable() {
        saveDefaultConfig();

        frameDupe = new ItemFrameDupe(this);
        donkeyDupe = new DonkeyShulkerDupe(this);
        grindstoneDupe = new GrindStoneDupe(this);
        getServer().getPluginManager().registerEvents(frameDupe.new FrameAll(), this);
        getServer().getPluginManager().registerEvents(frameDupe.new FrameSpecific(), this);
        getServer().getPluginManager().registerEvents(donkeyDupe, this);
        getServer().getPluginManager().registerEvents(grindstoneDupe, this);
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("dupeutls reload")) {
            if (!sender.hasPermission("dupeutils.reload")) {
                sender.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

            this.reloadConfig();
            frameDupe.reload();
            donkeyDupe.reload();
            grindstoneDupe.reload();

            sender.sendMessage("§SecVers Dupe Utils config reloaded.");
            return true;
        }
        return false;
    }
}
