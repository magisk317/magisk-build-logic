internal fun releaseBaseName(
    artifactBaseName: String,
    versionName: String,
): String {
    val base = artifactBaseName.trim().ifEmpty { "app" }.replace("\\s+".toRegex(), "_")
    val version = versionName.trim().replace("\\s+".toRegex(), "_")
    return "${base}_v$version"
}

internal fun releaseApkName(
    artifactBaseName: String,
    versionName: String,
    buildType: String,
    abiSuffix: String,
    flavorName: String = "",
): String {
    val flavorTag = when (flavorName) {
        "", "github", "githubNoE2ee" -> ""
        "githubWithE2ee" -> "e2ee_"
        else -> "${flavorName}_"
    }
    return "${abiSuffix}_${flavorTag}${releaseBaseName(artifactBaseName, versionName)}_${buildType}.apk"
}
