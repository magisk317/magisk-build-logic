internal sealed interface AndroidSdkSpec {
    data class ApiLevel(val value: Int) : AndroidSdkSpec

    data class Preview(val codename: String) : AndroidSdkSpec
}

internal fun parseAndroidSdkSpec(rawValue: String): AndroidSdkSpec {
    val value = rawValue.trim().removePrefix("android-")
    require(value.isNotEmpty()) { "Android SDK value must not be blank" }
    return value.toIntOrNull()?.let(AndroidSdkSpec::ApiLevel)
        ?: AndroidSdkSpec.Preview(value)
}
