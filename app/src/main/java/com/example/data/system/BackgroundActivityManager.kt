package com.example.data.system

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.data.model.BackgroundAppEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackgroundActivityManager(private val context: Context) {

    private val protectedSystemPrefixes = listOf(
        "com.android.systemui",
        "com.android.phone",
        "com.android.providers",
        "com.android.settings",
        "com.google.android.gms",
        "com.google.android.gsf",
        "android"
    )

    suspend fun getBackgroundActivity(
        targetGamePackage: String,
        hasUsageAccess: Boolean
    ): List<BackgroundAppEntry> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val results = mutableListOf<BackgroundAppEntry>()

        if (hasUsageAccess) {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                now - 1000L * 60L * 90L,
                now
            )
            if (!stats.isNullOrEmpty()) {
                val aggregated = stats
                    .filter { it.packageName != context.packageName && it.packageName != targetGamePackage }
                    .groupBy { it.packageName }
                    .mapNotNull { (pkg, list) ->
                        val latest = list.maxByOrNull { it.lastTimeUsed } ?: return@mapNotNull null
                        if (latest.lastTimeUsed <= 0L) return@mapNotNull null
                        val fgMinutes = list.sumOf { it.totalTimeInForeground } / 60_000L
                        val appInfo = getAppInfoOrNull(pm, pkg) ?: return@mapNotNull null
                        val isSys = isSystemCriticalApp(appInfo, pkg)
                        BackgroundAppEntry(
                            packageName = pkg,
                            appName = pm.getApplicationLabel(appInfo).toString(),
                            lastUsedMs = latest.lastTimeUsed,
                            foregroundMinutes = fgMinutes,
                            importanceLabel = if (isSys) "System Protected" else "User / Background App",
                            isSystemCritical = isSys,
                            appIcon = runCatching { pm.getApplicationIcon(appInfo) }.getOrNull()
                        )
                    }
                    .sortedWith(compareBy<BackgroundAppEntry> { it.isSystemCritical }.thenByDescending { it.lastUsedMs })
                    .take(20)
                results.addAll(aggregated)
            }
        }

        if (results.isEmpty()) {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            val running = am.runningAppProcesses.orEmpty()
            for (proc in running) {
                val pkg = proc.pkgList?.firstOrNull() ?: proc.processName
                val appInfo = getAppInfoOrNull(pm, pkg)
                val name = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: proc.processName
                val isSys = appInfo?.let { isSystemCriticalApp(it, pkg) } ?: true
                val importance = when (proc.importance) {
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "Foreground"
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> "Visible"
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE -> "Background Service"
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED -> "Cached Process"
                    else -> "Active Process"
                }
                results.add(
                    BackgroundAppEntry(
                        packageName = pkg,
                        appName = name,
                        lastUsedMs = System.currentTimeMillis(),
                        foregroundMinutes = 0L,
                        importanceLabel = importance,
                        isSystemCritical = isSys,
                        appIcon = appInfo?.let { runCatching { pm.getApplicationIcon(it) }.getOrNull() }
                    )
                )
            }
        }

        results
    }

    fun buildUsageAccessSettingsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun buildAppInfoIntent(packageName: String): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun isSystemCriticalApp(appInfo: ApplicationInfo, pkg: String): Boolean {
        val isUpdatedSys = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
            (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
        val matchesPrefix = protectedSystemPrefixes.any { pkg == it || pkg.startsWith("$it.") }
        return isUpdatedSys || matchesPrefix
    }

    private fun getAppInfoOrNull(pm: PackageManager, pkg: String): ApplicationInfo? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(pkg, 0)
            }
        } catch (_: Throwable) {
            null
        }
    }
}
