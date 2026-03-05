package de.ingomc.nozio.data.repository

import de.ingomc.nozio.data.remote.GitHubReleaseApi
import de.ingomc.nozio.data.remote.GitHubReleaseAssetDto
import de.ingomc.nozio.data.remote.GitHubReleaseDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateRepositoryTest {

    @Test
    fun checkForUpdate_detectsNewerSemverRelease() = runTest {
        val repository = AppUpdateRepository(
            gitHubReleaseApi = FakeGitHubReleaseApi(
                GitHubReleaseDto(
                    tagName = "v0.5.2",
                    name = "Nozio 0.5.2",
                    body = "Fixes and improvements",
                    htmlUrl = "https://github.com/ingomc/Nozio2/releases/tag/v0.5.2",
                    assets = listOf(
                        GitHubReleaseAssetDto(
                            name = "app-release-universal.apk",
                            browserDownloadUrl = "https://example.com/app-release-universal.apk"
                        )
                    )
                )
            )
        )

        val result = repository.checkForUpdate(currentVersionName = "0.5.1")

        assertTrue(result is UpdateCheckResult.UpdateAvailable)
    }

    @Test
    fun checkForUpdate_usesTagInequalityWhenSemverParsingFails() = runTest {
        val repository = AppUpdateRepository(
            gitHubReleaseApi = FakeGitHubReleaseApi(
                GitHubReleaseDto(
                    tagName = "release-2026-03-05",
                    name = "Nightly",
                    body = null,
                    htmlUrl = "https://github.com/ingomc/Nozio2/releases/tag/release-2026-03-05",
                    assets = emptyList()
                )
            )
        )

        val result = repository.checkForUpdate(currentVersionName = "0.5.1")

        assertTrue(result is UpdateCheckResult.UpdateAvailable)
    }

    @Test
    fun pickApkAsset_prefersUniversalApkThenFallsBackToFirstApk() {
        val repository = AppUpdateRepository(
            gitHubReleaseApi = FakeGitHubReleaseApi(
                GitHubReleaseDto(
                    tagName = "v0.5.1",
                    name = "Nozio 0.5.1",
                    body = null,
                    htmlUrl = "https://github.com/ingomc/Nozio2/releases/tag/v0.5.1",
                    assets = emptyList()
                )
            )
        )

        val selected = repository.pickApkAsset(
            listOf(
                GitHubReleaseAssetDto(
                    name = "app-arm64-v8a.apk",
                    browserDownloadUrl = "https://example.com/app-arm64-v8a.apk"
                ),
                GitHubReleaseAssetDto(
                    name = "app-release-universal.apk",
                    browserDownloadUrl = "https://example.com/app-release-universal.apk"
                )
            )
        )
        assertNotNull(selected)
        assertEquals("app-release-universal.apk", selected?.name)

        val fallback = repository.pickApkAsset(
            listOf(
                GitHubReleaseAssetDto(
                    name = "app-arm64-v8a.apk",
                    browserDownloadUrl = "https://example.com/app-arm64-v8a.apk"
                ),
                GitHubReleaseAssetDto(
                    name = "notes.txt",
                    browserDownloadUrl = "https://example.com/notes.txt"
                )
            )
        )
        assertEquals("app-arm64-v8a.apk", fallback?.name)

        val missing = repository.pickApkAsset(
            listOf(
                GitHubReleaseAssetDto(
                    name = "notes.txt",
                    browserDownloadUrl = "https://example.com/notes.txt"
                )
            )
        )
        assertEquals(null, missing)
    }

    private class FakeGitHubReleaseApi(
        private val release: GitHubReleaseDto
    ) : GitHubReleaseApi {
        override suspend fun getLatestRelease(owner: String, repo: String): GitHubReleaseDto = release
    }
}
