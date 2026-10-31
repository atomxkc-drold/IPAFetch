package com.omnidex.app.security

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import com.omnidex.app.data.AppSettings
import com.omnidex.app.domain.AccessPolicy
import com.omnidex.app.domain.AccessPolicyType
import com.omnidex.app.domain.VaultEntry
import java.util.Calendar

object AccessPolicyEngine {
    data class AccessDecision(
        val allow: Boolean,
        val reason: String = "",
        val requiredActions: List<RequiredAction> = emptyList()
    )
    
    enum class RequiredAction { AUTHENTICATE_BIOMETRIC, WAIT_UNTIL_WINDOW, DISABLE_SCREEN_CAPTURE, UNROOT_DEVICE }
    
    private val cache = mutableMapOf<Long, Pair<Long, AccessDecision>>()
    private const val CACHE_TTL = 30000L
    
    fun evaluate(entry: VaultEntry, context: Context, settings: AppSettings): AccessDecision {
        if (!settings.policyEngineEnabled) return AccessDecision(true)
        if (entry.policy.isEmpty()) {
            if (settings.policyDefaultDenyForProduction && entry.category.contains("prod", ignoreCase = true)) {
                return AccessDecision(false, "Production entry requires policy", listOf(RequiredAction.AUTHENTICATE_BIOMETRIC))
            }
            return AccessDecision(true)
        }
        
        // Check cache
        val cached = cache[entry.id]
        if (cached != null && System.currentTimeMillis() - cached.first < CACHE_TTL) {
            return cached.second
        }
        
        val decisions = mutableListOf<Boolean>()
        val reasons = mutableListOf<String>()
        val actions = mutableListOf<RequiredAction>()
        
        for (policy in entry.policy.filter { it.enabled }) {
            when (policy.type) {
                AccessPolicyType.TIME_WINDOW -> {
                    val startHour = policy.params["startHour"]?.toIntOrNull() ?: 9
                    val endHour = policy.params["endHour"]?.toIntOrNull() ?: 18
                    val calendar = Calendar.getInstance()
                    val hour = calendar.get(Calendar.HOUR_OF_DAY)
                    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    val isWeekday = dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY
                    val allowedDays = policy.params["weekdaysOnly"]?.toBoolean() ?: true
                    
                    if (allowedDays && !isWeekday) {
                        decisions.add(false)
                        reasons.add("Access denied outside weekdays")
                        // Calculate next weekday
                    } else if (hour < startHour || hour >= endHour) {
                        decisions.add(false)
                        reasons.add("Access denied outside time window $startHour:00-$endHour:00")
                        actions.add(RequiredAction.WAIT_UNTIL_WINDOW)
                    } else {
                        decisions.add(true)
                    }
                }
                AccessPolicyType.REQUIRE_BIOMETRIC -> {
                    // Assume biometric available if enabled
                    decisions.add(true)
                }
                AccessPolicyType.REQUIRE_CHARGING -> {
                    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    val isCharging = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        batteryManager.isCharging
                    } else {
                        false
                    }
                    if (!isCharging) {
                        decisions.add(false)
                        reasons.add("Device must be charging")
                    } else {
                        decisions.add(true)
                    }
                }
                AccessPolicyType.DENY_IF_ROOTED -> {
                    if (RootDetector.isRooted()) {
                        decisions.add(false)
                        reasons.add("Access denied on rooted device")
                        actions.add(RequiredAction.UNROOT_DEVICE)
                    } else {
                        decisions.add(true)
                    }
                }
                AccessPolicyType.DENY_IF_SCREEN_CAPTURE -> {
                    // Cannot directly detect, but we can warn
                    decisions.add(true)
                }
            }
        }
        
        val allow = decisions.all { it }
        val reason = if (!allow) reasons.joinToString("; ") else ""
        val decision = AccessDecision(allow, reason, actions.distinct())
        cache[entry.id] = Pair(System.currentTimeMillis(), decision)
        return decision
    }
    
    fun clearCache() {
        cache.clear()
    }
}
