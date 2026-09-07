package com.example.api

import retrofit2.Response
import retrofit2.http.*

// ─────────────────────────────────────────────
// GENERIC API RESPONSE ENVELOPE
// ─────────────────────────────────────────────
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null
)

data class ApiError(
    val code: String,
    val message: String,
    val details: List<Any>? = null
)

// ─────────────────────────────────────────────
// AUTH & PROFILE MODELS
// ─────────────────────────────────────────────
data class SignupRequest(
    val email: String,
    val password: String,
    val preferredName: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthData(
    val user: UserDto,
    val accessToken: String
)

data class UserDto(
    val id: String,
    val email: String,
    val displayName: String? = null
)

data class ProfileDto(
    val user_id: String,
    val display_name: String? = null,
    val date_of_birth: String? = null,
    val timezone: String? = null,
    val onboarding_status: String = "NOT_STARTED"
)

// ─────────────────────────────────────────────
// TASK MODELS
// ─────────────────────────────────────────────
data class TaskDto(
    val id: String,
    val user_id: String,
    val title: String,
    val status: String,
    val priority: String = "MEDIUM",
    val energy_tag: String? = null,
    val due_at: String? = null,
    val completed_at: String? = null,
    val version: Int = 1,
    val created_at: String,
    val updated_at: String
)

data class CreateTaskRequest(
    val title: String,
    val priority: String? = "MEDIUM",
    val energyTag: String? = null,
    val dueAt: String? = null
)

data class UpdateTaskRequest(
    val title: String? = null,
    val priority: String? = null,
    val status: String? = null,
    val dueAt: String? = null
)

// ─────────────────────────────────────────────
// DAILY PLAN & LOCK TOMORROW MODELS
// ─────────────────────────────────────────────
data class DailyPlanDto(
    val id: String,
    val user_id: String,
    val plan_date: String,
    val status: String,
    val locked_at: String? = null,
    val items: List<DailyPlanItemDto> = emptyList(),
    val version: Int = 1
)

data class DailyPlanItemDto(
    val id: String,
    val daily_plan_id: String,
    val item_type: String,
    val reference_id: String? = null,
    val status: String = "PLANNED",
    val task_title: String? = null,
    val task_priority: String? = null
)

data class LockPlanRequest(
    val taskIdsInOrder: List<String>
)

// ─────────────────────────────────────────────
// OFFLINE SYNC MODELS
// ─────────────────────────────────────────────
data class SyncPushRequest(
    val changes: List<SyncChangeDto>
)

data class SyncChangeDto(
    val operationId: String,
    val entity: String, // "task", "daily_plan"
    val action: String, // "INSERT", "UPDATE", "DELETE"
    val id: String,
    val version: Int? = 1,
    val updatedAt: String,
    val data: Map<String, Any?>
)

data class SyncPushResponseData(
    val committedOperations: List<String>,
    val conflicts: List<Any>,
    val serverTime: String
)

data class SyncPullResponseData(
    val tasks: List<TaskDto>,
    val plans: List<DailyPlanDto>,
    val serverTime: String
)

// ─────────────────────────────────────────────
// AURA CONTEXT ENGINE QUERY
// ─────────────────────────────────────────────
data class AuraAskRequest(
    val prompt: String,
    val clientLocalTime: String? = null
)

data class AuraAskResponseData(
    val message: String,
    val proposedAction: Any? = null,
    val suggestedQuickPrompts: List<String> = emptyList()
)

// ─────────────────────────────────────────────
// RETROFIT SERVICE INTERFACE
// ─────────────────────────────────────────────
interface AuraApiService {

    // Auth
    @POST("auth/signup")
    suspend fun signup(@Body request: SignupRequest): Response<ApiResponse<AuthData>>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthData>>

    @GET("auth/me")
    suspend fun getMe(): Response<ApiResponse<Map<String, Any>>>

    // Profile
    @GET("profile")
    suspend fun getProfile(): Response<ApiResponse<ProfileDto>>

    @PUT("profile")
    suspend fun updateProfile(@Body profile: Map<String, Any?>): Response<ApiResponse<ProfileDto>>

    // Tasks
    @GET("tasks")
    suspend fun listTasks(
        @Query("status") status: String? = null,
        @Query("date") date: String? = null
    ): Response<ApiResponse<List<TaskDto>>>

    @POST("tasks")
    suspend fun createTask(@Body request: CreateTaskRequest): Response<ApiResponse<TaskDto>>

    @GET("tasks/{id}")
    suspend fun getTask(@Path("id") id: String): Response<ApiResponse<TaskDto>>

    @POST("tasks/{id}/complete")
    suspend fun completeTask(@Path("id") id: String): Response<ApiResponse<TaskDto>>

    @PATCH("tasks/{id}")
    suspend fun updateTask(@Path("id") id: String, @Body request: UpdateTaskRequest): Response<ApiResponse<TaskDto>>

    @DELETE("tasks/{id}")
    suspend fun deleteTask(@Path("id") id: String): Response<ApiResponse<Map<String, Boolean>>>

    // Daily Plans & Locking
    @GET("daily-plans/{date}")
    suspend fun getDailyPlan(@Path("date") date: String): Response<ApiResponse<DailyPlanDto>>

    @POST("daily-plans/{id}/lock")
    suspend fun lockPlan(@Path("id") id: String, @Body request: LockPlanRequest): Response<ApiResponse<DailyPlanDto>>

    // Offline Synchronization
    @POST("sync/push")
    suspend fun syncPush(@Body request: SyncPushRequest): Response<ApiResponse<SyncPushResponseData>>

    @GET("sync/pull")
    suspend fun syncPull(@Query("since") since: String? = null): Response<ApiResponse<SyncPullResponseData>>

    // Context Engine
    @POST("aura/ask")
    suspend fun askAura(@Body request: AuraAskRequest): Response<ApiResponse<AuraAskResponseData>>
}
