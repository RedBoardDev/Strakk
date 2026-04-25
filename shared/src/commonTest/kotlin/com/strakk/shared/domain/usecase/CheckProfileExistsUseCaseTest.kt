package com.strakk.shared.domain.usecase

import com.strakk.shared.fixtures.FakeProfileRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CheckProfileExistsUseCaseTest {

    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var useCase: CheckProfileExistsUseCase

    @BeforeTest
    fun setUp() {
        profileRepository = FakeProfileRepository()
        useCase = CheckProfileExistsUseCase(profileRepository)
    }

    @Test
    fun profileExistsReturnsTrue() = runTest {
        profileRepository.profileExistsResult = true

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
    }

    @Test
    fun profileDoesNotExistReturnsFalse() = runTest {
        profileRepository.profileExistsResult = false

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrNull())
    }

    @Test
    fun repoThrowsReturnsFailure() = runTest {
        val dbError = RuntimeException("Database error")
        profileRepository.shouldThrow = dbError

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals(dbError, result.exceptionOrNull())
    }
}
