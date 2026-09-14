package dev.meowcy.app;

import android.content.Context;
import android.content.SharedPreferences;

final class ConfigStore {
    private static final String PREFS = "meowcy";

    record Config(String host, int port, String username, String password) {}

    static void save(Context context, Config config) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString("host", config.host())
                .putInt("port", config.port())
                .putString("username", config.username())
                .putString("password", config.password())
                .apply();
    }

    static Config load(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return new Config(
                p.getString("host", ""),
                p.getInt("port", 1080),
                p.getString("username", ""),
                p.getString("password", "")
        );
    }

    private ConfigStore() {}
}
