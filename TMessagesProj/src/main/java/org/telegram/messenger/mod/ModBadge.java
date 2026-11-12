package org.telegram.messenger.mod;

import org.telegram.tgnet.TLRPC;

/** Вспомогательные косметические вставки в UI Telegram. */
public class ModBadge {

    /**
     * Добавляет значок владельца к имени пользователя, если это владелец.
     * Работает только локально и только у мод-юзеров.
     */
    public static CharSequence appendOwner(CharSequence name, TLRPC.User user) {
        if (name == null || user == null) {
            return name;
        }
        try {
            ModConfig cfg = ModConfig.get();
            if (cfg.isOwner(user.username)) {
                String emoji = cfg.getOwnerBadgeEmoji();
                if (emoji != null && !emoji.isEmpty()) {
                    return name + " " + emoji;
                }
            }
        } catch (Throwable ignore) {
        }
        return name;
    }
}
