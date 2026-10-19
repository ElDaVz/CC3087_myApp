package com.uvg.cc3087.myapp

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class LauncherIconTest {
    @Test fun lightAndDarkConfigurationsLoadTheirOwnLauncherResources() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val variants = listOf(
            Configuration.UI_MODE_NIGHT_NO to Color.rgb(0x66, 0x50, 0xA4),
            Configuration.UI_MODE_NIGHT_YES to Color.rgb(0x1C, 0x1B, 0x1F)
        )
        variants.forEach { (nightMode, expectedBackground) ->
            val configuration = Configuration(base.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
            }
            val context = base.createConfigurationContext(configuration)
            listOf(R.mipmap.ic_launcher, R.mipmap.ic_launcher_round).forEach { resource ->
                val drawable = context.getDrawable(resource)
                assertTrue("El recurso $resource debe ser adaptativo en modo $nightMode", drawable is AdaptiveIconDrawable)
                val icon = drawable as AdaptiveIconDrawable
                val background = Bitmap.createBitmap(108, 108, Bitmap.Config.ARGB_8888)
                icon.background.setBounds(0, 0, 108, 108)
                icon.background.draw(Canvas(background))
                assertEquals(expectedBackground, background.getPixel(54, 54))
                val foreground = Bitmap.createBitmap(108, 108, Bitmap.Config.ARGB_8888)
                icon.foreground.setBounds(0, 0, 108, 108)
                icon.foreground.draw(Canvas(foreground))
                assertNotEquals(Color.TRANSPARENT, foreground.getPixel(36, 50))
                if (Build.VERSION.SDK_INT >= 33) assertNotNull(icon.monochrome)
                background.recycle()
                foreground.recycle()
            }
        }
        assertEquals("FormLink", base.getString(R.string.app_name))
    }
}
