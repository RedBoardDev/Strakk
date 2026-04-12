package com.strakk.shared

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoModifier
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.ext.list.withoutModifier
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test

/**
 * Architecture enforcement tests using Konsist.
 *
 * These tests run in CI to catch layer boundary violations
 * before they reach code review.
 */
class ArchitectureTests {

    // =========================================================================
    // Test 1: Domain layer has no framework imports
    // =========================================================================

    @Test
    fun `domain layer has no framework imports`() {
        Konsist.scopeFromModule("shared")
            .files
            .filter { it.resideInPackage("com.strakk.shared.domain..") }
            .assertFalse { file ->
                file.hasImport { import ->
                    import.name.startsWith("com.strakk.shared.data") ||
                        import.name.startsWith("com.strakk.shared.presentation") ||
                        import.name.startsWith("io.ktor") ||
                        import.name.startsWith("io.github.jan.supabase") ||
                        import.name.startsWith("kotlinx.serialization") ||
                        import.name.startsWith("android.") ||
                        import.name.startsWith("androidx.") ||
                        import.name.startsWith("platform.Foundation") ||
                        import.name.startsWith("org.koin")
                }
            }
    }

    // =========================================================================
    // Test 2: Domain only imports allowed packages
    // =========================================================================

    @Test
    fun `domain only imports kotlin, kotlinx coroutines, and kotlinx datetime`() {
        Konsist.scopeFromModule("shared")
            .files
            .filter { it.resideInPackage("com.strakk.shared.domain..") }
            .assertTrue { file ->
                file.imports.all { import ->
                    import.name.startsWith("kotlin.") ||
                        import.name.startsWith("kotlinx.coroutines") ||
                        import.name.startsWith("kotlinx.datetime") ||
                        import.name.startsWith("com.strakk.shared.domain")
                }
            }
    }

    // =========================================================================
    // Test 3: Data layer classes are internal
    // =========================================================================

    @Test
    fun `data layer classes are internal`() {
        Konsist.scopeFromModule("shared")
            .classes()
            .filter { it.resideInPackage("com.strakk.shared.data..") }
            .withoutModifier(KoModifier.DATA) // Data classes checked separately
            .assertTrue { it.hasInternalModifier }
    }

    @Test
    fun `data layer data classes are internal`() {
        Konsist.scopeFromModule("shared")
            .classes()
            .filter { it.resideInPackage("com.strakk.shared.data..") }
            .filter { it.hasModifier(KoModifier.DATA) }
            .assertTrue { it.hasInternalModifier }
    }

    @Test
    fun `data layer functions are internal`() {
        Konsist.scopeFromModule("shared")
            .functions()
            .filter { it.resideInPackage("com.strakk.shared.data..") }
            .filter { !it.isLocal } // Exclude local functions inside class bodies
            .assertTrue { it.hasInternalModifier || it.hasPrivateModifier }
    }

    // =========================================================================
    // Test 4: Use cases have single invoke operator
    // =========================================================================

    @Test
    fun `use cases have operator invoke function`() {
        Konsist.scopeFromModule("shared")
            .classes()
            .filter { it.resideInPackage("com.strakk.shared.domain.usecase..") }
            .withNameEndingWith("UseCase")
            .assertTrue { useCase ->
                useCase.hasFunction { function ->
                    function.name == "invoke" && function.hasOperatorModifier
                }
            }
    }

    @Test
    fun `use cases have only one public function`() {
        Konsist.scopeFromModule("shared")
            .classes()
            .filter { it.resideInPackage("com.strakk.shared.domain.usecase..") }
            .withNameEndingWith("UseCase")
            .assertTrue { useCase ->
                useCase.functions()
                    .filter { !it.hasPrivateModifier }
                    .size == 1
            }
    }

    // =========================================================================
    // Test 5: Repository implementations implement domain interfaces
    // =========================================================================

    @Test
    fun `repository implementations implement domain interfaces`() {
        Konsist.scopeFromModule("shared")
            .classes()
            .filter { it.resideInPackage("com.strakk.shared.data.repository..") }
            .withNameEndingWith("RepositoryImpl")
            .assertTrue { repoImpl ->
                repoImpl.hasParentInterface { parentInterface ->
                    parentInterface.name.endsWith("Repository")
                }
            }
    }

    @Test
    fun `repository interfaces are in domain layer`() {
        Konsist.scopeFromModule("shared")
            .interfaces()
            .withNameEndingWith("Repository")
            .assertTrue { it.resideInPackage("com.strakk.shared.domain.repository..") }
    }

    // =========================================================================
    // Test 6: Presentation layer does not import data layer
    // =========================================================================

    @Test
    fun `presentation layer does not import data layer`() {
        Konsist.scopeFromModule("shared")
            .files
            .filter { it.resideInPackage("com.strakk.shared.presentation..") }
            .assertFalse { file ->
                file.hasImport { import ->
                    import.name.startsWith("com.strakk.shared.data") ||
                        import.name.startsWith("io.ktor") ||
                        import.name.startsWith("io.github.jan.supabase") ||
                        import.name.startsWith("kotlinx.serialization")
                }
            }
    }

    // =========================================================================
    // Test 7: ViewModels depend on use cases, not repositories
    // =========================================================================

    @Test
    fun `ViewModels do not depend on repositories directly`() {
        Konsist.scopeFromModule("shared")
            .classes()
            .filter { it.resideInPackage("com.strakk.shared.presentation..") }
            .withNameEndingWith("ViewModel")
            .assertTrue { viewModel ->
                viewModel.constructorParameters.none { param ->
                    param.type.name.endsWith("Repository")
                }
            }
    }

    // =========================================================================
    // Test 8: Sealed interfaces (not sealed classes) for state
    // =========================================================================

    @Test
    fun `UI state hierarchies use sealed interface not sealed class`() {
        Konsist.scopeFromModule("shared")
            .classes()
            .filter { it.resideInPackage("com.strakk.shared.presentation..") }
            .filter { it.hasModifier(KoModifier.SEALED) }
            .withNameEndingWith("UiState")
            .assertTrue {
                // This will fail if any sealed CLASS named *UiState exists
                // since we're filtering classes — sealed interfaces won't appear here
                false // Forces failure: sealed classes with UiState suffix should not exist
            }
    }

    @Test
    fun `UI state sealed interfaces exist in presentation`() {
        Konsist.scopeFromModule("shared")
            .interfaces()
            .filter { it.resideInPackage("com.strakk.shared.presentation..") }
            .withNameEndingWith("UiState")
            .assertTrue { it.hasModifier(KoModifier.SEALED) }
    }
}
