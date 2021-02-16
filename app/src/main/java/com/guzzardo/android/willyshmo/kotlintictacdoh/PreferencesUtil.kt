package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.content.Context

object PreferencesUtil {
    var PREFS_FILE_NAME = "Joes_Preferences"
    @JvmStatic
    fun firstTimeAskingPermission(
        context: Context,
        permission: String?,
        isFirstTime: Boolean
    ) {
        val sharedPreference = context.getSharedPreferences(
            PREFS_FILE_NAME,
            Context.MODE_PRIVATE
        )
        sharedPreference.edit().putBoolean(permission, isFirstTime).apply()
    }

    @JvmStatic
    fun isFirstTimeAskingPermission(
        context: Context,
        permission: String?
    ): Boolean {
        return context.getSharedPreferences(
            PREFS_FILE_NAME,
            Context.MODE_PRIVATE
        ).getBoolean(permission, true)
    }
}