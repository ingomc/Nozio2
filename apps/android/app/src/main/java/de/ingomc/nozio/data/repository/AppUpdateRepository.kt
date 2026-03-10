package de.ingomc.nozio.data.repository

import de.ingomc.nozio.data.remote.GitHubReleaseApi
import de.ingomc.nozio.data.remote.GitHubReleaseAssetDto
import kotlin.math.sign

interface AppUpdateChecker {
    suspend fun checkForUpdate(currentVersionName: String): UpdateCheckResult
}

data class ApkAssetSelection(
    val name: String,
    val downloadUrl: String
)

data class AvailableRelease(
    val tagName: String,
    val title: String,
    val notesPreview: String,
    val htmlUrl: String,
    val apkAsset: ApkAssetSelection?
)

sealed interface UpdateCheckResult {
    data object UpToDate : UpdateCheckResult
    data class UpdateAvailable(val release: AvailableRelease) : UpdateCheckResult
}

class AppUpdateRepository(
    private val gitHubReleaseApi: GitHubReleaseApi,
    private val owner: String = "ingomc",
    private val repo: String = "Nozio2"
) : AppUpdateChecker {

    override suspend fun checkForUpdate(currentVersionName: String): UpdateCheckResult {
        val latestRelease = gitHubReleaseApi.getLatestRelease(owner = owner, repo = repo)
        val normalizedLatestTag = normalizeVersion(latestRelease.tagName)
        val normalizedCurrentVersion = normalizeVersion(currentVersionName)
        val latestIsNewer = compareSemanticVersions(normalizedLatestTag, normalizedCurrentVersion)
            ?.let { it > 0 }
            ?: (normalizedLatestTag != normalizedCurrentVersion)

        if (!latestIsNewer) {
            return UpdateCheckResult.UpToDate
        }

        val release = AvailableRelease(
            tagName = latestRelease.tagName,
            title = latestRelease.name?.trim().takeUnless { it.isNullOrBlank() } ?: latestRelease.tagName,
            notesPreview = buildNotesPreview(latestRelease.body),
            htmlUrl = latestRelease.htmlUrl,
            apkAsset = pickApkAsset(latestRelease.assets)
        )
        return UpdateCheckResult.UpdateAvailable(release)
    }

    fun pickApkAsset(assets: List<GitHubReleaseAssetDto>): ApkAssetSelection? {
        val apkAssets = assets.filter { it.name.lowercase().endsWith(".apk") }
        if (apkAssets.isEmpty()) return null

        val preferred = apkAssets.firstOrNull { it.name.contains("universal", ignoreCase = true) } ?: apkAssets.first()
        return ApkAssetSelection(
            name = preferred.name,
            downloadUrl = preferred.browserDownloadUrl
        )
    }

    private fun buildNotesPreview(body: String?): String {
        val cleaned = body
            ?.lineSequence()
            ?.map { sanitizeMarkdownLine(it) }
            ?.filter { it.isNotBlank() }
            ?.joinToString(separator = "\n")
            .orEmpty()

        if (cleaned.isBlank()) return "Keine Release-Notizen vorhanden."
        return if (cleaned.length <= RELEASE_NOTES_PREVIEW_MAX_LENGTH) {
            cleaned
        } else {
            cleaned.take(RELEASE_NOTES_PREVIEW_MAX_LENGTH - 3).trimEnd() + "..."
        }
    }

    private fun sanitizeMarkdownLine(rawLine: String): String {
        var line = rawLine.trim()
        if (line.isBlank()) return ""
        if (HORIZONTAL_RULE_REGEX.matches(line)) return ""

        line = line
            .replace(HEADING_PREFIX_REGEX, "")
            .replace(BLOCKQUOTE_PREFIX_REGEX, "")
            .replace(UNORDERED_LIST_PREFIX_REGEX, "• ")
            .replace(ORDERED_LIST_PREFIX_REGEX, "• ")
            .replace(MARKDOWN_LINK_REGEX, "$1")
            .replace("`", "")
            .replace(BOLD_ASTERISK_REGEX, "$1")
            .replace(BOLD_UNDERSCORE_REGEX, "$1")
            .replace(ITALIC_ASTERISK_REGEX, "$1")
            .replace(ITALIC_UNDERSCORE_REGEX, "$1")
            .replace(GITHUB_ATTRIBUTION_SUFFIX_REGEX, "")
            .trim()

        return line.replace(Regex("\\s+"), " ")
    }

    internal fun compareSemanticVersions(candidate: String, current: String): Int? {
        val candidateVersion = parseSemanticVersion(candidate) ?: return null
        val currentVersion = parseSemanticVersion(current) ?: return null
        val majorComparison = candidateVersion.major.compareTo(currentVersion.major)
        if (majorComparison != 0) return majorComparison.sign
        val minorComparison = candidateVersion.minor.compareTo(currentVersion.minor)
        if (minorComparison != 0) return minorComparison.sign
        return candidateVersion.patch.compareTo(currentVersion.patch).sign
    }

    private fun parseSemanticVersion(value: String): SemanticVersion? {
        val normalized = normalizeVersion(value)
            .substringBefore('-')
            .substringBefore('+')
        val match = SEMVER_REGEX.matchEntire(normalized) ?: return null
        return SemanticVersion(
            major = match.groupValues[1].toInt(),
            minor = match.groupValues[2].toInt(),
            patch = match.groupValues[3].toInt()
        )
    }

    private fun normalizeVersion(value: String): String = value.trim().removePrefix("v").removePrefix("V")

    private data class SemanticVersion(
        val major: Int,
        val minor: Int,
        val patch: Int
    )

    companion object {
        private const val RELEASE_NOTES_PREVIEW_MAX_LENGTH = 280
        private val SEMVER_REGEX = Regex("^(\\d+)\\.(\\d+)\\.(\\d+)$")
        private val HEADING_PREFIX_REGEX = Regex("^#{1,6}\\s*")
        private val BLOCKQUOTE_PREFIX_REGEX = Regex("^>\\s*")
        private val UNORDERED_LIST_PREFIX_REGEX = Regex("^[-*+]\\s+")
        private val ORDERED_LIST_PREFIX_REGEX = Regex("^\\d+[.)]\\s+")
        private val HORIZONTAL_RULE_REGEX = Regex("^([-*_])\\1{2,}$")
        private val MARKDOWN_LINK_REGEX = Regex("\\[([^\\]]+)]\\(([^)]+)\\)")
        private val BOLD_ASTERISK_REGEX = Regex("\\*\\*(.+?)\\*\\*")
        private val BOLD_UNDERSCORE_REGEX = Regex("__(.+?)__")
        private val ITALIC_ASTERISK_REGEX = Regex("(?<!\\*)\\*(.+?)\\*(?!\\*)")
        private val ITALIC_UNDERSCORE_REGEX = Regex("(?<!_)_(.+?)_(?!_)")
        private val GITHUB_ATTRIBUTION_SUFFIX_REGEX = Regex("\\s+by\\s+@[\\w-]+\\s+in\\s+.+$", RegexOption.IGNORE_CASE)
    }
}
