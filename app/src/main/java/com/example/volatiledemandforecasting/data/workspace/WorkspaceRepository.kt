package com.example.volatiledemandforecasting.data.workspace

import com.example.volatiledemandforecasting.core.network.apiCall
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

object Roles {
    const val OWNER = "OWNER"
    const val MANAGER = "MANAGER"
    const val VIEWER = "VIEWER"

    /** The read-only demo workspace. */
    const val REFERENCE = "REFERENCE"
}

@Serializable
data class WorkspaceDto(
    val id: String,
    val name: String,
    val role: String,
    val isReference: Boolean = false,
)

@Serializable
data class StoreDto(
    val id: String,
    val workspaceId: String,
    val code: String,
    val name: String,
    val country: String,
    val subdivision: String? = null,
    val timezone: String,
    val currency: String,
    val isReference: Boolean = false,
    val historyStart: String? = null,
    val historyEnd: String? = null,
)

@Serializable
data class CreateWorkspaceRequest(val name: String)

@Serializable
data class CreateStoreRequest(
    val code: String,
    val name: String,
    /** ISO-2. Used for public holidays. */
    val country: String,
    val subdivision: String? = null,
    /** IANA, e.g. Africa/Nairobi. */
    val timezone: String,
    val currency: String,
)

data class WorkspaceWithStores(val workspace: WorkspaceDto, val stores: List<StoreDto>)

interface WorkspaceApi {
    @GET("v1/workspaces")
    suspend fun workspaces(): List<WorkspaceDto>

    @GET("v1/workspaces/{workspaceId}/stores")
    suspend fun stores(@Path("workspaceId") workspaceId: String): List<StoreDto>

    @POST("v1/workspaces")
    suspend fun createWorkspace(@Body request: CreateWorkspaceRequest): WorkspaceDto

    @POST("v1/workspaces/{workspaceId}/stores")
    suspend fun createStore(@Path("workspaceId") workspaceId: String, @Body request: CreateStoreRequest): StoreDto

    @GET("v1/stores/{storeId}")
    suspend fun store(@Path("storeId") storeId: String): StoreDto
}

class WorkspaceRepository(private val api: WorkspaceApi, private val json: Json) {

    /** The demo workspace comes first, then the user's own (api.md). */
    suspend fun workspacesWithStores(): List<WorkspaceWithStores> = coroutineScope {
        apiCall(json) { api.workspaces() }
            .map { workspace -> async { WorkspaceWithStores(workspace, apiCall(json) { api.stores(workspace.id) }) } }
            .map { it.await() }
    }

    suspend fun createWorkspace(name: String): WorkspaceDto = apiCall(json) { api.createWorkspace(CreateWorkspaceRequest(name)) }

    suspend fun createStore(workspaceId: String, request: CreateStoreRequest): StoreDto =
        apiCall(json) { api.createStore(workspaceId, request) }

    suspend fun store(storeId: String): StoreDto = apiCall(json) { api.store(storeId) }
}
