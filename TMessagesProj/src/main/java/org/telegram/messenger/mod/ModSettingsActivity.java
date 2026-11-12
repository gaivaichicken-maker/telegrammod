package org.telegram.messenger.mod;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.util.List;
import java.util.Set;

/**
 * Экран настроек мода.
 *
 * Всё здесь — локальная косметика. Ничего не покупает, не даёт реальных прав
 * и не отправляет фейковые данные как настоящие.
 */
public class ModSettingsActivity extends Activity {

    private LinearLayout content;

    private int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ModConfig.init(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF17212B);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));
        scroll.addView(content, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);

        build();
    }

    private TextView title(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFFFFFFFF);
        tv.setTextSize(22);
        tv.setPadding(0, dp(8), 0, dp(16));
        return tv;
    }

    private TextView header(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFF6D7F8F);
        tv.setTextSize(14);
        tv.setPadding(0, dp(16), 0, dp(6));
        return tv;
    }

    private Switch toggle(String text, boolean checked, final ToggleListener listener) {
        Switch sw = new Switch(this);
        sw.setText(text);
        sw.setTextColor(0xFFFFFFFF);
        sw.setPadding(0, dp(6), 0, dp(6));
        sw.setChecked(checked);
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> listener.onChanged(isChecked));
        return sw;
    }

    private interface ToggleListener {
        void onChanged(boolean value);
    }

    private Button button(String text, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(6);
        b.setLayoutParams(lp);
        return b;
    }

    private void build() {
        content.removeAllViews();
        ModConfig cfg = ModConfig.get();

        content.addView(title("Мод"));

        // ---- Ghost mode ----
        content.addView(header("Режим призрака"));
        content.addView(toggle("Читать без «прочитано»", cfg.isGhostRead(), cfg::setGhostRead));
        content.addView(toggle("Не показывать «печатает…»", cfg.isGhostTyping(), cfg::setGhostTyping));
        content.addView(toggle("Скрывать статус «онлайн»", cfg.isGhostOnline(), cfg::setGhostOnline));

        // ---- Fake premium ----
        content.addView(header("Косметика"));
        content.addView(toggle("Фейк-Premium (только значок)", cfg.isFakePremium(), cfg::setFakePremium));
        content.addView(toggle("Плагины", cfg.isPluginsEnabled(), cfg::setPluginsEnabled));

        // ---- Fake stars ----
        content.addView(header("Фейк-звёзды"));
        TextView stars = new TextView(this);
        stars.setTextColor(0xFFFFD54F);
        stars.setTextSize(18);
        stars.setText("У тебя фейк-звёзд: " + cfg.getFakeStars() + " " + cfg.getStarStyle());
        content.addView(stars);
        content.addView(button("Сменить вид звёзд", v -> showStarStyle()));
        TextView note = new TextView(this);
        note.setTextColor(0xFF6D7F8F);
        note.setTextSize(12);
        note.setText("Только внутри мода. Не покупаются и не тратятся на реальное.");
        content.addView(note);
        content.addView(button("Задания", v -> showTasks()));
        content.addView(button("Фейк-подарки", v -> showGifts()));
        content.addView(button("Отправить фейк-подарок", v -> showSendGift()));
        content.addView(button("Проверить входящие (фейк)", v -> showCheckIncoming()));

        // ---- Owner ----
        content.addView(header("Владелец и админы"));
        boolean isOwner = cfg.isOwner(currentUsername());
        boolean isAdmin = cfg.isAdmin(currentUsername());
        TextView ownerInfo = new TextView(this);
        ownerInfo.setTextColor(0xFF8E7CFF);
        ownerInfo.setTextSize(14);
        String role = isOwner ? "владелец" : (isAdmin ? "админ" : "гость");
        ownerInfo.setText("Владелец: @" + cfg.getOwnerUsername() + "  •  ты: " + role);
        content.addView(ownerInfo);
        if (isAdmin) {
            content.addView(button("Выдать фейк-звёзды", v -> showGrantStars()));
            content.addView(button("Настроить ретранслятор", v -> showRelayConfig()));
        }
        if (isOwner) {
            content.addView(button("Управлять админами", v -> showAdmins()));
            content.addView(button("Сменить владельца (юзернейм)", v -> showChangeOwner()));
            content.addView(button("Сменить значок владельца", v -> showChangeBadge()));
        }
        if (!isAdmin) {
            TextView locked = new TextView(this);
            locked.setTextColor(0xFF6D7F8F);
            locked.setTextSize(12);
            locked.setText("Панель доступна только владельцу и админам.");
            content.addView(locked);
        }
    }

    private String currentUsername() {
        try {
            TLRPC.User me = UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser();
            return me != null && me.username != null ? me.username : "";
        } catch (Throwable t) {
            return "";
        }
    }

    private void showTasks() {
        List<ModTasks.Task> tasks = ModTasks.get().getTasks();
        String[] items = new String[tasks.size()];
        for (int i = 0; i < tasks.size(); i++) {
            ModTasks.Task t = tasks.get(i);
            items[i] = (t.done ? "✔ " : "• ") + t.title + "  (+" + t.reward + ")";
        }
        new AlertDialog.Builder(this)
                .setTitle("Задания")
                .setItems(items, (dialog, which) -> {
                    ModTasks.Task t = tasks.get(which);
                    if (ModTasks.get().complete(t.id)) {
                        toast("Начислено " + t.reward + " фейк-звёзд");
                    } else {
                        toast("Уже выполнено");
                    }
                    build();
                })
                .setNegativeButton("Закрыть", null)
                .show();
    }

    private void showGifts() {
        List<ModGifts.Gift> gifts = ModGifts.get().getCatalog();
        Set<String> owned = ModGifts.get().getOwned();
        String[] items = new String[gifts.size()];
        for (int i = 0; i < gifts.size(); i++) {
            ModGifts.Gift g = gifts.get(i);
            String price = g.starPrice > 0 ? (g.starPrice + " ⭐") : "бесплатно";
            items[i] = g.emoji + " " + g.name + " — " + price + (owned.contains(g.id) ? "  (есть)" : "");
        }
        new AlertDialog.Builder(this)
                .setTitle("Фейк-подарки")
                .setItems(items, (dialog, which) -> {
                    ModGifts.Gift g = gifts.get(which);
                    if (ModGifts.get().take(g.id)) {
                        toast("Взял: " + g.name + " (фейк)");
                    } else {
                        toast("Недостаточно фейк-звёзд");
                    }
                    build();
                })
                .setNegativeButton("Закрыть", null)
                .show();
    }

    private void showGrantStars() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Сколько фейк-звёзд");
        new AlertDialog.Builder(this)
                .setTitle("Выдать фейк-звёзды")
                .setView(input)
                .setPositiveButton("Выдать", (d, w) -> {
                    try {
                        long n = Long.parseLong(input.getText().toString().trim());
                        ModConfig.get().addFakeStars(n);
                        toast("Выдано " + n);
                        build();
                    } catch (Throwable t) {
                        toast("Введи число");
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showChangeOwner() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(ModConfig.get().getOwnerUsername());
        new AlertDialog.Builder(this)
                .setTitle("Владелец (юзернейм)")
                .setMessage("Права получит только тот, чей юзернейм указан здесь. Смена — вручную.")
                .setView(input)
                .setPositiveButton("Сохранить", (d, w) -> {
                    ModConfig.get().setOwnerUsername(input.getText().toString().trim());
                    build();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showChangeBadge() {
        final String[] badges = {"🦫 Перри-утконос", "⭐ Звезда", "👑 Корона", "🔥 Огонь", "💎 Алмаз"};
        new AlertDialog.Builder(this)
                .setTitle("Значок владельца")
                .setItems(badges, (d, which) -> {
                    ModConfig.get().setOwnerBadge(badges[which]);
                    toast("Значок изменён");
                    build();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showSendGift() {
        final List<ModGifts.Gift> gifts = ModGifts.get().getCatalog();
        String[] items = new String[gifts.size()];
        for (int i = 0; i < gifts.size(); i++) {
            ModGifts.Gift g = gifts.get(i);
            items[i] = g.emoji + " " + g.name;
        }
        new AlertDialog.Builder(this)
                .setTitle("Какой подарок отправить?")
                .setItems(items, (d, which) -> {
                    final ModGifts.Gift g = gifts.get(which);
                    final EditText input = new EditText(this);
                    input.setHint("@юзернейм получателя");
                    new AlertDialog.Builder(this)
                            .setTitle("Кому отправить?")
                            .setView(input)
                            .setPositiveButton("Отправить", (d2, w) -> {
                                String to = input.getText().toString().trim();
                                if (to.isEmpty()) {
                                    toast("Введи юзернейм");
                                    return;
                                }
                                ModRelay.send(currentUsername(), to, g.id, g.name, (ok, msg) -> toast(msg));
                            })
                            .setNegativeButton("Отмена", null)
                            .show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showCheckIncoming() {
        ModRelay.fetchPending(currentUsername(), gifts -> {
            if (gifts.isEmpty()) {
                toast("Новых фейк-подарков нет");
                return;
            }
            for (String[] g : gifts) {
                toast(ModGifts.get().buildReceivedText(g.length > 1 ? g[1] : "подарок"));
            }
            build();
        });
    }

    private void showStarStyle() {
        final String[] styles = {"⭐", "🌟", "💫", "✨", "🔸"};
        new AlertDialog.Builder(this)
                .setTitle("Вид звёзд")
                .setItems(styles, (d, which) -> {
                    ModConfig.get().setStarStyle(styles[which]);
                    build();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showAdmins() {
        final java.util.List<String> admins = new java.util.ArrayList<>(ModConfig.get().getAdmins());
        String[] items = new String[admins.size() + 1];
        items[0] = "➕ Добавить админа";
        for (int i = 0; i < admins.size(); i++) {
            items[i + 1] = "❌ @" + admins.get(i) + " (убрать)";
        }
        new AlertDialog.Builder(this)
                .setTitle("Админы")
                .setItems(items, (d, which) -> {
                    if (which == 0) {
                        final EditText input = new EditText(this);
                        input.setHint("@юзернейм");
                        new AlertDialog.Builder(this)
                                .setTitle("Новый админ")
                                .setView(input)
                                .setPositiveButton("Добавить", (d2, w) -> {
                                    ModConfig.get().addAdmin(input.getText().toString().trim());
                                    toast("Готово");
                                    build();
                                })
                                .setNegativeButton("Отмена", null)
                                .show();
                    } else {
                        ModConfig.get().removeAdmin(admins.get(which - 1));
                        toast("Убран");
                        build();
                    }
                })
                .setNegativeButton("Закрыть", null)
                .show();
    }

    private void showRelayConfig() {
        final EditText url = new EditText(this);
        url.setHint("URL ретранслятора");
        url.setText(ModConfig.get().getRelayUrl());
        final EditText secret = new EditText(this);
        secret.setHint("Секрет");
        secret.setText(ModConfig.get().getRelaySecret());
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(url);
        box.addView(secret);
        new AlertDialog.Builder(this)
                .setTitle("Ретранслятор фейк-подарков")
                .setView(box)
                .setPositiveButton("Сохранить", (d, w) -> {
                    ModConfig.get().setRelayUrl(url.getText().toString());
                    ModConfig.get().setRelaySecret(secret.getText().toString());
                    toast("Сохранено");
                    build();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
