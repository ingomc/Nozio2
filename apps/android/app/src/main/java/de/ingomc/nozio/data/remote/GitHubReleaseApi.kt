package de.ingomc.nozio.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface GitHubReleaseApi {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GitHubReleaseDto
}
