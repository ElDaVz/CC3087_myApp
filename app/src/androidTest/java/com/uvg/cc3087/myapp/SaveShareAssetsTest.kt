package com.uvg.cc3087.myapp

import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveShareAssetsTest {
    @Test fun allFiveLocalSvgAssetsCanBeDecodedWithoutNetworkAccess() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val loader = ImageLoader.Builder(context).build()
        try {
            for (asset in listOf(
                R.raw.save_share_success, R.raw.save_share_copy, R.raw.save_share_whatsapp,
                R.raw.save_share_link, R.raw.save_share_back
            )) {
                val request = ImageRequest.Builder(context)
                    .data("android.resource://${context.packageName}/$asset")
                    .size(64, 64)
                    .build()
                val result = loader.execute(request)
                if (result is ErrorResult) throw AssertionError("SVG resource $asset failed", result.throwable)
                assertTrue(result is SuccessResult)
                val image = (result as SuccessResult).image
                assertTrue(image.width > 0 && image.height > 0)
            }
        } finally {
            loader.shutdown()
        }
    }
}
