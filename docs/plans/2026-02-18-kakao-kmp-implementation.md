# kakao-kmp Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Create `:kakao-kmp` KMP library module with Page Object (KmpScreen/KNode), Flaky Safety, and Allure DSL + JSON generation — all in commonMain with zero expect/actual.

**Architecture:** Single Gradle module `:kakao-kmp` as a KMP library targeting Android + iOS. Wraps Compose Multiplatform Test API with typed DSL. Allure reports generated via own JSON writer (kotlinx-serialization + kotlinx-io).

**Tech Stack:** Kotlin 2.3.0, Compose Multiplatform 1.10.0 (uiTest), kotlinx-serialization-json 1.6.3, kotlinx-io-core 0.6.0, kotlinx-datetime 0.6.1

**Design doc:** `docs/plans/2026-02-18-kakao-kmp-design.md`

---

## Task 1: Create Gradle module `:kakao-kmp`

**Files:**
- Create: `kakao-kmp/build.gradle.kts`
- Modify: `settings.gradle.kts` (add `include(":kakao-kmp")`)
- Modify: `gradle/libs.versions.toml` (add new dependency versions)
- Modify: `build.gradle.kts` (root — add `kotlinSerialization apply false`)

**Step 1: Add versions and libraries to version catalog**

In `gradle/libs.versions.toml`, add:

```toml
# Under [versions]
kotlinx-io = "0.6.0"
kotlinx-datetime = "0.6.1"

# Under [libraries]
kotlinx-io-core = { module = "org.jetbrains.kotlinx:kotlinx-io-core", version.ref = "kotlinx-io" }
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
```

Note: `kotlinx-serialization-json` reuses the existing `kotlinx-serialization = "1.6.3"` version ref — no new version entry needed.

**Step 2: Add `kotlinSerialization` plugin to root `build.gradle.kts`**

In `build.gradle.kts` (root), add alongside existing plugin declarations:

```kotlin
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false   // <-- add this line
}
```

**Step 3: Create `kakao-kmp/build.gradle.kts`**

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(compose.uiTest)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.io.core)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.serialization.json) // needed for AllureWriterTest deserialization
        }
    }

    // Global opt-in so library code and tests don't need per-file @OptIn annotations
    sourceSets.all {
        languageSettings.optIn("androidx.compose.ui.test.ExperimentalTestApi")
    }
}

android {
    namespace = "io.github.kakaokmp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
```

Key decisions:
- Uses `androidLibrary` plugin + `androidTarget()` because the consumer `:composeApp` uses `androidTarget()`. A library with `jvm()` would not resolve as a dependency for an Android target.
- iOS targets declared without `binaries.framework` — this is a library consumed via Gradle, not a standalone framework.
- Global `ExperimentalTestApi` opt-in avoids per-file annotation noise. Consumers should add the same opt-in (see Task 7).

**Step 4: Register module in settings**

In `settings.gradle.kts`, add:

```kotlin
include(":kakao-kmp")
```

**Step 5: Sync and verify the module compiles**

Run: `./gradlew :kakao-kmp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```
feat: create :kakao-kmp gradle module with KMP dependencies
```

---

## Task 2: FlakySafety — config and retry mechanism

**Files:**
- Create: `kakao-kmp/src/commonMain/kotlin/io/github/kakaokmp/safety/KakaoKmpConfig.kt`
- Create: `kakao-kmp/src/commonMain/kotlin/io/github/kakaokmp/safety/FlakySafetyTimeoutException.kt`
- Create: `kakao-kmp/src/commonMain/kotlin/io/github/kakaokmp/safety/FlakySafety.kt`
- Create: `kakao-kmp/src/commonTest/kotlin/io/github/kakaokmp/safety/FlakySafetyTest.kt`

**Step 1: Write the test**

```kotlin
package io.github.kakaokmp.safety

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FlakySafetyTest {

    @Test
    fun succeedsOnFirstAttempt() {
        var callCount = 0
        val result = flakySafelyPlain(timeoutMs = 1000) {
            callCount++
            "ok"
        }
        assertEquals("ok", result)
        assertEquals(1, callCount)
    }

    @Test
    fun retriesUntilSuccess() {
        var callCount = 0
        val result = flakySafelyPlain(timeoutMs = 5000) {
            callCount++
            if (callCount < 3) throw AssertionError("not yet")
            "ok"
        }
        assertEquals("ok", result)
        assertEquals(3, callCount)
    }

    @Test
    fun throwsNonRetryableImmediately() {
        var callCount = 0
        assertFailsWith<IllegalArgumentException> {
            flakySafelyPlain(timeoutMs = 5000) {
                callCount++
                throw IllegalArgumentException("bad arg")
            }
        }
        assertEquals(1, callCount)
    }

    @Test
    fun throwsTimeoutWithLastError() {
        val ex = assertFailsWith<FlakySafetyTimeoutException> {
            flakySafelyPlain(timeoutMs = 100) {
                throw AssertionError("always fails")
            }
        }
        assertEquals(100, ex.timeoutMs)
        assertEquals("always fails", ex.cause?.message)
    }

    @Test
    fun rejectsNonPositiveTimeout() {
        assertFailsWith<IllegalArgumentException> {
            flakySafelyPlain(timeoutMs = 0) { "never" }
        }
        assertFailsWith<IllegalArgumentException> {
            flakySafelyPlain(timeoutMs = -1) { "never" }
        }
    }

    @Test
    fun retriesSubclassOfAllowedException() {
        // NumberFormatException extends IllegalStateException on some platforms,
        // but let's use a custom subclass to be explicit.
        open class ParentException : AssertionError("parent")
        class ChildException : ParentException()

        var callCount = 0
        val result = flakySafelyPlain(
            timeoutMs = 5000,
            allowedExceptions = setOf(ParentException::class),
        ) {
            callCount++
            if (callCount < 2) throw ChildException()
            "ok"
        }
        assertEquals("ok", result)
        assertEquals(2, callCount)
    }
}
```

Note: `flakySafelyPlain` is a standalone version (no ComposeUiTest dependency) used for unit testing the retry logic. The `ComposeUiTest.flakySafely` extension delegates to the same algorithm and adds `waitForIdle()`.

**Step 2: Run test to verify it fails**

Run: `./gradlew :kakao-kmp:testDebugUnitTest --tests "io.github.kakaokmp.safety.FlakySafetyTest"`
Expected: FAIL — unresolved references

**Step 3: Implement KakaoKmpConfig**

```kotlin
package io.github.kakaokmp.safety

import kotlin.reflect.KClass

object KakaoKmpConfig {
    var defaultTimeoutMs: Long = 5_000L

    /**
     * Interval between retry attempts (milliseconds).
     * In ComposeUiTest.flakySafely, `waitForIdle()` is used instead (driven by the Compose clock).
     * This value is reserved for future non-Compose retry functions.
     */
    var defaultIntervalMs: Long = 500L

    var allowedExceptions: Set<KClass<out Throwable>> = setOf(
        AssertionError::class,
        IllegalStateException::class,
    )
}
```

**Step 4: Implement FlakySafetyTimeoutException**

```kotlin
package io.github.kakaokmp.safety

class FlakySafetyTimeoutException(
    val timeoutMs: Long,
    cause: Throwable,
) : AssertionError(
    "Flaky safety timeout after ${timeoutMs}ms. Last error: ${cause.message}",
    cause,
)
```

**Step 5: Implement FlakySafety**

```kotlin
package io.github.kakaokmp.safety

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * Standalone retry function with no Compose dependency.
 * Used for unit-testing the retry logic itself.
 *
 * Note: uses a spin-wait loop because `Thread.sleep()` is not available in Kotlin common.
 * This is acceptable for unit test scenarios only. For Compose UI tests, use
 * [ComposeUiTest.flakySafely] which calls `waitForIdle()` between retries.
 */
inline fun <T> flakySafelyPlain(
    timeoutMs: Long = KakaoKmpConfig.defaultTimeoutMs,
    allowedExceptions: Set<kotlin.reflect.KClass<out Throwable>> = KakaoKmpConfig.allowedExceptions,
    block: () -> T,
): T {
    require(timeoutMs > 0) { "timeoutMs must be positive, was $timeoutMs" }

    val mark = TimeSource.Monotonic.markNow()
    var lastError: Throwable? = null
    while (mark.elapsedNow() < timeoutMs.milliseconds) {
        try {
            return block()
        } catch (e: Throwable) {
            if (allowedExceptions.none { it.isInstance(e) }) throw e
            lastError = e
        }
    }
    throw FlakySafetyTimeoutException(timeoutMs, lastError!!)
}

/**
 * Compose-aware retry function. Calls [ComposeUiTest.waitForIdle] between attempts
 * to let the Compose framework advance recompositions and animations.
 */
@OptIn(ExperimentalTestApi::class)
inline fun <T> ComposeUiTest.flakySafely(
    timeoutMs: Long = KakaoKmpConfig.defaultTimeoutMs,
    allowedExceptions: Set<kotlin.reflect.KClass<out Throwable>> = KakaoKmpConfig.allowedExceptions,
    block: () -> T,
): T {
    require(timeoutMs > 0) { "timeoutMs must be positive, was $timeoutMs" }

    val mark = TimeSource.Monotonic.markNow()
    var lastError: Throwable? = null
    while (mark.elapsedNow() < timeoutMs.milliseconds) {
        try {
            return block()
        } catch (e: Throwable) {
            if (allowedExceptions.none { it.isInstance(e) }) throw e
            lastError = e
            waitForIdle()
        }
    }
    throw FlakySafetyTimeoutException(timeoutMs, lastError!!)
}
```

**Step 6: Run tests**

Run: `./gradlew :kakao-kmp:testDebugUnitTest --tests "io.github.kakaokmp.safety.FlakySafetyTest"`
Expected: ALL PASS

**Step 7: Commit**

```
feat(kakao-kmp): add FlakySafety retry mechanism with configurable timeout
```

---

## Task 3: MatcherBuilder — DSL for composing matchers

**Files:**
- Create: `kakao-kmp/src/commonMain/kotlin/io/github/kakaokmp/node/MatcherBuilder.kt`
- Create: `kakao-kmp/src/commonTest/kotlin/io/github/kakaokmp/node/MatcherBuilderTest.kt`

**Step 1: Write the test**

```kotlin
package io.github.kakaokmp.node

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertFailsWith

class MatcherBuilderTest {

    @Test
    fun buildsSingleMatcher() {
        val matcher = MatcherBuilder().apply {
            hasTestTag("my_tag")
        }.build()
        assertNotNull(matcher)
    }

    @Test
    fun combinesMultipleMatchers() {
        val matcher = MatcherBuilder().apply {
            hasTestTag("my_tag")
            hasText("Hello")
        }.build()
        assertNotNull(matcher)
    }

    @Test
    fun failsOnEmptyBuilder() {
        assertFailsWith<IllegalStateException> {
            MatcherBuilder().build()
        }
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :kakao-kmp:testDebugUnitTest --tests "io.github.kakaokmp.node.MatcherBuilderTest"`
Expected: FAIL

**Step 3: Implement MatcherBuilder**

```kotlin
package io.github.kakaokmp.node

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText

class MatcherBuilder {
    private val matchers = mutableListOf<SemanticsMatcher>()

    fun hasTestTag(tag: String) {
        matchers += hasTestTag(tag)
    }

    fun hasText(text: String, substring: Boolean = false, ignoreCase: Boolean = false) {
        matchers += hasText(text, substring = substring, ignoreCase = ignoreCase)
    }

    fun hasContentDescription(value: String, substring: Boolean = false, ignoreCase: Boolean = false) {
        matchers += hasContentDescription(value, substring = substring, ignoreCase = ignoreCase)
    }

    fun matcher(custom: SemanticsMatcher) {
        matchers += custom
    }

    internal fun build(): SemanticsMatcher {
        check(matchers.isNotEmpty()) { "MatcherBuilder requires at least one matcher" }
        return matchers.reduce { a, b -> a and b }
    }
}
```

**Step 4: Run tests**

Run: `./gradlew :kakao-kmp:testDebugUnitTest --tests "io.github.kakaokmp.node.MatcherBuilderTest"`
Expected: ALL PASS

**Step 5: Commit**

```
feat(kakao-kmp): add MatcherBuilder DSL for composing SemanticsMatcher
```

---

## Task 4: KNode — typed UI element with flaky-safe actions

**Files:**
- Create: `kakao-kmp/src/commonMain/kotlin/io/github/kakaokmp/node/KNode.kt`

**Step 1: Implement KNode**

```kotlin
package io.github.kakaokmp.node

import androidx.compose.ui.test.*
import io.github.kakaokmp.safety.flakySafely

@OptIn(ExperimentalTestApi::class)
class KNode(
    private val composeUiTest: ComposeUiTest,
    private val matcher: SemanticsMatcher,
) {
    private fun interaction(): SemanticsNodeInteraction =
        composeUiTest.onNode(matcher)

    // --- Actions ---

    fun click() = composeUiTest.flakySafely {
        interaction().performClick()
    }

    fun typeText(text: String) = composeUiTest.flakySafely {
        interaction().performTextInput(text)
    }

    fun clearText() = composeUiTest.flakySafely {
        interaction().performTextClearance()
    }

    fun scrollTo() = composeUiTest.flakySafely {
        interaction().performScrollTo()
    }

    // --- Assertions ---

    fun assertIsDisplayed() = composeUiTest.flakySafely {
        interaction().assertIsDisplayed()
    }

    fun assertIsNotDisplayed() = composeUiTest.flakySafely {
        interaction().assertIsNotDisplayed()
    }

    fun assertTextEquals(vararg expected: String, includeEditableText: Boolean = true) =
        composeUiTest.flakySafely {
            interaction().assertTextEquals(*expected, includeEditableText = includeEditableText)
        }

    fun assertTextContains(value: String, substring: Boolean = true, ignoreCase: Boolean = false) =
        composeUiTest.flakySafely {
            interaction().assertTextContains(value, substring = substring, ignoreCase = ignoreCase)
        }

    fun assertIsEnabled() = composeUiTest.flakySafely {
        interaction().assertIsEnabled()
    }

    fun assertIsNotEnabled() = composeUiTest.flakySafely {
        interaction().assertIsNotEnabled()
    }

    fun assertExists() = composeUiTest.flakySafely {
        interaction().assertExists()
    }

    fun assertDoesNotExist() = composeUiTest.flakySafely {
        interaction().assertDoesNotExist()
    }
}
```

No unit test for KNode directly — it is a thin wrapper. It will be integration-tested in Task 7 via `:composeApp` tests.

**Step 2: Verify compilation**

Run: `./gradlew :kakao-kmp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```
feat(kakao-kmp): add KNode with flaky-safe actions and assertions
```

---

## Task 5: KmpScreen + KmpScreenFactory — Page Object pattern

**Files:**
- Create: `kakao-kmp/src/commonMain/kotlin/io/github/kakaokmp/screen/KmpScreen.kt` (contains both `KmpScreen` and `KmpScreenFactory`)

Note: `KmpScreenFactory` is only 3 lines, so it is merged into the same file as `KmpScreen`.

**Step 1: Implement KmpScreen and KmpScreenFactory**

```kotlin
package io.github.kakaokmp.screen

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import io.github.kakaokmp.node.KNode
import io.github.kakaokmp.node.MatcherBuilder

@OptIn(ExperimentalTestApi::class)
abstract class KmpScreen<T : KmpScreen<T>>(
    internal val composeUiTest: ComposeUiTest,
) : SemanticsNodeInteractionsProvider by composeUiTest {

    fun KNode(matcherBlock: MatcherBuilder.() -> Unit): KNode =
        KNode(composeUiTest, MatcherBuilder().apply(matcherBlock).build())
}

/**
 * Companion-object base enabling `Screen { }` syntax inside [TestScope].
 *
 * Usage:
 * ```
 * class HomeScreen(test: ComposeUiTest) : KmpScreen<HomeScreen>(test) {
 *     companion object : KmpScreenFactory<HomeScreen>(::HomeScreen)
 * }
 * ```
 */
@OptIn(ExperimentalTestApi::class)
open class KmpScreenFactory<T : KmpScreen<T>>(
    internal val create: (ComposeUiTest) -> T,
)
```

**Step 2: Verify compilation**

Run: `./gradlew :kakao-kmp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```
feat(kakao-kmp): add KmpScreen page object and KmpScreenFactory
```

---

## Task 6: Allure model + lifecycle + writer + TestScope + kakaoTest

**Files:**
- Create: `kakao-kmp/src/commonMain/kotlin/io/github/kakaokmp/allure/model/AllureStatus.kt`
- Create: `kakao-kmp/src/commonMain/kotlin/io/github/kakaokmp/allure/model/AllureLabel.kt`
- Create: `kakao-kmp/src/commonMain/kotlin/io/github/kakaokmp/allure/model/AllureStatusDetails.kt`
- Create: `kakao-kmp/src/commonMain/kotlin/io/github/kakaokmp/allure/model/AllureStepResult.kt`
- Create: `kakao-kmp/src/commonMain/kotlin/io/github/kakaokmp/allure/model/AllureTestResult.kt`
- Create: `kakao-kmp/src/commonMain/kotlin/io/github/kakaokmp/allure/AllureLifecycle.kt`
- Create: `kakao-kmp/src/commonMain/kotlin/io/github/kakaokmp/allure/AllureWriter.kt`
- Create: `kakao-kmp/src/commonMain/kotlin/io/github/kakaokmp/dsl/TestScope.kt`
- Create: `kakao-kmp/src/commonMain/kotlin/io/github/kakaokmp/dsl/KakaoTest.kt`
- Create: `kakao-kmp/src/commonTest/kotlin/io/github/kakaokmp/allure/AllureLifecycleTest.kt`
- Create: `kakao-kmp/src/commonTest/kotlin/io/github/kakaokmp/allure/AllureWriterTest.kt`
- Create: `kakao-kmp/src/commonTest/kotlin/io/github/kakaokmp/dsl/InferTestNameTest.kt`

**Step 1: Write AllureLifecycle test**

```kotlin
package io.github.kakaokmp.allure

import io.github.kakaokmp.allure.model.AllureStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class AllureLifecycleTest {

    @Test
    fun collectsLabels() {
        val lifecycle = AllureLifecycle()
        lifecycle.startTest("myTest")
        lifecycle.addLabel("AS_ID", "TC-123")
        lifecycle.addLabel("feature", "Cards")
        lifecycle.finishTest(AllureStatus.PASSED)

        val result = lifecycle.buildResult()
        assertEquals("myTest", result.name)
        assertEquals(AllureStatus.PASSED, result.status)
        assertEquals(2, result.labels.size)
        assertEquals("TC-123", result.labels.first { it.name == "AS_ID" }.value)
    }

    @Test
    fun collectsSteps() {
        val lifecycle = AllureLifecycle()
        lifecycle.startTest("myTest")
        lifecycle.startStep("Step 1")
        lifecycle.stopStep(AllureStatus.PASSED)
        lifecycle.startStep("Step 2")
        lifecycle.stopStep(AllureStatus.FAILED)
        lifecycle.finishTest(AllureStatus.FAILED)

        val result = lifecycle.buildResult()
        assertEquals(2, result.steps.size)
        assertEquals("Step 1", result.steps[0].name)
        assertEquals(AllureStatus.PASSED, result.steps[0].status)
        assertEquals("Step 2", result.steps[1].name)
        assertEquals(AllureStatus.FAILED, result.steps[1].status)
    }

    @Test
    fun collectsNestedSteps() {
        val lifecycle = AllureLifecycle()
        lifecycle.startTest("myTest")
        lifecycle.startStep("Outer step")
        lifecycle.startStep("Inner step")
        lifecycle.stopStep(AllureStatus.PASSED)
        lifecycle.stopStep(AllureStatus.PASSED)
        lifecycle.finishTest(AllureStatus.PASSED)

        val result = lifecycle.buildResult()
        assertEquals(1, result.steps.size)
        assertEquals("Outer step", result.steps[0].name)
        assertEquals(1, result.steps[0].steps.size)
        assertEquals("Inner step", result.steps[0].steps[0].name)
        assertEquals(AllureStatus.PASSED, result.steps[0].steps[0].status)
    }

    @Test
    fun recordsTimestamps() {
        val lifecycle = AllureLifecycle()
        lifecycle.startTest("myTest")
        lifecycle.finishTest(AllureStatus.PASSED)

        val result = lifecycle.buildResult()
        assert(result.start > 0)
        assert(result.stop >= result.start)
    }
}
```

**Step 2: Write AllureWriter test**

```kotlin
package io.github.kakaokmp.allure

import io.github.kakaokmp.allure.model.*
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class AllureWriterTest {

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun writesResultAsJsonAndReadsBack() {
        val testUuid = Uuid.random().toString()
        val result = AllureTestResult(
            uuid = testUuid,
            name = "sampleTest",
            fullName = "sampleTest",
            status = AllureStatus.PASSED,
            start = 1000L,
            stop = 2000L,
            labels = listOf(AllureLabel("feature", "Test")),
            steps = listOf(
                AllureStepResult(
                    name = "Step 1",
                    status = AllureStatus.PASSED,
                    start = 1000L,
                    stop = 1500L,
                )
            ),
        )

        // Use build directory to avoid polluting the project root
        val tempDir = "build/test-allure-results"
        val originalOutputDir = AllureWriter.outputDir
        try {
            AllureWriter.outputDir = tempDir
            AllureWriter.write(result)

            // Read back and deserialize
            val filePath = Path(tempDir, "$testUuid-result.json")
            val content = SystemFileSystem.source(filePath).buffered().readString()
            val json = Json { ignoreUnknownKeys = true }
            val deserialized = json.decodeFromString<AllureTestResult>(content)

            assertEquals("sampleTest", deserialized.name)
            assertEquals(AllureStatus.PASSED, deserialized.status)
            assertEquals(1000L, deserialized.start)
            assertEquals(2000L, deserialized.stop)
            assertEquals(1, deserialized.labels.size)
            assertEquals("Test", deserialized.labels[0].value)
            assertEquals(1, deserialized.steps.size)
            assertEquals("Step 1", deserialized.steps[0].name)
        } finally {
            AllureWriter.outputDir = originalOutputDir
        }
    }
}
```

**Step 3: Write inferTestName test**

```kotlin
package io.github.kakaokmp.dsl

import kotlin.test.Test
import kotlin.test.assertTrue

class InferTestNameTest {

    @Test
    fun returnsNonEmptyString() {
        val name = inferTestName()
        // On JVM, should find the test class in the stack trace.
        // On Kotlin/Native, falls back to "UnknownTest".
        // Either way, must not be empty.
        assertTrue(name.isNotEmpty(), "inferTestName() returned empty string")
    }

    @Test
    fun fallbackIsUnknownTest() {
        // The function should never return empty — at minimum "UnknownTest"
        val name = inferTestName()
        // On JVM it may find this method; on Native it returns "UnknownTest"
        assertTrue(
            name.contains("InferTestNameTest") || name == "UnknownTest",
            "Expected test class name or 'UnknownTest', got: $name"
        )
    }
}
```

**Step 4: Run tests to verify they fail**

Run: `./gradlew :kakao-kmp:testDebugUnitTest --tests "io.github.kakaokmp.allure.*" --tests "io.github.kakaokmp.dsl.*"`
Expected: FAIL — unresolved references

**Step 5: Implement Allure model classes**

`AllureStatus.kt`:
```kotlin
package io.github.kakaokmp.allure.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AllureStatus {
    @SerialName("passed") PASSED,
    @SerialName("failed") FAILED,
    @SerialName("broken") BROKEN,
    @SerialName("skipped") SKIPPED,
}
```

`AllureLabel.kt`:
```kotlin
package io.github.kakaokmp.allure.model

import kotlinx.serialization.Serializable

@Serializable
data class AllureLabel(val name: String, val value: String)
```

`AllureStatusDetails.kt`:
```kotlin
package io.github.kakaokmp.allure.model

import kotlinx.serialization.Serializable

@Serializable
data class AllureStatusDetails(
    val message: String? = null,
    val trace: String? = null,
)
```

`AllureStepResult.kt`:
```kotlin
package io.github.kakaokmp.allure.model

import kotlinx.serialization.Serializable

@Serializable
data class AllureStepResult(
    val name: String,
    val status: AllureStatus,
    val start: Long,
    val stop: Long,
    val steps: List<AllureStepResult> = emptyList(),
)
```

`AllureTestResult.kt`:
```kotlin
package io.github.kakaokmp.allure.model

import kotlinx.serialization.Serializable

@Serializable
data class AllureTestResult(
    val uuid: String,
    val name: String,
    val fullName: String = name,
    val status: AllureStatus,
    val start: Long,
    val stop: Long,
    val labels: List<AllureLabel> = emptyList(),
    val steps: List<AllureStepResult> = emptyList(),
    val statusDetails: AllureStatusDetails? = null,
)
```

**Step 6: Implement AllureLifecycle**

```kotlin
package io.github.kakaokmp.allure

import io.github.kakaokmp.allure.model.*
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Collects Allure test result data during a test run.
 *
 * Supports nested steps via an internal [ArrayDeque] stack. Each [startStep] pushes
 * a new entry; each [stopStep] pops and attaches the completed step to its parent
 * (or to the top-level step list if there is no parent).
 */
class AllureLifecycle {
    private var testName: String = ""
    private var testStart: Long = 0L
    private var testStop: Long = 0L
    private var testStatus: AllureStatus = AllureStatus.PASSED
    private var testStatusDetails: AllureStatusDetails? = null
    private val labels = mutableListOf<AllureLabel>()
    private val steps = mutableListOf<AllureStepResult>()

    // Stack for nested step support
    private data class StepEntry(
        val name: String,
        val start: Long,
        val children: MutableList<AllureStepResult> = mutableListOf(),
    )

    private val stepStack = ArrayDeque<StepEntry>()

    fun startTest(name: String) {
        testName = name
        testStart = now()
    }

    fun addLabel(name: String, value: String) {
        labels += AllureLabel(name, value)
    }

    fun startStep(name: String) {
        stepStack.addLast(StepEntry(name = name, start = now()))
    }

    fun stopStep(status: AllureStatus, error: Throwable? = null) {
        val entry = stepStack.removeLast()
        val result = AllureStepResult(
            name = entry.name,
            status = status,
            start = entry.start,
            stop = now(),
            steps = entry.children.toList(),
        )
        // If there is a parent step on the stack, attach as a child; otherwise top-level
        val parent = stepStack.lastOrNull()
        if (parent != null) {
            parent.children += result
        } else {
            steps += result
        }
    }

    fun finishTest(status: AllureStatus, error: Throwable? = null) {
        testStatus = status
        testStop = now()
        if (error != null) {
            testStatusDetails = AllureStatusDetails(
                message = error.message,
                trace = error.stackTraceToString(),
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun buildResult(): AllureTestResult = AllureTestResult(
        uuid = Uuid.random().toString(),
        name = testName,
        fullName = testName,
        status = testStatus,
        start = testStart,
        stop = testStop,
        labels = labels.toList(),
        steps = steps.toList(),
        statusDetails = testStatusDetails,
    )

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()
}
```

**Step 7: Implement AllureWriter**

```kotlin
package io.github.kakaokmp.allure

import io.github.kakaokmp.allure.model.AllureTestResult
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object AllureWriter {
    /**
     * Output directory for Allure result JSON files.
     *
     * Default is `"allure-results"` (relative to the working directory).
     *
     * **Important for mobile targets:** On Android, the working directory during
     * `testDebugUnitTest` is typically the module root, so files land in
     * `<module>/allure-results/`. On iOS simulator tests, it may be the app sandbox.
     * Set this to an absolute path if you need predictable output location:
     *
     * ```
     * AllureWriter.outputDir = "/tmp/allure-results"
     * ```
     */
    var outputDir: String = "allure-results"

    private val json = Json { prettyPrint = true }

    fun write(result: AllureTestResult) {
        val dir = Path(outputDir)
        SystemFileSystem.createDirectories(dir)
        val file = Path(dir, "${result.uuid}-result.json")
        SystemFileSystem.sink(file).buffered().use { sink ->
            sink.writeString(json.encodeToString(result))
        }
    }
}
```

**Step 8: Implement TestScope**

```kotlin
package io.github.kakaokmp.dsl

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import io.github.kakaokmp.allure.AllureLifecycle
import io.github.kakaokmp.allure.model.AllureStatus
import io.github.kakaokmp.screen.KmpScreen
import io.github.kakaokmp.screen.KmpScreenFactory

@OptIn(ExperimentalTestApi::class)
class TestScope(
    val composeUiTest: ComposeUiTest,
    internal val allureLifecycle: AllureLifecycle = AllureLifecycle(),
) : ComposeUiTest by composeUiTest {

    operator fun <T : KmpScreen<T>> KmpScreenFactory<T>.invoke(block: T.() -> Unit) {
        create(composeUiTest).block()
    }

    fun allureId(id: String) = allureLifecycle.addLabel("AS_ID", id)
    fun feature(name: String) = allureLifecycle.addLabel("feature", name)
    fun epic(name: String) = allureLifecycle.addLabel("epic", name)
    fun story(name: String) = allureLifecycle.addLabel("story", name)

    /**
     * Executes [block] as a named step recorded in the Allure report.
     *
     * On failure, dumps the Compose semantics tree to logcat/stdout for debugging
     * (same behavior as the existing `Steps.kt` helper).
     */
    inline fun <R> step(title: String, block: TestScope.() -> R): R {
        println("STEP ▶ $title")
        allureLifecycle.startStep(title)
        return try {
            val result = block()
            allureLifecycle.stopStep(AllureStatus.PASSED)
            result
        } catch (e: Throwable) {
            println("STEP ✗ $title")
            println("ERROR: ${e::class.simpleName}: ${e.message}")
            // Dump semantics tree for debugging (matches existing Steps.kt behavior)
            runCatching {
                composeUiTest.onRoot(useUnmergedTree = true).printToLog(tag = "SemanticsTree")
            }
            allureLifecycle.stopStep(AllureStatus.FAILED, e)
            throw e
        }
    }
}
```

**Step 9: Implement kakaoTest entry point**

```kotlin
package io.github.kakaokmp.dsl

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import io.github.kakaokmp.allure.AllureLifecycle
import io.github.kakaokmp.allure.AllureWriter
import io.github.kakaokmp.allure.model.AllureStatus

/**
 * Entry point for kakao-kmp UI tests with Allure reporting.
 *
 * @param testName Name used in the Allure report. If `null`, the function attempts to
 *   infer the test name from the JVM stack trace. **On Kotlin/Native (iOS) this
 *   inference does not work** — always pass [testName] explicitly on iOS targets,
 *   or accept the `"UnknownTest"` fallback.
 *
 * Example:
 * ```
 * @Test
 * fun createCardFlow() = kakaoTest(testName = "createCardFlow") {
 *     allureId("TC-001")
 *     feature("Cards")
 *     step("Open screen") { HomeScreen { createButton.click() } }
 * }
 * ```
 */
@OptIn(ExperimentalTestApi::class)
fun kakaoTest(
    testName: String? = null,
    block: TestScope.() -> Unit,
) = runComposeUiTest {
    val lifecycle = AllureLifecycle()
    val scope = TestScope(this, lifecycle)
    val name = testName ?: inferTestName()

    lifecycle.startTest(name)
    try {
        scope.block()
        lifecycle.finishTest(AllureStatus.PASSED)
    } catch (e: Throwable) {
        lifecycle.finishTest(AllureStatus.FAILED, e)
        throw e
    } finally {
        AllureWriter.write(lifecycle.buildResult())
    }
}

/**
 * Attempts to extract the test method name from the JVM stack trace.
 *
 * Looks for a stack frame containing "Test." that is not part of kakao-kmp internals.
 * Returns `"UnknownTest"` if no matching frame is found (e.g., on Kotlin/Native where
 * stack trace format differs).
 */
internal fun inferTestName(): String {
    val trace = Throwable().stackTraceToString()
    val testLine = trace.lineSequence()
        .firstOrNull { line ->
            line.contains("Test.") && !line.contains("kakaoTest") && !line.contains("inferTestName")
        }
    return testLine
        ?.trim()
        ?.substringAfter("at ")
        ?.substringBefore("(")
        ?: "UnknownTest"
}
```

**Step 10: Run AllureLifecycle and AllureWriter tests**

Run: `./gradlew :kakao-kmp:testDebugUnitTest --tests "io.github.kakaokmp.allure.*"`
Expected: ALL PASS

**Step 11: Run inferTestName test**

Run: `./gradlew :kakao-kmp:testDebugUnitTest --tests "io.github.kakaokmp.dsl.InferTestNameTest"`
Expected: ALL PASS

**Step 12: Verify full module compilation**

Run: `./gradlew :kakao-kmp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 13: Commit**

```
feat(kakao-kmp): add Allure model, lifecycle, writer, TestScope and kakaoTest DSL
```

---

## Task 7: Integration — migrate existing test to kakao-kmp

**Files:**
- Modify: `composeApp/build.gradle.kts` (add `:kakao-kmp` dependency + ExperimentalTestApi opt-in)
- Create: `composeApp/src/commonTest/kotlin/org/example/project/screens/HomeScreen.kt`
- Create: `composeApp/src/commonTest/kotlin/org/example/project/screens/CreateCardScreen.kt`
- Create: `composeApp/src/commonTest/kotlin/org/example/project/CardCreationKakaoTest.kt`

**Step 1: Add dependency and opt-in to composeApp**

In `composeApp/build.gradle.kts`, update `commonTest.dependencies` and add global opt-in:

```kotlin
commonTest.dependencies {
    implementation(libs.kotlin.test)
    implementation(libs.compose.uiTest)
    implementation(projects.kakaoKmp)
}
```

Also add global opt-in for the consumer so test files don't need per-file `@OptIn`:

```kotlin
kotlin {
    // ... existing config ...

    // Add this inside the kotlin { } block:
    sourceSets.configureEach {
        languageSettings.optIn("androidx.compose.ui.test.ExperimentalTestApi")
    }
}
```

Note: `@OptIn(ExperimentalTestApi::class)` is required by Compose Multiplatform test API. Adding it as a global opt-in via `languageSettings` eliminates the need for per-file annotations. Without this, every test file and screen definition that touches `ComposeUiTest` would need the annotation.

**Step 2: Create HomeScreen page object**

```kotlin
package org.example.project.screens

import androidx.compose.ui.test.ComposeUiTest
import io.github.kakaokmp.screen.KmpScreen
import io.github.kakaokmp.screen.KmpScreenFactory
import io.github.kakaokmp.node.KNode
import org.example.project.ui.UiTags

class HomeScreen(test: ComposeUiTest) : KmpScreen<HomeScreen>(test) {
    val createCardButton = KNode { hasTestTag(UiTags.HomeCreateCardButton) }

    fun cardWithText(text: String) = KNode { hasText(text) }

    companion object : KmpScreenFactory<HomeScreen>(::HomeScreen)
}
```

**Step 3: Create CreateCardScreen page object**

```kotlin
package org.example.project.screens

import androidx.compose.ui.test.ComposeUiTest
import io.github.kakaokmp.screen.KmpScreen
import io.github.kakaokmp.screen.KmpScreenFactory
import io.github.kakaokmp.node.KNode
import org.example.project.ui.UiTags

class CreateCardScreen(test: ComposeUiTest) : KmpScreen<CreateCardScreen>(test) {
    val firstNameField = KNode { hasTestTag(UiTags.CreateFirstNameField) }
    val lastNameField = KNode { hasTestTag(UiTags.CreateLastNameField) }
    val submitButton = KNode { hasTestTag(UiTags.CreateSubmitButton) }
    val backButton = KNode { hasTestTag(UiTags.CreateBackButton) }

    companion object : KmpScreenFactory<CreateCardScreen>(::CreateCardScreen)
}
```

**Step 4: Write the new kakao-kmp test**

```kotlin
package org.example.project

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.create
import com.arkivanov.essenty.lifecycle.resume
import io.github.kakaokmp.dsl.kakaoTest
import org.example.project.root.DefaultRootComponent
import org.example.project.screens.CreateCardScreen
import org.example.project.screens.HomeScreen
import kotlin.test.Test

class CardCreationKakaoTest {

    @Test
    fun createCardFlow_addsCardToHomeList() = kakaoTest(testName = "createCardFlow_addsCardToHomeList") {
        allureId("TC-001")
        feature("Cards")

        val lifecycle = LifecycleRegistry().apply {
            create()
            resume()
        }
        val root = DefaultRootComponent(
            componentContext = DefaultComponentContext(lifecycle = lifecycle),
        )

        setContent {
            App(root = root)
        }

        step("[1] Нажать \"Создать карточку\"") {
            HomeScreen {
                createCardButton.click()
            }
        }
        step("[2] Ввести имя и фамилию") {
            CreateCardScreen {
                firstNameField.typeText("TestName")
                lastNameField.typeText("TestSurname")
            }
        }
        step("[3] Нажать \"Создать\"") {
            CreateCardScreen {
                submitButton.click()
            }
        }
        step("[4] Проверить, что карточка появилась на главном") {
            HomeScreen {
                cardWithText("TestName TestSurname").assertIsDisplayed()
            }
        }
    }
}
```

Note: `testName` is passed explicitly to `kakaoTest()` for reliability across all platforms (see I2). On Kotlin/Native (iOS), automatic inference does not work.

**Step 5: Run old test to ensure nothing is broken**

Run: `./gradlew :composeApp:desktopTest --tests "org.example.project.CardCreationUiTest"`
Expected: PASS

**Step 6: Run new kakao-kmp test**

Run: `./gradlew :composeApp:desktopTest --tests "org.example.project.CardCreationKakaoTest"`
Expected: PASS

**Step 7: Verify allure-results JSON was generated**

Run: `ls allure-results/` or `find . -name "*-result.json" -path "*/allure-results/*"`
Expected: One JSON file with TC-001 label

**Step 8: Commit**

```
feat: migrate card creation test to kakao-kmp with Page Objects and Allure
```

---

## Task 8: Run full test suite and verify

**Step 1: Run all tests**

Run: `./gradlew :kakao-kmp:testDebugUnitTest :composeApp:desktopTest`
Expected: ALL PASS

**Step 2: Inspect allure-results JSON content**

Read the generated JSON file and verify it contains:
- `status: "passed"`
- `labels` with `AS_ID = "TC-001"` and `feature = "Cards"`
- `steps` array with 4 steps, all `"passed"`

**Step 3: Final commit**

```
chore: verify full test suite passes with kakao-kmp integration
```
