package com.example.myapplication.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest

/**
 * Checks if all required permissions are granted.
 *
 * @param context The context to check permissions.
 * @param permissions Array of permissions to check.
 * @return `true` if all permissions are granted, `false` otherwise.
 */
fun hasRequiredPermissions(context: Context, permissions: Array<String>): Boolean {
    return permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

