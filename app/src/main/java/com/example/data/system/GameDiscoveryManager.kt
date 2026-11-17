package com.example.data.system

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.example.data.model.GameEntry
import com.example.data.model.GameTargetType

class GameDiscoveryManager(private val context: Context) {

    companion object {
        const val PKG_FREE_FIRE = "com.dts.freefireth"
        const val PKG_FREE_FIRE_MAX = "com.dts.freefiremax"
    }

    fun getPrimaryFreeFireTargets(): List<GameEntry> {
        return listOf(
            buildGameEntry(PKG_FREE_FIRE, "Free Fire", GameTargetType.FREE_FIRE),
            buildGameEntry(PKG_FREE_FIRE_MAX, "Free Fire MAX", GameTargetType.FREE_FIRE_MAX)
        )
    }

    fun detectInstalledGames(manualCustomPackages: Set<String>): List<GameEntry> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolvedApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcherIntent, 0)
        }

        val detected = mutableListOf<GameEntry>()
        val seenPackages = mutableSetOf(PKG_FREE_FIRE, PKG_FREE_FIRE_MAX)

        for (resolveInfo in resolvedApps) {
            val appInfo = resolveInfo.activityInfo?.applicationInfo ?: continue
            val pkg = appInfo.packageName ?: continue
            if (pkg == context.packageName || seenPackages.contains(pkg)) continue

            val isGame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appInfo.category == ApplicationInfo.CATEGORY_GAME
            } else {
                @Suppress("DEPRECATION")
                (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
            }

            val isManual = manualCustomPackages.contains(pkg)
            if (isGame || isManual) {
                seenPackages.add(pkg)
                val label = pm.getApplicationLabel(appInfo).toString()
                val icon = runCatching { pm.getApplicationIcon(appInfo) }.getOrNull()
                val version = getVersionName(pkg)
                detected.add(
                    GameEntry(
                        packageName = pkg,
                        displayName = label,
                        isInstalled = true,
                        versionName = version,
                        targetType = if (isManual) GameTargetType.MANUAL_CUSTOM else GameTargetType.DETECTED_GAME,
                        appIcon = icon
                    )
                )
            }
        }
        return detected.sortedBy { it.displayName.lowercase() }
    }

    fun getAllLaunchableAppsForManualPicker(): List<GameEntry> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolvedApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcherIntent, 0)
        }

        val seen = mutableSetOf<String>()
        val results = mutableListOf<GameEntry>()
        for (info in resolvedApps) {
            val appInfo = info.activityInfo?.applicationInfo ?: continue
            val pkg = appInfo.packageName ?: continue
            if (pkg == context.packageName || !seen.add(pkg)) continue
            val label = pm.getApplicationLabel(appInfo).toString()
            val icon = runCatching { pm.getApplicationIcon(appInfo) }.getOrNull()
            results.add(
                GameEntry(
                    packageName = pkg,
                    displayName = label,
                    isInstalled = true,
                    versionName = getVersionName(pkg),
                    targetType = GameTargetType.MANUAL_CUSTOM,
                    appIcon = icon
                )
            )
        }
        return results.sortedBy { it.displayName.lowercase() }
    }

    fun resolveTargetGame(packageName: String): GameEntry {
        return when (packageName) {
            PKG_FREE_FIRE -> buildGameEntry(PKG_FREE_FIRE, "Free Fire", GameTargetType.FREE_FIRE)
            PKG_FREE_FIRE_MAX -> buildGameEntry(PKG_FREE_FIRE_MAX, "Free Fire MAX", GameTargetType.FREE_FIRE_MAX)
            else -> buildGameEntry(packageName, getAppLabelOrPackage(packageName), GameTargetType.MANUAL_CUSTOM)
        }
    }

    fun launchGame(packageName: String): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return try {
            context.startActivity(launchIntent)
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun openPlayStoreForGame(packageName: String) {
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(marketIntent)
        } catch (_: Throwable) {
            runCatching { context.startActivity(webIntent) }
        }
    }

    private fun buildGameEntry(
        packageName: String,
        fallbackName: String,
        type: GameTargetType
    ): GameEntry {
        val pkgInfo = getPackageInfoOrNull(packageName)
        val installed = pkgInfo != null
        val icon = if (installed) runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull() else null
        return GameEntry(
            packageName = packageName,
            displayName = if (installed && type == GameTargetType.MANUAL_CUSTOM) {
                getAppLabelOrPackage(packageName)
            } else fallbackName,
            isInstalled = installed,
            versionName = pkgInfo?.versionName,
            targetType = type,
            appIcon = icon
        )
    }

    private fun getAppLabelOrPackage(packageName: String): String {
        return try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getApplicationInfo(packageName, 0)
            }
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Throwable) {
            packageName
        }
    }

    private fun getVersionName(packageName: String): String? {
        return getPackageInfoOrNull(packageName)?.versionName
    }

    private fun getPackageInfoOrNull(packageName: String): PackageInfo? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
        } catch (_: Throwable) {
            null
        }
    }
}
