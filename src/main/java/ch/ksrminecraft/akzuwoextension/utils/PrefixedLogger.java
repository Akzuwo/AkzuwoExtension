package ch.ksrminecraft.akzuwoextension.utils;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;

/**
 * Logger that prefixes all messages with the plugin name and colors warnings yellow.
 */
public class PrefixedLogger extends Logger {
    private static final String PREFIX = "[AkzuwoExtension] ";
    private final Logger base;

    public PrefixedLogger(Logger base) {
        super(base.getName(), null);
        this.base = base;
        setParent(base.getParent());
        setLevel(base.getLevel());
    }

    @Override
    public void info(String msg) {
        base.info(PREFIX + msg);
    }

    @Override
    public void warning(String msg) {
        base.warning(ChatColor.YELLOW + PREFIX + msg + ChatColor.RESET);
    }

    @Override
    public void severe(String msg) {
        base.severe(PREFIX + msg);
    }

    @Override
    public void log(Level level, String msg) {
        if (level == Level.WARNING) {
            warning(msg);
        } else if (level == Level.INFO) {
            info(msg);
        } else if (level == Level.SEVERE) {
            severe(msg);
        } else {
            base.log(level, PREFIX + msg);
        }
    }
}

