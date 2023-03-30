package io.github.pelmenstar1.digiDict.common

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import io.github.pelmenstar1.digiDict.common.android.*
import io.github.pelmenstar1.digiDict.commonTestUtils.launchActivity
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class LifecycleKtTests {
    class TestActivity : AppCompatActivity() {
        lateinit var container: ViewGroup

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            container = LinearLayout(this)
            setContentView(container)
        }
    }

    private class SimpleSnackbar(
        context: Context,
        parent: ViewGroup,
        content: View,
        contentViewCallback: com.google.android.material.snackbar.ContentViewCallback
    ) : BaseTransientBottomBar<SimpleSnackbar>(context, parent, content, contentViewCallback) {
        var isDismissCalled = false

        override fun dismiss() {
            isDismissCalled = true

            super.dismiss()
        }

        companion object {
            fun make(parent: ViewGroup): SimpleSnackbar {
                val context = parent.context
                val layout = SnackbarLayout(context)
                val callback = object : com.google.android.material.snackbar.ContentViewCallback {
                    override fun animateContentIn(delay: Int, duration: Int) {
                    }

                    override fun animateContentOut(delay: Int, duration: Int) {
                    }
                }

                return SimpleSnackbar(context, parent, layout, callback)
            }
        }
    }

    // Because of use of the reflection, the class won't work when obfuscation is enabled.
    private class SnackbarManagerWrapper(private val obj: Any) {
        fun isAnySnackbarShown(): Boolean {
            val field = snackbarManagerClass.getDeclaredField("currentSnackbar").apply {
                isAccessible = true
            }

            return field.get(obj) != null
        }

        companion object {
            private val snackbarManagerClass = Class.forName("com.google.android.material.snackbar.SnackbarManager")

            // Calls com.google.android.material.snackbar.SnackbarManager.getInstance() and returns a wrapper.
            fun getInstance(): SnackbarManagerWrapper {
                val method = snackbarManagerClass.getDeclaredMethod("getInstance").apply {
                    isAccessible = true
                }

                val obj = method.invoke(null)!!

                return SnackbarManagerWrapper(obj)
            }
        }
    }

    private class TestViewModel : ViewModel() {
        val successAction = viewModelAction("TAG") {}
        val faultAction = viewModelAction("TAG") { throw Exception() }
    }

    private inline fun onTestActivity(crossinline block: (TestActivity) -> Unit) {
        launchActivity<TestActivity>().onActivity { block(it) }
    }

    @Test
    fun showLifecycleAwareSnackbarTest() {
        onTestActivity { activity ->
            val snackbar = SimpleSnackbar.make(activity.container)
            val lifecycleRegistry = LifecycleRegistry(activity)
            lifecycleRegistry.currentState = Lifecycle.State.CREATED

            snackbar.showLifecycleAwareSnackbar(lifecycleRegistry)
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED

            assertTrue(snackbar.isDismissCalled, "dismiss() wasn't called")
            assertEquals(0, lifecycleRegistry.observerCount, "lifecycle observer is not removed")
        }
    }

    private fun showSnackbarEventHandlerOnErrorTestHelper(
        getAction: TestViewModel.() -> NoArgumentViewModelAction,
        expectedIsAnySnackbarShown: Boolean
    ) {
        // We can only validate correct behaviour on the debug build.
        if (!BuildConfig.DEBUG) {
            return
        }

        onTestActivity { activity ->
            val vm = TestViewModel()
            val action = vm.getAction()

            activity.showSnackbarEventHandlerOnError(action, activity.container, R.string.test_res_1)
            action.run()

            val manager = SnackbarManagerWrapper.getInstance()
            val isAnySnackbarShown = manager.isAnySnackbarShown()

            assertEquals(expectedIsAnySnackbarShown, isAnySnackbarShown)
        }
    }

    @Test
    fun showSnackbarEventHandlerOnErrorTest_successAction() {
        showSnackbarEventHandlerOnErrorTestHelper({ successAction }, expectedIsAnySnackbarShown = false)
    }

    @Test
    fun showSnackbarEventHandlerOnErrorTest_faultAction() {
        showSnackbarEventHandlerOnErrorTestHelper({ faultAction }, expectedIsAnySnackbarShown = true)
    }
}