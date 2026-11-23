package com.txnrelay.config

/**
 * THE single place that decides which apps we listen to.
 *
 * ⚠️ Verify the package names on your target devices before relying on them:
 *   - Install bKash / Nagad from the Play Store, then run:
 *       adb shell pm list packages | grep -i -E "bkash|nagad"
 *   - Or open the app's Play Store URL: ...details?id=<package name>
 *   - Or: Settings → Apps → bKash → App info (some OEMs show the package name).
 * bKash Business / Nagad Business / region-specific builds may use different packages –
 * just add them to the sets below.
 */
enum class PaymentApp(
    /** Stable id sent to the backend as "source". */
    val id: String,
    val displayName: String,
    val packages: Set<String>,
) {
    BKASH(
        id = "bkash",
        displayName = "bKash",
        packages = setOf(
            "com.bkash.customerapp",
            // "com.bKash.businessapp",   // example: add more packages here if needed
        ),
    ),
    NAGAD(
        id = "nagad",
        displayName = "Nagad",
        packages = setOf(
            "com.konasl.nagad",
        ),
    ),
}

object TargetApps {
    /** Returns the matching app, or null if [packageName] is not one we care about. */
    fun fromPackage(packageName: String?): PaymentApp? {
        if (packageName == null) return null
        return PaymentApp.entries.firstOrNull { packageName in it.packages }
    }

    val displayNames: List<String> = PaymentApp.entries.map { it.displayName }
}
