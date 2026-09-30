# Sonar — فاحص التطبيقات

**An open-source Android app inspector · فاحص تطبيقات أندرويد مفتوح المصدر**

Made by **Assem Hussein** — من تطوير **عاصم حسين**

---

## English

Sonar lists everything installed on your phone and tells you what you need to know about it:
version, install and update dates, APK size and how long you actually use each app. From any app
you can open it, jump to its system info screen, open it on Google Play, or uninstall it.

### Features
- **Apps list** — search, filter (all / user / system) and sort by name, install date, last update,
  usage time or size.
- **App details** — version and version code, install date, last update (absolute + relative),
  APK size, UID, foreground usage time and last used time.
- **Manage apps** — open, system app info, uninstall (through the official system confirmation
  screen). The store button always opens **Google Play only**, never another store that happens to
  be installed on the phone.
- **Freeze apps (root)** — disable an app without uninstalling it, keeping its data, and unfreeze
  it later. Requires root. Sonar asks your root manager for permission and shows a warning before
  freezing any system app.
- **Request root on demand** — Sonar only asks for root when *you* ask for it: a “Request root
  permission” button in the Root tab (and in Settings) brings up your root manager's grant prompt.
  If you try to freeze an app before granting it, Sonar offers to request root and then retries the
  freeze automatically once you approve.
- **Update checks (Google Play only)** — reads each user app's public Google Play listing and
  compares its update date with the copy installed on your device. No other store is used. Check
  everything in-app, or enable an optional background check every 12 hours with a notification
  when updates are found.
- **Root scan** — a simple, local check: `su` binaries, running the root command, root manager
  packages, Magisk / KernelSU / APatch paths, build properties and SELinux state.
- **Language** — full English and Arabic interface; follow the system language or pick one in
  Settings.
- **Internet switch** — you decide whether the app is allowed to use the network. With it off,
  Sonar makes no network requests at all.
- **Private by design** — everything runs on your device. Nothing about your apps is uploaded
  anywhere.

### Install
Download `Sonar-1.2.apk` from the [Releases](../../releases) page and open it on your phone,
allowing installation from unknown sources when asked.

- Package: `com.assem.sonar`
- Android 8.0+ (minSdk 26), targetSdk 37
- The usage-time column needs the *Usage access* permission, granted from system settings.

### Build
Requires JDK 21 and the Android SDK (compileSdk 37, build-tools 36.1.0).

```bash
./gradlew assembleRelease
```

Then align and sign the APK:

```bash
zipalign -f -p 4 app/build/outputs/apk/release/app-release-unsigned.apk Sonar-aligned.apk
apksigner sign --ks your.jks --out Sonar-1.2.apk Sonar-aligned.apk
```

### Notes
- Freezing runs `pm disable-user` / `pm enable` through a root shell. Android gives ordinary apps
  no way to disable another package, so without root the freeze button tells you root is needed.
- Google Play no longer publishes an app's version number on its public listing, so Sonar compares
  the listing's **“Updated on”** date with the last-update date of the copy on your device, leaving
  one day of slack for time zones.
- Root detection is indicative only. Advanced hiding can conceal every indicator it looks for.
  Sonar does not bypass or defeat any security or integrity check.

---

## العربية

«سونار» يسرد كل ما هو مثبّت على هاتفك ويخبرك بما يهمّك عنه: الإصدار، تواريخ التثبيت والتحديث،
حجم ملف APK، ومدة استخدامك الفعلية لكل تطبيق. ومن أي تطبيق يمكنك فتحه، أو الانتقال إلى شاشة
معلوماته في النظام، أو فتحه في متجر جوجل، أو إلغاء تثبيته.

### المزايا
- **قائمة التطبيقات** — بحث وفلترة (الكل / المستخدم / النظام) وترتيب بالاسم أو تاريخ التثبيت أو
  آخر تحديث أو وقت الاستخدام أو الحجم.
- **تفاصيل التطبيق** — الإصدار ورقمه، تاريخ التثبيت، آخر تحديث (مطلق ونسبي)، حجم ملف APK،
  معرّف المستخدم، ووقت الاستخدام وآخر استخدام.
- **إدارة التطبيقات** — فتح، معلومات النظام، وإلغاء التثبيت من شاشة تأكيد النظام. زر المتجر يفتح
  **Google Play فقط**، ولا يفتح أي متجر آخر مثبّت على الهاتف.
- **تجميد التطبيقات (يحتاج root)** — تعطيل التطبيق بدون حذفه مع بقاء بياناته، وإلغاء تجميده لاحقًا.
  يحتاج صلاحية root، ويطلبها سونار من مدير الروت، ويعرض تحذيرًا قبل تجميد أي تطبيق نظام.
- **طلب صلاحية الروت عند الحاجة** — لا يطلب سونار صلاحية الروت إلا عندما تطلبها أنت: زر «طلب صلاحية
  الروت» في تبويب الروت (وفي الإعدادات) يُظهر نافذة الموافقة من مدير الروت. وإذا حاولت تجميد تطبيق قبل
  منح الصلاحية، يعرض سونار طلبها ثم يعيد التجميد تلقائيًا بعد موافقتك.
- **فحص التحديثات (من Google Play فقط)** — يقرأ صفحة كل تطبيق مستخدم على Google Play ويقارن تاريخ
  التحديث هناك بتاريخ النسخة على جهازك. لا يُستخدم أي متجر آخر. فحص فوري داخل التطبيق، أو فحص
  خلفي اختياري كل 12 ساعة مع إشعار عند وجود تحديث.
- **فحص الروت** — فحص محلي بسيط: ملفات `su`، تنفيذ أمر الجذر، حزم إدارة الروت، مسارات
  Magisk / KernelSU / APatch، خصائص البناء، وحالة SELinux.
- **اللغة** — واجهة كاملة بالعربية والإنجليزية، تتبع لغة النظام أو تختارها من الإعدادات.
- **مفتاح الإنترنت** — أنت من يقرّر السماح للتطبيق باستخدام الشبكة. عند إيقافه لا يجري التطبيق
  أي اتصال بالشبكة إطلاقًا.
- **خصوصية بالتصميم** — كل شيء يعمل على جهازك، ولا تُرسل أي بيانات عن تطبيقاتك لأي جهة.

### التثبيت
نزّل `Sonar-1.2.apk` من صفحة [الإصدارات](../../releases) وافتحه على هاتفك، مع السماح بالتثبيت من
مصادر غير معروفة عند الطلب.

- اسم الحزمة: `com.assem.sonar`
- أندرويد 8.0 فأحدث (minSdk 26)، targetSdk 37
- عمود وقت الاستخدام يحتاج صلاحية «الوصول لبيانات الاستخدام» من إعدادات النظام.

### ملاحظات
- التجميد ينفّذ `pm disable-user` و`pm enable` عبر صدفة root. أندرويد لا يمنح التطبيقات العادية أي
  طريقة لتعطيل حزمة أخرى، لذلك بدون root يخبرك زر التجميد أن الصلاحية مطلوبة.
- Google Play لم يعد ينشر رقم إصدار التطبيق في صفحته العامة، لذلك يقارن سونار تاريخ **«Updated on»**
  في الصفحة بتاريخ آخر تحديث للنسخة على جهازك، مع ترك يوم واحد كهامش للفروق الزمنية.
- كشف الروت إرشادي فقط؛ أنظمة الإخفاء المتقدمة قد تُخفي كل العلامات التي يبحث عنها التطبيق.
  ولا يتجاوز «سونار» أي فحص أمني أو سلامة.

---

## License · الرخصة

Copyright (C) 2026 **Assem Hussein** (عاصم حسين)

Licensed under the **GNU General Public License v3.0** — see [LICENSE](LICENSE).
هذا المشروع مُرخّص تحت **رخصة جنو العمومية العامة الإصدار الثالث** — انظر ملف [LICENSE](LICENSE).
