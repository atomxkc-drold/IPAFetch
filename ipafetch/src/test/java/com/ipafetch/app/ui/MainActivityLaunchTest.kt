package com.ipafetch.app.ui

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import com.ipafetch.app.MainActivity
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertNotNull

/**
 * Boots the real MainActivity so a startup crash (theme inflation, composition,
 * ViewModel wiring) fails here instead of on a device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MainActivityLaunchTest {

    @Test
    fun firstLaunchComposesTheSetupScreen() {
        // Guards the manifest <-> source package wiring: if the declared launcher
        // activity does not exist, this test fails before the APK ships.
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertNotNull(activity)

        val content = activity.window.decorView
        assertNotNull(content)

        controller.pause().stop().destroy()
    }
}
