package org.telegram.messenger.mod;

import android.content.Context;

import org.telegram.messenger.ApplicationLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import dalvik.system.DexClassLoader;

/**
 * Простейший загрузчик плагинов.
 *
 * Плагины лежат в каталоге {@code <внешний каталог>/mod_plugins}.
 * Рядом с файлом плагина ({@code X.jar}/{@code X.dex}/{@code X.apk}) кладётся
 * текстовый файл {@code X.plugin} с одной строкой — полным именем класса,
 * реализующего {@link ModPlugin} и имеющего публичный конструктор без аргументов.
 */
public class ModPlugins {

    public interface ModPlugin {
        String getId();

        String getName();

        void onLoad(Context context);
    }

    private static ModPlugins instance;

    public static synchronized ModPlugins get() {
        if (instance == null) {
            instance = new ModPlugins();
        }
        return instance;
    }

    private final List<ModPlugin> loaded = new ArrayList<>();

    public File getPluginsDir() {
        Context ctx = ApplicationLoader.applicationContext;
        File base = ctx.getExternalFilesDir(null);
        if (base == null) {
            base = ctx.getFilesDir();
        }
        File dir = new File(base, "mod_plugins");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    public List<ModPlugin> getLoaded() {
        return loaded;
    }

    public void loadAll() {
        loaded.clear();
        if (!ModConfig.get().isPluginsEnabled()) {
            return;
        }
        File dir = getPluginsDir();
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        Context ctx = ApplicationLoader.applicationContext;
        File optDir = ctx.getDir("mod_plugin_opt", Context.MODE_PRIVATE);
        for (File f : files) {
            String name = f.getName().toLowerCase();
            if (!name.endsWith(".jar") && !name.endsWith(".dex") && !name.endsWith(".apk")) {
                continue;
            }
            String className = readManifest(f);
            if (className == null || className.isEmpty()) {
                continue;
            }
            try {
                DexClassLoader loader = new DexClassLoader(
                        f.getAbsolutePath(),
                        optDir.getAbsolutePath(),
                        null,
                        ctx.getClassLoader());
                Class<?> cls = loader.loadClass(className);
                Object obj = cls.getDeclaredConstructor().newInstance();
                if (obj instanceof ModPlugin) {
                    ModPlugin plugin = (ModPlugin) obj;
                    plugin.onLoad(ctx);
                    loaded.add(plugin);
                }
            } catch (Throwable ignore) {
            }
        }
    }

    private String readManifest(File pluginFile) {
        String base = pluginFile.getName();
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        File manifest = new File(pluginFile.getParentFile(), base + ".plugin");
        if (!manifest.exists()) {
            return null;
        }
        try (BufferedReader r = new BufferedReader(new FileReader(manifest))) {
            return r.readLine();
        } catch (Throwable t) {
            return null;
        }
    }
}
