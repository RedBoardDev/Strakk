package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.DomainError
import com.strakk.shared.fixtures.FakeAuthRepository
import com.strakk.shared.fixtures.TestFixtures
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SignInUseCaseTest {

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var useCase: SignInUseCase

    @BeforeTest
    fun setUp() {
        authRepository = FakeAuthRepository()
        useCase = SignInUseCase(authRepository)
    }

    @Test
    fun validCredentialsCallsRepoAndReturnsSuccess() = runTest {
        val result = useCase(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD)

        assertTrue(result.isSuccess)
        assertEquals(1, authRepository.signInCalls.size)
        assertEquals(TestFixtures.VALID_EMAIL to TestFixtures.VALID_PASSWORD, authRepository.signInCalls.first())
    }

    @Test
    fun emptyEmailReturnsValidationError() = runTest {
        val result = useCase(TestFixtures.EMAIL_EMPTY, TestFixtures.VALID_PASSWORD)

        assertTrue(result.isFailure)
        assertIs<DomainError.ValidationError>(result.exceptionOrNull())
        assertEquals(0, authRepository.signInCalls.size)
    }

    @Test
    fun blankEmailReturnsValidationError() = runTest {
        val result = useCase(TestFixtures.EMAIL_BLANK, TestFixtures.VALID_PASSWORD)

        assertTrue(result.isFailure)
        assertIs<DomainError.ValidationError>(result.exceptionOrNull())
    }

    @Test
    fun emailWithoutAtReturnsValidationError() = runTest {
        val result = useCase(TestFixtures.EMAIL_NO_AT, TestFixtures.VALID_PASSWORD)

        assertTrue(result.isFailure)
        assertIs<DomainError.ValidationError>(result.exceptionOrNull())
    }

    @Test
    fun emailWithoutDotReturnsValidationError() = runTest {
        val result = useCase(TestFixtures.EMAIL_NO_DOT, TestFixtures.VALID_PASSWORD)

        assertTrue(result.isFailure)
        assertIs<DomainError.ValidationError>(result.exceptionOrNull())
    }

    @Test
    fun blankPasswordReturnsValidationError() = runTest {
        val result = useCase(TestFixtures.VALID_EMAIL, "")

        assertTrue(result.isFailure)
        assertIs<DomainError.ValidationError>(result.exceptionOrNull())
        assertEquals(0, authRepository.signInCalls.size)
    }

    @Test
    fun repoThrowsReturnsFailure() = runTest {
        val networkError = RuntimeException("Network unavailable")
        authRepository.shouldThrow = networkError

        val result = useCase(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD)

        assertTrue(result.isFailure)
        assertEquals(networkError, result.exceptionOrNull())
    }

    @Test
    fun emailIsTrimmedBeforeCallingRepo() = runTest {
        useCase(TestFixtures.VALID_EMAIL_TRIMMED, TestFixtures.VALID_PASSWORD)

        assertEquals(TestFixtures.VALID_EMAIL, authRepository.signInCalls.first().first)
    }
}
