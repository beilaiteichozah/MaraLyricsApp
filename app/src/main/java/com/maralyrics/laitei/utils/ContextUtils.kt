package com.maralyrics.laitei.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Resources
import androidx.activity.ComponentActivity

fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

class LocalizedContextWrapper(base: Context, private val localizedContext: Context) : ContextWrapper(base) {
    override fun getResources(): Resources = localizedContext.resources
    override fun getAssets(): AssetManager = localizedContext.assets
    override fun getSystemService(name: String): Any? = localizedContext.getSystemService(name)
    override fun getTheme(): Resources.Theme = localizedContext.theme
}
