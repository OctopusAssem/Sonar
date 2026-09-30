package com.assem.sonar.root

import android.content.Context
import android.os.Build
import com.assem.sonar.R
import com.assem.sonar.model.RootCheckResult
import java.io.File
import java.util.concurrent.TimeUnit

class RootChecker(private val context: Context) {

    private val suPaths = listOf(
        "/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su",
        "/data/local/bin/su", "/data/local/xbin/su", "/data/local/su",
        "/system/sd/xbin/su", "/system/bin/failsafe/su", "/system/bin/.ext/su",
        "/system/usr/we-need-root/su", "/cache/su", "/dev/su",
    )

    private val rootPackages = listOf(
        "com.topjohnwu.magisk", "io.github.huskydg.magisk", "io.github.vvb2060.magisk",
        "com.kingroot.kinguser", "com.kingo.root", "com.zhiqupk.root.global",
        "eu.chainfire.supersu", "com.noshufou.android.su", "com.koushikdutta.superuser",
        "com.thirdparty.superuser", "com.yellowes.su", "me.phh.superuser",
        "com.ramdroid.appquarantine", "com.formyhm.hideroot", "org.freedesktop.superuser",
    )

    private val rootPaths = listOf(
        "/sbin/.magisk", "/data/adb/magisk", "/data/adb/modules", "/cache/.disable_magisk",
        "/data/adb/ksu", "/data/adb/ksud", "/data/adb/ksu/bin/ksud",
        "/data/adb/ap", "/data/adb/apd", "/data/adb/susfs",
        "/dev/com.koushikdutta.superuser.daemon/",
    )

    private val busyboxPaths = listOf(
        "/system/bin/busybox", "/system/xbin/busybox", "/sbin/busybox",
        "/data/local/busybox", "/data/local/xbin/busybox",
    )

    fun runAll(): List<RootCheckResult> = listOf(
        checkSuFiles(),
        checkSuExec(),
        checkRootPackages(),
        checkRootPaths(),
        checkBusybox(),
        checkBuildTags(),
        checkProps(),
        checkSelinux(),
    )

    private fun checkSuFiles(): RootCheckResult {
        val found = suPaths.filter { exists(it) }
        return RootCheckResult(
            category = str(R.string.rc_category_binaries),
            name = str(R.string.rc_su_binaries),
            detected = found.isNotEmpty(),
            detail = if (found.isEmpty()) str(R.string.rc_none_found) else found.joinToString("\n"),
        )
    }

    private fun checkSuExec(): RootCheckResult {
        val name = str(R.string.rc_su_exec)
        val category = str(R.string.rc_category_exec)
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val finished = p.waitFor(3, TimeUnit.SECONDS)
            val text = p.inputStream.bufferedReader().use { it.readText() }.trim()
            val isRoot = finished && text.contains("uid=0")
            RootCheckResult(
                category = category,
                name = name,
                detected = isRoot,
                detail = when {
                    isRoot -> text.take(120)
                    finished && text.isNotEmpty() -> str(R.string.rc_exec_no_root, text.take(80))
                    else -> str(R.string.rc_exec_unavailable)
                },
            )
        } catch (t: Throwable) {
            RootCheckResult(
                category, name, false,
                str(R.string.rc_exec_error, t.javaClass.simpleName),
            )
        }
    }

    private fun checkRootPackages(): RootCheckResult {
        val pm = context.packageManager
        val found = rootPackages.filter { pkg ->
            runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess
        }
        return RootCheckResult(
            category = str(R.string.rc_category_packages),
            name = str(R.string.rc_root_packages),
            detected = found.isNotEmpty(),
            detail = if (found.isEmpty()) str(R.string.rc_none) else found.joinToString("\n"),
        )
    }

    private fun checkRootPaths(): RootCheckResult {
        val found = rootPaths.filter { exists(it) }
        return RootCheckResult(
            category = str(R.string.rc_category_paths),
            name = str(R.string.rc_root_paths),
            detected = found.isNotEmpty(),
            detail = if (found.isEmpty()) str(R.string.rc_none) else found.joinToString("\n"),
        )
    }

    private fun checkBusybox(): RootCheckResult {
        val found = busyboxPaths.filter { exists(it) }
        return RootCheckResult(
            category = str(R.string.rc_category_binaries),
            name = str(R.string.rc_busybox),
            detected = found.isNotEmpty(),
            detail = if (found.isEmpty()) str(R.string.rc_none_found) else found.joinToString("\n"),
        )
    }

    private fun checkBuildTags(): RootCheckResult {
        val tags = Build.TAGS ?: ""
        return RootCheckResult(
            category = str(R.string.rc_category_system),
            name = str(R.string.rc_build_tags),
            detected = tags.contains("test-keys"),
            detail = "TAGS=$tags",
        )
    }

    private fun checkProps(): RootCheckResult {
        val debuggable = prop("ro.debuggable")
        val secure = prop("ro.secure")
        return RootCheckResult(
            category = str(R.string.rc_category_system),
            name = str(R.string.rc_props),
            detected = debuggable == "1" || secure == "0",
            detail = "ro.debuggable=$debuggable  ro.secure=$secure",
        )
    }

    private fun checkSelinux(): RootCheckResult {
        val enforce = runCatching { File("/sys/fs/selinux/enforce").readText().trim() }.getOrNull()
        return RootCheckResult(
            category = str(R.string.rc_category_system),
            name = str(R.string.rc_selinux),
            detected = enforce == "0",
            detail = when (enforce) {
                null -> str(R.string.rc_selinux_unavailable)
                "1" -> str(R.string.rc_selinux_enforcing)
                "0" -> str(R.string.rc_selinux_permissive)
                else -> enforce
            },
        )
    }

    private fun str(id: Int, vararg args: Any): String = context.getString(id, *args)

    private fun prop(name: String): String {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("getprop", name))
            p.waitFor(2, TimeUnit.SECONDS)
            p.inputStream.bufferedReader().use { it.readText() }.trim()
        } catch (_: Throwable) {
            "?"
        }
    }

    private fun exists(path: String): Boolean = runCatching { File(path).exists() }.getOrDefault(false)
}
