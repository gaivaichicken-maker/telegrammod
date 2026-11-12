package org.telegram.messenger.mod;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Задания, за которые начисляются фейк-звёзды.
 *
 * Фейк-звёзды — внутренняя косметическая валюта. Их нельзя купить,
 * обменять на деньги или потратить на что-либо реальное.
 */
public class ModTasks {

    public static class Task {
        public final String id;
        public final String title;
        public final long reward;
        public boolean done;

        public Task(String id, String title, long reward) {
            this.id = id;
            this.title = title;
            this.reward = reward;
        }
    }

    private static ModTasks instance;

    public static synchronized ModTasks get() {
        if (instance == null) {
            instance = new ModTasks();
        }
        return instance;
    }

    private final SharedPreferences prefs;

    private ModTasks() {
        Context ctx = ApplicationLoader.applicationContext;
        prefs = ctx.getSharedPreferences("mod_tasks", Context.MODE_PRIVATE);
    }

    public List<Task> getTasks() {
        List<Task> list = new ArrayList<>();
        list.add(task("open_mod", "Открыть панель мода", 10));
        list.add(task("read_ghost", "Включить режим призрака", 20));
        list.add(task("send_fake_gift", "Отправить фейк-подарок", 30));
        list.add(task("check_owner", "Заглянуть в owner-панель", 15));
        list.add(task("daily", "Ежедневный вход", 5));
        return list;
    }

    private Task task(String id, String title, long reward) {
        Task t = new Task(id, title, reward);
        t.done = prefs.getBoolean("done_" + id, false);
        return t;
    }

    public boolean isDone(String id) {
        return prefs.getBoolean("done_" + id, false);
    }

    /** Засчитывает задание и начисляет награду, если оно ещё не выполнено. */
    public boolean complete(String id) {
        if (isDone(id)) {
            return false;
        }
        for (Task t : getTasks()) {
            if (t.id.equals(id)) {
                prefs.edit().putBoolean("done_" + id, true).apply();
                ModConfig.get().addFakeStars(t.reward);
                return true;
            }
        }
        return false;
    }
}
