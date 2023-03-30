package io.github.pelmenstar1.digiDict.common.ui.tests

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.lifecycleScope
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.RootMatchers.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.common.DataLoadState
import io.github.pelmenstar1.digiDict.common.DataLoadStateManager
import io.github.pelmenstar1.digiDict.common.firstSuccess
import io.github.pelmenstar1.digiDict.common.ui.DataLoadStateContainer
import io.github.pelmenstar1.digiDict.common.ui.ErrorContainer
import io.github.pelmenstar1.digiDict.common.ui.R
import io.github.pelmenstar1.digiDict.common.ui.SingleDataLoadStateHolder
import io.github.pelmenstar1.digiDict.commonTestUtils.assertSameIf
import io.github.pelmenstar1.digiDict.commonTestUtils.firstViewOfType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class DataLoadStateContainerTests {
    open class CannotRefreshAfterSuccessImpl : SingleDataLoadStateHolder<String> {
        private sealed interface Message {
            data class Success(val value: String) : Message
            class Error(val ex: Exception) : Message
        }

        private val messageChannel = Channel<Message>(Channel.UNLIMITED)
        private val dataStateManager = DataLoadStateManager<String>("StateHolderImpl")
        private val scope = CoroutineScope(Dispatchers.Main.immediate)

        override val dataStateFlow: SharedFlow<DataLoadState<String>> = dataStateManager.buildFlow(scope) {
            fromFlow {
                flow {
                    while (true) {
                        when (val message = messageChannel.receive()) {
                            is Message.Success -> emit(message.value)
                            is Message.Error -> throw message.ex
                        }
                    }
                }
            }
        }

        override val canRefreshAfterSuccess: Boolean
            get() = false

        override fun retryLoadData() {
            dataStateManager.retry()
        }

        fun sendValue(value: String) = runBlocking(Dispatchers.Main) {
            messageChannel.send(Message.Success(value))
            dataStateFlow.firstSuccess()
        }

        fun sendError() = runBlocking(Dispatchers.Main) {
            messageChannel.send(Message.Error(RuntimeException()))
            dataStateFlow.first { it is DataLoadState.Error }
        }

        fun cancelScope() {
            scope.cancel()
        }
    }

    class CanRefreshAfterSuccessImpl : CannotRefreshAfterSuccessImpl() {
        override val canRefreshAfterSuccess: Boolean
            get() = true
    }

    class TestActivity : AppCompatActivity() {
        lateinit var dataLoadStateContainer: DataLoadStateContainer
        lateinit var dataContent: TextView

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            setContentView(FrameLayout(this).apply {
                dataLoadStateContainer = DataLoadStateContainer(context).apply {
                    dataContent = AppCompatTextView(context).apply {
                        text = "content"
                    }

                    addView(dataContent)
                }

                addView(dataLoadStateContainer)
            })
        }
    }

    private fun launchActivity() = ActivityScenario.launch(TestActivity::class.java)

    private fun onProgressBar() = onView(instanceOf(ProgressBar::class.java))

    private fun onErrorContainer() = onView(instanceOf(ErrorContainer::class.java))

    private fun onContent() = onView(withText("content"))

    private fun captureLastSuccessValue(
        scenario: ActivityScenario<TestActivity>,
        impl: SingleDataLoadStateHolder<String>
    ): AtomicReference<String?> {
        val ref = AtomicReference<String?>()

        scenario.onActivity { activity ->
            activity.dataLoadStateContainer.setupLoadStateFlow(activity.lifecycleScope, impl) { value ->
                ref.set(value)
            }
        }

        return ref
    }

    private fun inHierarchyAssertion(state: Boolean) = if (state) {
        matches(withEffectiveVisibility(Visibility.GONE))
    } else {
        doesNotExist()
    }

    private fun checkLoadingState(isErrorContainerInHierarchy: Boolean) {
        onProgressBar().check(matches(isDisplayed()))
        onErrorContainer().check(inHierarchyAssertion(isErrorContainerInHierarchy))
        onContent().check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    private fun checkErrorState(isProgressBarInHierarchy: Boolean) {
        onProgressBar().check(inHierarchyAssertion(isProgressBarInHierarchy))
        onErrorContainer().check(matches(isDisplayed()))
        onContent().check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    private fun checkSuccessState(isProgressBarInHierarchy: Boolean, isErrorContainerInHierarchy: Boolean) {
        onProgressBar().check(inHierarchyAssertion(isProgressBarInHierarchy))
        onErrorContainer().check(inHierarchyAssertion(isErrorContainerInHierarchy))
        onContent().check(matches(isDisplayed()))
    }

    private fun sendValueAndCheckSuccessState(
        impl: CannotRefreshAfterSuccessImpl,
        lastSuccessHolder: AtomicReference<String?>,
        value: String,
        isProgressBarInHierarchy: Boolean,
        isErrorContainerInHierarchy: Boolean
    ) {
        impl.sendValue(value)

        assertEquals(value, lastSuccessHolder.get())
        checkSuccessState(isProgressBarInHierarchy, isErrorContainerInHierarchy)
    }

    private inline fun <reified T : View> findViewInContainer(scenario: ActivityScenario<TestActivity>): T {
        var view: T? = null

        scenario.onActivity { activity ->
            view = activity.dataLoadStateContainer.firstViewOfType()
        }

        return view!!
    }

    private fun clickRetryButton() {
        onView(withText(R.string.retry)).perform(click())
    }

    private fun createImpl(canRefreshAfterSuccess: Boolean): CannotRefreshAfterSuccessImpl {
        return if (canRefreshAfterSuccess) CanRefreshAfterSuccessImpl() else CannotRefreshAfterSuccessImpl()
    }

    private fun successTestHelper(canRefreshAfterSuccess: Boolean) {
        val scenario = launchActivity()
        val impl = createImpl(canRefreshAfterSuccess)
        val lastSuccessHolder = captureLastSuccessValue(scenario, impl)

        assertNull(lastSuccessHolder.get())
        checkLoadingState(isErrorContainerInHierarchy = false)

        sendValueAndCheckSuccessState(
            impl, lastSuccessHolder,
            value = "123",
            isProgressBarInHierarchy = canRefreshAfterSuccess, isErrorContainerInHierarchy = false
        )

        impl.cancelScope()
    }

    @Test
    fun successTest_canRefreshAfterSuccess() = successTestHelper(canRefreshAfterSuccess = true)

    @Test
    fun successTest_cannotRefreshAfterSuccess() = successTestHelper(canRefreshAfterSuccess = false)

    @Test
    fun errorTest() {
        val scenario = launchActivity()
        val impl = CanRefreshAfterSuccessImpl()

        val lastSuccessHolder = captureLastSuccessValue(scenario, impl)

        assertNull(lastSuccessHolder.get())
        checkLoadingState(isErrorContainerInHierarchy = false)

        impl.sendError()

        // onSuccess lambda shouldn't be called
        assertNull(lastSuccessHolder.get())
        checkErrorState(isProgressBarInHierarchy = true)

        clickRetryButton()

        // Still we haven't been in success state.
        assertNull(lastSuccessHolder.get())
        checkLoadingState(isErrorContainerInHierarchy = true)

        sendValueAndCheckSuccessState(
            impl, lastSuccessHolder,
            value = "321",
            isProgressBarInHierarchy = true, isErrorContainerInHierarchy = true
        )

        impl.cancelScope()
    }

    private fun errorAfterSuccessTestHelper(canRefreshAfterSuccess: Boolean) {
        val scenario = launchActivity()
        val impl = createImpl(canRefreshAfterSuccess)

        val lastSuccessHolder = captureLastSuccessValue(scenario, impl)

        assertNull(lastSuccessHolder.get())
        checkLoadingState(isErrorContainerInHierarchy = false)

        sendValueAndCheckSuccessState(
            impl, lastSuccessHolder,
            value = "123",
            isProgressBarInHierarchy = canRefreshAfterSuccess, isErrorContainerInHierarchy = false
        )

        impl.sendError()
        checkErrorState(isProgressBarInHierarchy = canRefreshAfterSuccess)

        impl.cancelScope()
    }

    @Test
    fun errorAfterSuccessTest_canRefreshAfterSuccess() = errorAfterSuccessTestHelper(canRefreshAfterSuccess = true)

    @Test
    fun errorAfterSuccessTest_cannotRefreshAfterSuccess() = errorAfterSuccessTestHelper(canRefreshAfterSuccess = false)

    // Test this combination: loading -> success -> error -> loading
    private fun loadingSuccessErrorLoadingTestHelper(canRefreshAfterSuccess: Boolean) {
        val scenario = launchActivity()
        val impl = createImpl(canRefreshAfterSuccess)

        val lastSuccessHolder = captureLastSuccessValue(scenario, impl)

        val initialProgressBar = findViewInContainer<ProgressBar>(scenario)

        assertNull(lastSuccessHolder.get())
        checkLoadingState(isErrorContainerInHierarchy = false)

        sendValueAndCheckSuccessState(
            impl, lastSuccessHolder,
            value = "123",
            isProgressBarInHierarchy = canRefreshAfterSuccess, isErrorContainerInHierarchy = false
        )

        impl.sendError()
        checkErrorState(isProgressBarInHierarchy = canRefreshAfterSuccess)

        impl.retryLoadData()

        // Wait for loading state.
        runBlocking {
            impl.dataStateFlow.first { it is DataLoadState.Loading }
        }

        val progressBarAfterSuccess = findViewInContainer<ProgressBar>(scenario)

        // Error container should be present in the hierarchy because there was no success state after error one.
        checkLoadingState(isErrorContainerInHierarchy = true)

        // Test whether canRefreshAfterSuccess property works as expected.
        assertSameIf(canRefreshAfterSuccess, initialProgressBar, progressBarAfterSuccess)

        impl.cancelScope()
    }

    @Test
    fun loadingSuccessErrorLoadingTest_canRefreshAfterSuccess() {
        loadingSuccessErrorLoadingTestHelper(canRefreshAfterSuccess = true)
    }

    @Test
    fun loadingSuccessErrorLoadingTest_cannotRefreshAfterSuccess() {
        loadingSuccessErrorLoadingTestHelper(canRefreshAfterSuccess = false)
    }

    // Test this combination: loading -> error -> loading -> success -> loading -> error
    private fun loadingErrorLoadingSuccessLoadingErrorTestHelper(canRefreshAfterSuccess: Boolean) {
        val scenario = launchActivity()
        val impl = createImpl(canRefreshAfterSuccess)

        val lastSuccessHolder = captureLastSuccessValue(scenario, impl)

        val initialProgressBar = findViewInContainer<ProgressBar>(scenario)

        assertNull(lastSuccessHolder.get())
        checkLoadingState(isErrorContainerInHierarchy = false)

        impl.sendError()
        checkErrorState(isProgressBarInHierarchy = true)

        val initialErrorContainer = findViewInContainer<ErrorContainer>(scenario)

        impl.retryLoadData()

        checkLoadingState(isErrorContainerInHierarchy = true)

        sendValueAndCheckSuccessState(
            impl, lastSuccessHolder,
            value = "321",
            isProgressBarInHierarchy = canRefreshAfterSuccess, isErrorContainerInHierarchy = canRefreshAfterSuccess
        )

        impl.retryLoadData()

        checkLoadingState(isErrorContainerInHierarchy = canRefreshAfterSuccess)

        val progressBarAfterError = findViewInContainer<ProgressBar>(scenario)

        impl.sendError()
        checkErrorState(isProgressBarInHierarchy = true)

        val errorContainerAfterSecondError = findViewInContainer<ErrorContainer>(scenario)

        assertSameIf(canRefreshAfterSuccess, initialProgressBar, progressBarAfterError)
        assertSameIf(canRefreshAfterSuccess, initialErrorContainer, errorContainerAfterSecondError)

        impl.cancelScope()
    }

    @Test
    fun loadingErrorLoadingSuccessLoadingErrorTest_canRefreshAfterSuccess() {
        loadingErrorLoadingSuccessLoadingErrorTestHelper(canRefreshAfterSuccess = true)
    }

    @Test
    fun loadingErrorLoadingSuccessLoadingErrorTest_cannotRefreshAfterSuccess() {
        loadingErrorLoadingSuccessLoadingErrorTestHelper(canRefreshAfterSuccess = false)
    }
}