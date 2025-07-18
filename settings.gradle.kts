// Settings file with minimal configuration
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "AuraFrameFX"

// Include modules
include(":app")

// Only include jvm-test if it exists
if (file("jvm-test").exists()) {
    include(":jvm-test")
    project(":jvm-test").projectDir = file("jvm-test")
}

if (file("sandbox-ui").exists()) {
    include(":sandbox-ui")
}

// Clean up any commented-out or non-existent module includes
// Remove or uncomment these based on your actual project structure

// Commenting out all lib-* modules as their directories are reported missing by Gradle
// include(":lib-ai")
// include(":lib-ai-ai21")
// include(":lib-ai-ai22")
// include(":lib-ai-ai23")
// include(":lib-ai-ai24")
// include(":lib-ai-ai25")
// include(":lib-ai-ai26")
// include(":lib-ai-ai27")
// include(":lib-ai-ai28")
// include(":lib-ai-ai29")
// include(":lib-ai-ai30")
// include(":lib-ai-ai31")
// include(":lib-ai-ai32")
// include(":lib-ai-ai33")
// include(":lib-ai-ai34")
// include(":lib-ai-ai35")
// include(":lib-ai-ai36")
// include(":lib-ai-ai37")
// include(":lib-ai-ai38")
// include(":lib-ai-ai39")
// include(":lib-ai-ai40")
// include(":lib-ai-ai41")
// include(":lib-ai-ai42")
// include(":lib-ai-ai43")
// include(":lib-ai-ai44")
//
include(":lib-ai-ai21-xposed")
include(":lib-ai-ai22-xposed")
include(":lib-ai-ai23-xposed")
include(":lib-ai-ai24-xposed")
include(":lib-ai-ai25-xposed")
include(":lib-ai-ai26-xposed")
include(":lib-ai-ai27-xposed")
include(":lib-ai-ai28-xposed")
include(":lib-ai-ai29-xposed")
include(":lib-ai-ai30-xposed")
include(":lib-ai-ai31-xposed")
include(":lib-ai-ai32-xposed")
include(":lib-ai-ai33-xposed")
include(":lib-ai-ai34-xposed")
include(":lib-ai-ai35-xposed")
include(":lib-ai-ai36-xposed")
include(":lib-ai-ai37-xposed")
include(":lib-ai-ai38-xposed")
include(":lib-ai-ai39-xposed")
include(":lib-ai-ai40-xposed")
include(":lib-ai-ai41-xposed")
include(":lib-ai-ai42-xposed")
include(":lib-ai-ai43-xposed")
include(":lib-ai-ai44-xposed")

include(":lib-system-quicksettings")
include(":lib-system-quicksettings-xposed")
include(":lib-system-lockscreen")
include(":lib-system-lockscreen-xposed")
include(":lib-system-overlay")
include(":lib-system-overlay-xposed")
include(":lib-system-homescreen")
include(":lib-system-homescreen-xposed")

include(":lib-system-notchbar")
include(":lib-system-notchbar-xposed")

include(":lib-system-statusbar")
include(":lib-system-statusbar-xposed")

include(":lib-system-navigationbar")
include(":lib-system-navigationbar-xposed")

include(":lib-system-telephony")

include(":lib-system-customization")
include(":lib-system-customization-xposed")

include(":lib-system-keyguard")
include(":lib-system-keyguard-xposed")

include(":lib-system-systemui")
include(":lib-system-systemui-xposed")

include(":lib-system-sysui")
include(":lib-system-sysui-xposed")

include(":lib-system-battery")
include(":lib-system-battery-xposed")

include(":lib-system-bluetooth")
include(":lib-system-bluetooth-xposed")

include(":lib-system-display")
include(":lib-system-display-xposed")

include(":lib-system-fingerprint")

include(":lib-system-sound")
include(":lib-system-sound-xposed")

include(":lib-system-usb")

include(":lib-system-vibrator")

include(":lib-system-power")
include(":lib-system-power-xposed")

include(":lib-system-camera")

include(":lib-system-graphics")
include(":lib-system-graphics-xposed")

include(":lib-system-permissions")
include(":lib-system-permissions-xposed")

include(":lib-system-settings")
include(":lib-system-settings-xposed")

include(":lib-system-tweaks")
include(":lib-system-tweaks-xposed")

include(":lib-system-widget")
include(":lib-system-widget-xposed")

include(":lib-system-apps")
include(":lib-system-apps-xposed")

include(":lib-system-contacts")
include(":lib-system-contacts-xposed")

include(":lib-system-calendar")
include(":lib-system-calendar-xposed")

include(":lib-system-messaging")
include(":lib-system-messaging-xposed")

include(":lib-system-notifications")
include(":lib-system-notifications-xposed")

include(":lib-system-media")
include(":lib-system-media-xposed")

include(":lib-system-photos")
// include(":lib-system-photos-xposed")
//
// include(":lib-system-voice")
// include(":lib-system-voice-xposed")
//
// include(":lib-system-video")
// include(":lib-system-video-xposed")
//
// include(":lib-system-vpn")
//
// include(":lib-system-weather")
// include(":lib-system-weather-xposed")
//
// include(":lib-system-translation") // First instance of :lib-system-translation
// include(":lib-system-translation-xposed") // First instance of :lib-system-translation-xposed
//
// include(":lib-system-fonts")
// include(":lib-system-fonts-xposed")

// Duplicates for lib-system-translation already commented out above
// include(":lib-system-translation")
// include(":lib-system-translation-xposed")

// include("ksp-plugin") // Commented out as it was also reported missing in logs.
// If ksp-plugin is a local plugin and *does* exist, ensure its path is correctly configured.
// For now, assuming it's among the missing modules based on typical warning patterns.
