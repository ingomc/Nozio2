package de.ingomc.nozio.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String,
    val assets: List<GitHubReleaseAssetDto> = emptyList()
)

@Serializable
data class GitHubReleaseAssetDto(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String
)
