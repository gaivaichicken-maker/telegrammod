package org.telegram.messenger.mod;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Каталог фейк-подарков и «полученные» фейк-подарки.
 *
 * Подарки полностью косметические: они не отправляются в Telegram и видны
 * только пользователям этого мода. В уведомлении получателю в конце
 * обязательно указывается, что подарок фейковый.
 */
public class ModGifts {

    public static class Gift {
        public final String id;
        public final String emoji;
        public final String name;
        public final long starPrice;

        public Gift(String id, String emoji, String name, long starPrice) {
            this.id = id;
            this.emoji = emoji;
            this.name = name;
            this.starPrice = starPrice;
        }
    }

    /** Пометка, которая обязательно добавляется к любому фейк-подарку. */
    public static final String FAKE_MARK = "фейк";

    private static ModGifts instance;

    public static synchronized ModGifts get() {
        if (instance == null) {
            instance = new ModGifts();
        }
        return instance;
    }

    private final SharedPreferences prefs;

    private ModGifts() {
        Context ctx = ApplicationLoader.applicationContext;
        prefs = ctx.getSharedPreferences("mod_gifts", Context.MODE_PRIVATE);
    }

    public List<Gift> getCatalog() {
        List<Gift> list = new ArrayList<>();
        list.add(new Gift("bear", "🧸", "Мишка", 10));
        list.add(new Gift("rose", "🌹", "Роза", 15));
        list.add(new Gift("cake", "🎂", "Торт", 20));
        list.add(new Gift("rocket", "🚀", "Ракета", 50));
        list.add(new Gift("crown", "👑", "Корона", 100));
        list.add(new Gift("perry", "🦫", "Перри-утконос", 0));
        return list;
    }

    public Gift find(String id) {
        for (Gift g : getCatalog()) {
            if (g.id.equals(id)) {
                return g;
            }
        }
        return null;
    }

    /** «Взять» подарок: списывает фейк-звёзды и кладёт подарок в инвентарь. */
    public boolean take(String id) {
        Gift g = find(id);
        if (g == null) {
            return false;
        }
        if (g.starPrice > 0 && ModConfig.get().getFakeStars() < g.starPrice) {
            return false;
        }
        if (g.starPrice > 0) {
            ModConfig.get().addFakeStars(-g.starPrice);
        }
        Set<String> owned = new HashSet<>(prefs.getStringSet("owned", new HashSet<String>()));
        owned.add(id);
        prefs.edit().putStringSet("owned", owned).apply();
        return true;
    }

    public Set<String> getOwned() {
        return new HashSet<>(prefs.getStringSet("owned", new HashSet<String>()));
    }

    /** Текст уведомления получателю. Всегда помечен как фейк. */
    public String buildReceivedText(String giftName) {
        return "Вам передали уникальный подарок: " + giftName + " (" + FAKE_MARK + ")";
    }
}
