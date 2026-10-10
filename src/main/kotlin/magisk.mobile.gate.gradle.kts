import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.ProductFlavor

// Mobile automation activation gate: distributions delivered through Google
// Play ship the gate off -- the app publishes an always-allowed snapshot at
// startup and gate readers short-circuit to open -- while sideload flavors
// keep the token/Telegram activation flow.
//
// The flag reaches application AND library modules. The hook process evaluates
// the gate from the :runtime module (HookPrefsReader), which never sees the app
// module's BuildConfig, so a flag confined to the app leaves the hook boundary
// enforcing a signed lease that play builds never issue.
//
// The channel travels with the flag so every process can report which
// distribution it belongs to without repeating the flavor-to-channel mapping.

private fun ProductFlavor.applyGateFields() {
    if (name == "play") {
        buildConfigField("boolean", "ENABLE_MOBILE_ENTITLEMENT", "false")
        buildConfigField("String", "MOBILE_ENTITLEMENT_CHANNEL", "\"play\"")
    } else {
        buildConfigField("boolean", "ENABLE_MOBILE_ENTITLEMENT", "true")
        buildConfigField("String", "MOBILE_ENTITLEMENT_CHANNEL", "\"sideload\"")
    }
}

pluginManager.withPlugin("com.android.application") {
    extensions.configure<ApplicationExtension> {
        productFlavors.configureEach { applyGateFields() }
    }
}

pluginManager.withPlugin("com.android.library") {
    extensions.configure<LibraryExtension> {
        productFlavors.configureEach { applyGateFields() }
    }
}
