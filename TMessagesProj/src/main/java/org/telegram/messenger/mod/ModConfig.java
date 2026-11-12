package org.telegram.messenger.mod;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.telegram.messenger.ApplicationLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Глобальные настройки мода.
 *
 * Всё, что здесь хранится, — ЛОКАЛЬНОЕ и косметическое. Ничего отсюда не влияет
 * на реальные платежи, подписки или серверные данные Telegram.
 */
public class ModConfig {

    private static final String PREFS = "mod_config";

    private static ModConfig instance;

    public static synchronized ModConfig get() {
        if (instance == null) {
            instance = new ModConfig();
        }
        return instance;
    }

    public static void init(Context context) {
        get();
    }

    private final SharedPreferences prefs;
    private final List<Runnable> listeners = new ArrayList<>();

    private boolean ghostRead;
    private boolean ghostTyping;
    private boolean ghostOnline;

    private boolean fakePremium;

    private String ownerUsername;
    private String ownerBadge;

    private boolean pluginsEnabled;

    private long fakeStars;

    private ModConfig() {
        Context ctx = ApplicationLoader.applicationContext;
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        ghostRead = prefs.getBoolean("ghost_read", false);
        ghostTyping = prefs.getBoolean("ghost_typing", false);
        ghostOnline = prefs.getBoolean("ghost_online", false);
        fakePremium = prefs.getBoolean("fake_premium", false);
        ownerUsername = prefs.getString("owner_username", "paois");
        ownerBadge = prefs.getString("owner_badge", "perry");
        pluginsEnabled = prefs.getBoolean("plugins_enabled", true);
        fakeStars = prefs.getLong("fake_stars", 0L);
    }

    public void addListener(Runnable r) {
        if (r != null && !listeners.contains(r)) {
            listeners.add(r);
        }
    }

    public void removeListener(Runnable r) {
        listeners.remove(r);
    }

    private void notifyChanged() {
        for (Runnable r : new ArrayList<>(listeners)) {
            try {
                r.run();
            } catch (Throwable ignore) {
            }
        }
    }

    // ---- Ghost mode ----

    public boolean isGhostRead() {
        return ghostRead;
    }

    public void setGhostRead(boolean value) {
        ghostRead = value;
        prefs.edit().putBoolean("ghost_read", value).apply();
        notifyChanged();
    }

    public boolean isGhostTyping() {
        return ghostTyping;
    }

    public void setGhostTyping(boolean value) {
        ghostTyping = value;
        prefs.edit().putBoolean("ghost_typing", value).apply();
        notifyChanged();
    }

    public boolean isGhostOnline() {
        return ghostOnline;
    }

    public void setGhostOnline(boolean value) {
        ghostOnline = value;
        prefs.edit().putBoolean("ghost_online", value).apply();
        notifyChanged();
    }

    // ---- Fake premium (cosmetic only) ----

    public boolean isFakePremium() {
        return fakePremium;
    }

    public void setFakePremium(boolean value) {
        fakePremium = value;
        prefs.edit().putBoolean("fake_premium", value).apply();
        notifyChanged();
    }

    // ---- Owner ----

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String value) {
        ownerUsername = value == null ? "" : value.trim();
        prefs.edit().putString("owner_username", ownerUsername).apply();
        notifyChanged();
    }

    public String getOwnerBadge() {
        return ownerBadge;
    }

    /** Только эмодзи значка (без подписи) — для отображения рядом с именем. */
    public String getOwnerBadgeEmoji() {
        if (ownerBadge == null || ownerBadge.isEmpty()) {
            return "";
        }
        int sp = ownerBadge.indexOf(' ');
        return sp > 0 ? ownerBadge.substring(0, sp) : ownerBadge;
    }

    public void setOwnerBadge(String value) {
        ownerBadge = value == null ? "perry" : value;
        prefs.edit().putString("owner_badge", ownerBadge).apply();
        notifyChanged();
    }

    /**
     * Проверка владельца по юзернейму.
     *
     * Сравнение идёт со значением, жёстко сохранённым в настройках/коде.
     * Даже пользователь с тем же юзернеймом не получит права, пока владелец
     * не подтвердит его (сменив значение здесь).
     */
    public boolean isOwner(String username) {
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(ownerUsername)) {
            return false;
        }
        String a = username.startsWith("@") ? username.substring(1) : username;
        String b = ownerUsername.startsWith("@") ? ownerUsername.substring(1) : ownerUsername;
        return a.equalsIgnoreCase(b);
    }

    // ---- Админы (выдаёт владелец) ----

    public java.util.Set<String> getAdmins() {
        return new java.util.HashSet<>(prefs.getStringSet("admins", new java.util.HashSet<String>()));
    }

    public void addAdmin(String username) {
        if (TextUtils.isEmpty(username)) {
            return;
        }
        String a = username.startsWith("@") ? username.substring(1) : username;
        java.util.Set<String> set = getAdmins();
        set.add(a.toLowerCase());
        prefs.edit().putStringSet("admins", set).apply();
        notifyChanged();
    }

    public void removeAdmin(String username) {
        if (TextUtils.isEmpty(username)) {
            return;
        }
        String a = username.startsWith("@") ? username.substring(1) : username;
        java.util.Set<String> set = getAdmins();
        set.remove(a.toLowerCase());
        prefs.edit().putStringSet("admins", set).apply();
        notifyChanged();
    }

    public boolean isAdmin(String username) {
        if (isOwner(username)) {
            return true;
        }
        if (TextUtils.isEmpty(username)) {
            return false;
        }
        String a = username.startsWith("@") ? username.substring(1) : username;
        return getAdmins().contains(a.toLowerCase());
    }

    // ---- Plugins ----

    public boolean isPluginsEnabled() {
        return pluginsEnabled;
    }

    public void setPluginsEnabled(boolean value) {
        pluginsEnabled = value;
        prefs.edit().putBoolean("plugins_enabled", value).apply();
        notifyChanged();
    }

    // ---- Ретранслятор фейк-подарков ----

    public String getRelayUrl() {
        return prefs.getString("relay_url", "");
    }

    public void setRelayUrl(String value) {
        prefs.edit().putString("relay_url", value == null ? "" : value.trim()).apply();
        notifyChanged();
    }

    public String getRelaySecret() {
        return prefs.getString("relay_secret", "");
    }

    public void setRelaySecret(String value) {
        prefs.edit().putString("relay_secret", value == null ? "" : value.trim()).apply();
        notifyChanged();
    }

    // ---- Fake stars (внутренняя косметическая валюта) ----

    public long getFakeStars() {
        return fakeStars;
    }
    public void setFakeStars(long value) {
        fakeStars = Math.max(0, value);
        prefs.edit().putLong("fake_stars", fakeStars).apply();
        notifyChanged();
    }

    public void addFakeStars(long delta) {
        setFakeStars(fakeStars + delta);
    }

    /** Внешний вид звёзд (косметика). */
    public String getStarStyle() {
        return prefs.getString("star_style", "⭐");
    }

    public void setStarStyle(String value) {
        prefs.edit().putString("star_style", value == null || value.isEmpty() ? "⭐" : value).apply();
        notifyChanged();
    }
}
