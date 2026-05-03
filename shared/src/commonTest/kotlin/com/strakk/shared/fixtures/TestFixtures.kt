package com.strakk.shared.fixtures

import com.strakk.shared.domain.model.AuthStatus
import com.strakk.shared.domain.model.NutritionGoals
import com.strakk.shared.domain.model.OnboardingData
import com.strakk.shared.domain.model.SubscriptionState
import com.strakk.shared.domain.model.UserProfile
import com.strakk.shared.domain.repository.AuthRepository
import com.strakk.shared.domain.repository.ProfileRepository
import com.strakk.shared.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Reusable test data shared across all test suites.
 */
object TestFixtures {

    const val VALID_EMAIL = "user@example.com"
    const val VALID_EMAIL_TRIMMED = "  user@example.com  "
    const val EMAIL_NO_AT = "userexample.com"
    const val EMAIL_NO_DOT = "user@examplecom"
    const val EMAIL_EMPTY = ""
    const val EMAIL_BLANK = "   "

    const val VALID_PASSWORD = "secret123"
    const val SHORT_PASSWORD = "abc"

    val defaultOnboardingData = OnboardingData(
        weightKg = 75.0,
        heightCm = 175,
        birthDate = null,
        biologicalSex = null,
        fitnessGoal = null,
        trainingFrequency = null,
        trainingTypes = emptySet(),
        trainingIntensity = null,
        dailyActivityLevel = null,
        proteinGoal = 150,
        calorieGoal = 2000,
        fatGoal = null,
        carbGoal = null,
        waterGoal = 2500,
    )

    val defaultUserProfile = UserProfile(
        id = "user-id-123",
        weightKg = 75.0,
        heightCm = 175,
        birthDate = null,
        biologicalSex = null,
        fitnessGoal = null,
        trainingFrequency = null,
        trainingTypes = emptySet(),
        trainingIntensity = null,
        dailyActivityLevel = null,
        proteinGoal = 150,
        calorieGoal = 2000,
        fatGoal = null,
        carbGoal = null,
        waterGoal = 2500,
        onboardingCompleted = true,
    )
}

/**
 * Fake AuthRepository for testing — avoids Mokkery version issues.
 */
class FakeAuthRepository : AuthRepository {
    val authStatusFlow = MutableSharedFlow<AuthStatus>(replay = 1)
    var signInCalls = mutableListOf<Pair<String, String>>()
    var signUpCalls = mutableListOf<Pair<String, String>>()
    var signOutCalled = false
    var currentEmail: String? = "user@example.com"
    var shouldThrow: Throwable? = null

    override fun observeSessionStatus(): Flow<AuthStatus> = authStatusFlow

    override suspend fun signIn(email: String, password: String) {
        shouldThrow?.let { throw it }
        signInCalls.add(email to password)
    }

    override suspend fun signUp(email: String, password: String) {
        shouldThrow?.let { throw it }
        signUpCalls.add(email to password)
    }

    override suspend fun signOut() {
        shouldThrow?.let { throw it }
        signOutCalled = true
    }

    override suspend fun getCurrentUserEmail(): String? = currentEmail

    override suspend fun resetPassword(email: String) {
        shouldThrow?.let { throw it }
    }
}

/**
 * Fake ProfileRepository for testing.
 */
class FakeProfileRepository : ProfileRepository {
    var profileExistsResult: Boolean = false
    var getProfileResult: UserProfile? = TestFixtures.defaultUserProfile
    var createProfileResult: UserProfile = TestFixtures.defaultUserProfile
    var updateProfileResult: UserProfile = TestFixtures.defaultUserProfile
    var shouldThrow: Throwable? = null
    val createProfileCalls = mutableListOf<OnboardingData>()
    val completeOnboardingCalls = mutableListOf<NutritionGoals>()
    val updateProfileCalls = mutableListOf<List<Any?>>()

    override suspend fun profileExists(): Boolean {
        shouldThrow?.let { throw it }
        return profileExistsResult
    }

    override suspend fun createProfile(data: OnboardingData): UserProfile {
        shouldThrow?.let { throw it }
        createProfileCalls.add(data)
        return createProfileResult
    }

    override suspend fun getProfile(): UserProfile? {
        shouldThrow?.let { throw it }
        return getProfileResult
    }

    override suspend fun updateProfile(
        proteinGoal: Int?,
        calorieGoal: Int?,
        waterGoal: Int?,
    ): UserProfile {
        shouldThrow?.let { throw it }
        updateProfileCalls.add(
            listOf(proteinGoal, calorieGoal, waterGoal)
        )
        return updateProfileResult
    }

    override suspend fun getHevyApiKey(): String? {
        shouldThrow?.let { throw it }
        return null
    }

    override suspend fun updateHevyApiKey(apiKey: String) {
        shouldThrow?.let { throw it }
    }

    override suspend fun completeOnboarding(goals: NutritionGoals): UserProfile {
        shouldThrow?.let { throw it }
        completeOnboardingCalls.add(goals)
        return createProfileResult
    }

    val profileFlow = MutableStateFlow(getProfileResult)

    override fun observeProfile(): Flow<UserProfile?> = profileFlow

    override fun clearCache() {
        profileFlow.value = null
    }
}

/**
 * Fake SubscriptionRepository for testing. Supports pushing arbitrary states.
 */
class FakeSubscriptionRepository : SubscriptionRepository {
    private val stateFlow = MutableStateFlow<SubscriptionState>(SubscriptionState.Free)

    fun emit(state: SubscriptionState) {
        stateFlow.value = state
    }

    override fun observeState(): Flow<SubscriptionState> = stateFlow
    override suspend fun getState(): SubscriptionState = stateFlow.value
    override suspend fun refreshState() = Unit
}
