package com.strakk.shared.domain.usecase

import com.strakk.shared.fixtures.FakeProfileRepository
import com.strakk.shared.fixtures.TestFixtures
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreateProfileUseCaseTest {

    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var useCase: CreateProfileUseCase

    @BeforeTest
    fun setUp() {
        profileRepository = FakeProfileRepository()
        useCase = CreateProfileUseCase(profileRepository)
    }

    @Test
    fun validDataCallsRepoAndReturnsProfile() = runTest {
        val result = useCase(TestFixtures.defaultOnboardingData)

        assertTrue(result.isSuccess)
        assertEquals(TestFixtures.defaultUserProfile, result.getOrNull())
        assertEquals(1, profileRepository.createProfileCalls.size)
        assertEquals(TestFixtures.defaultOnboardingData, profileRepository.createProfileCalls.first())
    }

    @Test
    fun repoThrowsReturnsFailure() = runTest {
        val networkError = RuntimeException("Network error")
        profileRepository.shouldThrow = networkError

        val result = useCase(TestFixtures.defaultOnboardingData)

        assertTrue(result.isFailure)
        assertEquals(networkError, result.exceptionOrNull())
    }
}
