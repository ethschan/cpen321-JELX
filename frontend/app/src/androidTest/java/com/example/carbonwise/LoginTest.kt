package com.example.carbonwise

import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.*
import com.example.carbonwise.MainActivity
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoogleSignInTest {

    private lateinit var device: UiDevice

    @Rule
    @JvmField
    var activityRule: ActivityTestRule<MainActivity> = ActivityTestRule(MainActivity::class.java)

    @Before
    fun setUp() {
        // Get the device instance
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assertNotNull("InstrumentationRegistry returned null!", instrumentation)
        device = UiDevice.getInstance(instrumentation)

        // Press home button to start fresh
        device.pressHome()

        // Get the app context and launch the main activity
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull("Application context is null!", context)

        val intent = context.packageManager.getLaunchIntentForPackage("com.example.carbonwise")
        assertNotNull("Launch intent for app is null! Check package name.", intent)

        intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
    }

    @Test
    fun testGoogleSignInFlow() {
        allowCameraPermission()

        navigateToLoginFragment()

        val googleSignInButton = device.findObject(UiSelector().resourceId("com.example.carbonwise:id/loginButton"))
        assertTrue("Google Sign-In button not found", googleSignInButton.waitForExists(5000))
        googleSignInButton.click()

        handleGoogleSignIn()
    }

    @Test
    fun testLoginConnectionError() {

        device.executeShellCommand("svc wifi disable")
        Thread.sleep(5000)

        allowCameraPermission()

        navigateToLoginFragment()

        val googleSignInButton = device.findObject(UiSelector().resourceId("com.example.carbonwise:id/loginButton"))
        assertTrue("Google Sign-In button not found", googleSignInButton.waitForExists(5000))
        googleSignInButton.click()

        val logs = device.executeShellCommand("logcat -d | grep Toast")
        assertTrue("Expected toast message not found", logs.contains("No internet connection"))

        device.executeShellCommand("svc wifi enable")
        Thread.sleep(10000) // Wifi takes a LONG time to turn on
    }

    private fun allowCameraPermission() {
        val allowButton = device.findObject(UiSelector().resourceId("com.android.permissioncontroller:id/permission_allow_foreground_only_button"))
        if (allowButton.waitForExists(5000) && allowButton.isEnabled) {
            allowButton.click()
        } else {
            // Already allowed, do nothing
        }
    }

    private fun navigateToLoginFragment() {
        val loginButton = device.findObject(UiSelector().resourceId("com.example.carbonwise:id/navigation_login"))
        if (loginButton.waitForExists(5000)) {
            loginButton.click()
        }
    }

    private fun handleGoogleSignIn() {
        val accountSelector = device.findObject(UiSelector().textContains("Continue"))
        if (accountSelector.waitForExists(5000)) {
            accountSelector.click()
        }

        val allowButton = device.findObject(UiSelector().textContains("Allow"))
        if (allowButton.waitForExists(5000)) {
            allowButton.click()
        }
    }
}
