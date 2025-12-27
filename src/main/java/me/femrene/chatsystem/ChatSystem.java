package me.femrene.chatsystem;

import me.femrene.chatsystem.commands.PluginReload;
import me.femrene.chatsystem.listeners.onChat;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

public final class ChatSystem extends JavaPlugin {

    private static ChatSystem instance;
    public static String id = "1";

    @Override
    public void onEnable() {
        instance = this;
        setConf();

        // Check for updates if enabled
        if (getBooleanFromConf("autoUpdate")) {
            getLogger().info("Checking for updates...");
            //checkForUpdates(); // TODO: need to add in future
        }

        // Listener
        Bukkit.getPluginManager().registerEvents(new onChat(), this);
        // Commands
        getCommand("creload").setExecutor(new PluginReload());
    }

    /**
     * Checks for updates and downloads the latest version if available
     */
    private void checkForUpdates() {
        try {
            // Create URL for the latest version
            String updateUrl = "https://femrene.dev/plugins/" + id + "/latest/download";
            URL url = new URL(updateUrl);

            // Open connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.connect();

            // Check if there's a redirect (which means there's a new version)
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || 
                responseCode == HttpURLConnection.HTTP_MOVED_PERM || 
                responseCode == HttpURLConnection.HTTP_SEE_OTHER) {

                // Get the redirect URL
                String newUrl = "https://femrene.dev"+connection.getHeaderField("Location");
                getLogger().info("New version found at: " + newUrl);

                // Download and install the update
                downloadAndInstallUpdate(newUrl);
            } else {
                getLogger().info("No updates found.");
            }

            connection.disconnect();
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to check for updates", e);
        }
    }

    /**
     * Downloads and installs the latest version of the plugin
     * 
     * @param downloadUrl URL to download the latest version from
     */
    private void downloadAndInstallUpdate(String downloadUrl) {
        try {
            // Get the plugin file
            File pluginFile = getPluginFile();
            if (pluginFile == null) {
                getLogger().warning("Could not locate the plugin file. Auto-update failed.");
                return;
            }

            // Create a temporary file for the download
            Path tempFile = Files.createTempFile("ChatSystem-update-", ".jar");

            // Download the new version
            getLogger().info("Downloading update...");
            URL url = new URL(downloadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(tempFile.toFile())) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            // Rename the current plugin file to .old
            String oldFilePath = pluginFile.getAbsolutePath() + ".old";
            File oldFile = new File(oldFilePath);

            // Delete previous .old file if it exists
            if (oldFile.exists()) {
                oldFile.delete();
            }

            getLogger().info("Installing update...");

            // Schedule the file replacement for when the server stops
            // This is necessary because we can't replace the plugin file while it's loaded
            Bukkit.getScheduler().runTaskLater(this, () -> {
                try {
                    // Rename current plugin to .old
                    Files.move(pluginFile.toPath(), oldFile.toPath());

                    // Move the new version to the plugins directory
                    Files.move(tempFile, pluginFile.toPath());

                    getLogger().info("Update installed successfully! Restart the server to apply the update.");
                } catch (IOException e) {
                    getLogger().log(Level.SEVERE, "Failed to install update", e);
                }
            }, 1L); // Run after 1 tick

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to download and install update", e);
        }
    }

    /**
     * Gets the plugin's JAR file
     * 
     * @return The plugin's JAR file or null if it couldn't be found
     */
    private File getPluginFile() {
        try {
            // Get the location of the plugin JAR file
            Path pluginsDir = Paths.get("plugins");
            File pluginFile = new File(pluginsDir.toFile(), "ChatSystem.jar");

            // If the file doesn't exist with that name, try to find it by class location
            if (!pluginFile.exists()) {
                String jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
                // Convert URL path to file path
                jarPath = jarPath.replaceAll("%20", " ");
                if (jarPath.startsWith("/")) {
                    jarPath = jarPath.substring(1);
                }
                pluginFile = new File(jarPath);
            }

            return pluginFile;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to locate plugin file", e);
            return null;
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (getBooleanFromConf("autoUpdate")) {
            getLogger().info("Checking for updates...");
            checkForUpdates();
        }
    }

    public static ChatSystem getInstance() {
        return instance;
    }

    public static String getFromConf(String path) {
        FileConfiguration config = instance.getConfig();
        return config.getString(path);
    }

    public static boolean getBooleanFromConf(String path) {
        FileConfiguration config = instance.getConfig();
        return config.getBoolean(path);
    }

    private static void setConf() {
        instance.saveDefaultConfig();
        FileConfiguration config = instance.getConfig();
        if (!config.contains("arrow")) {
            config.set("arrow", "<#555555>»");
        }
        if (!config.contains("msg")) {
            config.set("msg", "%prefix %arrow <#AAAAAA>%player %suffix: <reset>%message");
        }
        if (!config.contains("mentionMessage")) {
            config.set("mentionMessage", "<#55FFFF>@%player<reset>");
        }
        if (!config.contains("useMetaKeyAsPrefix")) {
            config.set("useMetaKeyAsPrefix", false);
        }
        if (!config.contains("metaPrefixString")) {
            config.set("metaPrefixString", "META-KEY");
        }
        if (!config.contains("useMetaKeyAsSuffix")) {
            config.set("useMetaKeyAsSuffix", false);
        }
        if (!config.contains("metaSuffixString")) {
            config.set("metaSuffixString", "META-KEY");
        }
        if (!config.contains("pingSound")) {
            config.set("pingSound", true);
        }
        if (!config.contains("autoUpdate")) {
            config.set("autoUpdate", true);
        }
        instance.saveConfig();
    }
}