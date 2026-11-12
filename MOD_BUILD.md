# Как получить APK (через GitHub Actions)

Сборка Telegram Android требует 8–16 ГБ RAM и много часов на слабом ПК,
поэтому собираем в облаке GitHub — бесплатно.

## Что нужно от тебя

1. Аккаунт на https://github.com (если нет — зарегистрируйся, это бесплатно).
2. Установленный **Git** (уже установлен) или GitHub Desktop.

## Шаги

### 1. Создать пустой репозиторий на GitHub
- Открой https://github.com/new
- Имя: например `telegram-mod`
- Тип: **Public** (для бесплатной сборки; Telegram под GPL — форк и так должен быть открытым)
- Не добавляй README/gitignore/license.

### 2. Залить исходники

В папке `D:\TelegramMod\src\telegram-android` выполни в PowerShell:

```powershell
cd D:\TelegramMod\src\telegram-android

# укажи свои данные GitHub
git config user.email "gaivaichicken@gmail.com"
git config user.name "gaivaichicken-maker"

# зафиксировать изменения мода
git add -A
git commit -m "Telegram mod: cosmetic features + CI"

# подключить твой репозиторий (замени URL на свой)
git remote add origin https://github.com/gaivaichicken-maker/telegrammod.git
git branch -M main
git push -u origin main
```

При push Git попросит логин/пароль. Вместо пароля нужен **Personal Access Token**:
GitHub → Settings → Developer settings → Personal access tokens → Generate new token (scope: `repo`).

### 3. Запустить сборку
- Открой свой репозиторий на GitHub → вкладка **Actions**
- Слева выбери **Build APK** → кнопка **Run workflow**
- Жди ~40–90 минут (первая сборка самая долгая).

### 4. Скачать APK
- В завершённой сборке внизу будет артефакт **telegram-mod-apk**
- Скачай архив, внутри — `app.apk`
- Перекинь на телефон и установи (разреши установку из неизвестных источников).

## Важно
- Репозиторий должен быть **Public** — иначе минут сборки может не хватить.
- APK подписан тестовым ключом из репозитория, ставится поверх чистой установки
  только если удалить официальный Telegram (другой package signature).
