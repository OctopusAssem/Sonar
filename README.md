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
- **Manage apps** — open, system app info, Google Play page, uninstall (through the official
  system confirmation screen).
- **Update checks** — compares each user app with the newest release on Google Play, falling back
  to Aptoide. Check everything in-app, or enable an optional background check every 12 hours with
  a notification when updates are found.
- **Root scan** — a simple, local check: `su` binaries, running the root command, root manager
  packages, Magisk / KernelSU / APatch paths, build properties and SELinux state.
- **Language** — full English and Arabic interface; follow the system language or pick one in
  Settings.
- **Internet switch** — you decide whether the app is allowed to use the network. With it off,
  Sonar makes no network requests at all.
- **Private by design** — everything runs on your device. Nothing about your apps is uploaded
  anywhere.

### Install
Download `Sonar-1.0.apk` from the [Releases](../../releases) page and open it on your phone,
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
apksigner sign --ks your.jks --out Sonar-1.0.apk Sonar-aligned.apk
```

### Notes
- Root detection is indicative only. Advanced hiding can conceal every indicator it looks for.
  Sonar does not bypass or defeat any security or integrity check.
- Update checks scrape public store pages, so they can fail when a store blocks the request; the
  app reports that instead of guessing.

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
- **إدارة التطبيقات** — فتح، معلومات النظام، صفحة متجر جوجل، وإلغاء التثبيت من شاشة تأكيد النظام.
- **فحص التحديثات** — يقارن كل تطبيق مستخدم بأحدث إصدار على Google Play ثم Aptoide كبديل. فحص
  فوري داخل التطبيق، أو فحص خلفي اختياري كل 12 ساعة مع إشعار عند وجود تحديث.
- **فحص الروت** — فحص محلي بسيط: ملفات `su`، تنفيذ أمر الجذر، حزم إدارة الروت، مسارات
  Magisk / KernelSU / APatch، خصائص البناء، وحالة SELinux.
- **اللغة** — واجهة كاملة بالعربية والإنجليزية، تتبع لغة النظام أو تختارها من الإعدادات.
- **مفتاح الإنترنت** — أنت من يقرّر السماح للتطبيق باستخدام الشبكة. عند إيقافه لا يجري التطبيق
  أي اتصال بالشبكة إطلاقًا.
- **خصوصية بالتصميم** — كل شيء يعمل على جهازك، ولا تُرسل أي بيانات عن تطبيقاتك لأي جهة.

### التثبيت
نزّل `Sonar-1.0.apk` من صفحة [الإصدارات](../../releases) وافتحه على هاتفك، مع السماح بالتثبيت من
مصادر غير معروفة عند الطلب.

- اسم الحزمة: `com.assem.sonar`
- أندرويد 8.0 فأحدث (minSdk 26)، targetSdk 37
- عمود وقت الاستخدام يحتاج صلاحية «الوصول لبيانات الاستخدام» من إعدادات النظام.

### ملاحظات
- كشف الروت إرشادي فقط؛ أنظمة الإخفاء المتقدمة قد تُخفي كل العلامات التي يبحث عنها التطبيق.
  ولا يتجاوز «سونار» أي فحص أمني أو سلامة.
- فحص التحديثات يقرأ صفحات المتاجر العامة، لذلك قد يفشل إذا حجب المتجر الطلب، وحينها يخبرك
  التطبيق بذلك بدلًا من التخمين.

---

## License · الرخصة

Copyright (C) 2026 **Assem Hussein** (عاصم حسين)

Licensed under the **GNU General Public License v3.0** — see [LICENSE](LICENSE).
هذا المشروع مُرخّص تحت **رخصة جنو العمومية العامة الإصدار الثالث** — انظر ملف [LICENSE](LICENSE).
