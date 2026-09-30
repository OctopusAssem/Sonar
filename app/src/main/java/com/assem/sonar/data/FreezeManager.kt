package com.assem.sonar.data

import java.util.concurrent.TimeUnit

/**
 * Freezes (disables) and unfreezes apps.
 *
 * Android does not let an ordinary app disable another package — that needs the signature-level
 * CHANGE_COMPONENT_ENABLED_STATE permission — so Sonar performs the operation through a root
 * shell (`pm disable-user` / `pm enable`). Without root the operation is reported as unavailable.
 */
object FreezeManager {

    sealed interface Result {
        data object Success : Result

        /** No usable root shell (not granted, not installed, or the prompt was never answered). */
        data object NoRoot : Result

        data class Failure(val detail: String) : Result
    }

    private data class SuResult(val code: Int, val output: String)

    private val validPackage = Regex("^[A-Za-z0-9_.]{2,255}$")

    /** Packages whose removal can leave the device unusable. */
    private val criticalPackages = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.providers.settings",
        "com.android.providers.telephony",
        "com.android.providers.contacts",
        "com.android.providers.media",
        "com.android.shell",
        "com.android.keychain",
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.android.launcher3",
        "com.miui.home",
        "com.android.inputmethod.latin",
    )

    private val criticalPrefixes = listOf(
        "com.android.launcher",
        "com.android.inputmethod",
        "com.google.android.inputmethod",
    )

    fun isCritical(packageName: String): Boolean =
        packageName in criticalPackages || criticalPrefixes.any { packageName.startsWith(it) }

    /** True when a root shell answers as uid 0. Shows the root manager prompt on first use. */
    fun hasRoot(): Boolean {
        val r = runSu("id")
        return r.code == 0 && r.output.contains("uid=0")
    }

    fun freeze(packageName: String): Result = apply(packageName, disable = true)

    fun unfreeze(packageName: String): Result = apply(packageName, disable = false)

    private fun apply(packageName: String, disable: Boolean): Result {
        if (!validPackage.matches(packageName)) {
            return Result.Failure("invalid package name")
        }
        val command = if (disable) {
            "pm disable-user --user 0 $packageName"
        } else {
            "pm enable $packageName"
        }
        val r = runSu(command)
        if (r.code == -1) return Result.NoRoot
        if (r.code != 0 && r.output.isBlank()) {
            return Result.Failure("exit ${r.code}")
        }
        // Trust the reported state, not the exit code alone.
        return if (isDisabled(packageName) == disable) {
            Result.Success
        } else {
            Result.Failure(r.output.ifBlank { "exit ${r.code}" })
        }
    }

    fun isDisabled(packageName: String): Boolean {
        val r = runSu("pm list packages -d")
        if (r.code != 0) return false
        return r.output.lineSequence().any { it.trim() == "package:$packageName" }
    }

    private fun runSu(command: String, timeoutMs: Long = 25_000L): SuResult {
        return try {
            val process = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()
            val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                SuResult(-1, "timed out waiting for root")
            } else {
                val out = process.inputStream.bufferedReader().use { it.readText() }.trim()
                SuResult(process.exitValue(), out)
            }
        } catch (t: Throwable) {
            SuResult(-1, t.javaClass.simpleName)
        }
    }
}
