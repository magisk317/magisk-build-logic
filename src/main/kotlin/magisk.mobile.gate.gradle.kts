import com.android.build.api.dsl.ApplicationExtension

// Mobile automation activation gate: distributions delivered through Google
// Play ship the gate off -- the app publishes an always-allowed snapshot at
// startup and gate readers short-circuit to open -- while sideload flavors
// keep the token/Telegram activation flow.
pluginManager.withPlugin("com.android.application") {
    extensions.configure<ApplicationExtension> {
        productFlavors.matching { it.name == "play" }.configureEach {
            buildConfigField("boolean", "ENABLE_MOBILE_ENTITLEMENT", "false")
        }
        productFlavors.matching { it.name != "play" }.configureEach {
            buildConfigField("boolean", "ENABLE_MOBILE_ENTITLEMENT", "true")
        }
    }
}
