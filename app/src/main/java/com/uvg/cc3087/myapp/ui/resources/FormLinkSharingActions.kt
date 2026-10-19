package com.uvg.cc3087.myapp.ui.resources

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.app.Activity
import com.uvg.cc3087.myapp.R

// estos efectos pertenecen a android, no al viewmodel ni al repositorio
internal fun copyFormLink(context: Context, link: String): Boolean = try {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    if (clipboard == null) false else {
        clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.shareable_link), link))
        true
    }
} catch (_: SecurityException) {
    false
}

internal fun shareFormLinkOnWhatsApp(context: Context, link: String): Boolean {
    for (packageName in listOf("com.whatsapp", "com.whatsapp.w4b")) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, link)
            setPackage(packageName)
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            return true
        } catch (_: ActivityNotFoundException) {
            // si no está la versión normal, intentamos con whatsapp business
        } catch (_: SecurityException) {
            return false
        }
    }
    return false
}
