package io.github.pelmenstar1.digiDict.common.ui

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import com.google.android.material.progressindicator.CircularProgressIndicator
import io.github.pelmenstar1.digiDict.common.DataLoadState
import io.github.pelmenstar1.digiDict.common.debugLog
import io.github.pelmenstar1.digiDict.common.launchFlowCollector
import kotlinx.coroutines.CoroutineScope

class DataLoadStateContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
    private var loadingIndicator: CircularProgressIndicator? = null
    private var errorContainer: ErrorContainer? = null

    private var errorText: CharSequence? = null

    private var _content: View? = null
    val content: View
        get() = _content ?: throw IllegalStateException("No content was previously added")

    init {
        // DataLoadState.Loading will be almost always present in the flow.
        // So, prepare for it in the constructor.
        superAdd(createLoadingIndicator().also { loadingIndicator = it })

        if (attrs != null) {
            val theme = context.theme
            val a = theme.obtainStyledAttributes(attrs, R.styleable.DataLoadStateContainer, defStyleAttr, defStyleRes)

            try {
                errorText = a.getText(R.styleable.DataLoadStateContainer_errorText)
            } finally {
                a.recycle()
            }
        }
    }

    private fun createLoadingIndicator(): CircularProgressIndicator {
        return CircularProgressIndicator(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }

            isIndeterminate = true
        }
    }

    private fun createErrorContainer(): ErrorContainer {
        return ErrorContainer(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER

                errorText?.let(::setErrorText)
            }
        }
    }

    fun <T> setupLoadStateFlow(
        scope: CoroutineScope,
        stateHolder: SingleDataLoadStateHolder<T>,
        onSuccess: suspend (T) -> Unit
    ) {
        scope.launchFlowCollector(stateHolder.dataStateFlow) {
            // TODO: Delete it.
            debugLog("DataLoadStateContainer") {
                info("state: $it")
            }

            when (it) {
                is DataLoadState.Loading -> {
                    var li = loadingIndicator
                    if (li == null) {
                        li = createLoadingIndicator()
                        loadingIndicator = li

                        superAdd(li)
                    }

                    li.visibility = View.VISIBLE
                    errorContainer?.visibility = View.GONE
                    content.visibility = View.GONE
                }
                is DataLoadState.Error -> {
                    var ec = errorContainer
                    if (ec == null) {
                        ec = createErrorContainer()
                        ec.setOnRetryListener { stateHolder.retryLoadData() }

                        errorContainer = ec
                        superAdd(ec)
                    }

                    ec.visibility = View.VISIBLE
                    loadingIndicator?.visibility = View.GONE
                    content.visibility = View.GONE
                }
                is DataLoadState.Success -> {
                    content.visibility = View.VISIBLE

                    if (stateHolder.canRefreshAfterSuccess) {
                        loadingIndicator?.visibility = View.GONE
                        errorContainer?.visibility = View.GONE
                    } else {
                        // If it's stated that it's  unlikely that Loading or Error can be emitted after Success,
                        // remove loadingIndicator and errorContainer from the hierarchy. If the assumption doesn't hold,
                        // the views will be recreated and added back to the hierarchy but it will have
                        // big performance drawback.
                        loadingIndicator?.let { li ->
                            removeView(li)

                            // Allow GC do its work.
                            loadingIndicator = null
                        }

                        errorContainer?.let { ec ->
                            removeView(ec)

                            // Allow GC do its work.
                            errorContainer = null
                        }
                    }

                    onSuccess(it.value)
                }
            }
        }
    }

    // It's called to bypass overloaded addView which assigns the view to be added to _content.
    // Must be called if 'view' is not content (always).
    private fun superAdd(view: View) {
        super.addView(view, -1, view.layoutParams)
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        _content = child

        super.addView(child, index, params)
    }
}