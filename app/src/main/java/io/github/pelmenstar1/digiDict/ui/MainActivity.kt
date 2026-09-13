package io.github.pelmenstar1.digiDict.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.ui.home.HomeFragmentDirections
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {
    @Inject
    lateinit var appDatabase: AppDatabase

    private lateinit var toolbar: MaterialToolbar
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        // The theme is dark-only, so both bars always need light icons. Requesting the styles
        // explicitly avoids auto() picking light icons from the (unused) light configuration.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        super.onCreate(savedInstanceState)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.main_container_view) as NavHostFragment
        navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(navController.graph)

        toolbar = findViewById(R.id.main_toolbar)

        applyWindowInsets()

        // The toolbar is the support action bar so that a destination can contribute its own menu
        // through a MenuProvider bound to its own lifecycle. The activity therefore no longer
        // inflates or clears menus itself based on the current destination id.
        setSupportActionBar(toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)

        val imm = getSystemService(InputMethodManager::class.java)
        navController.addOnDestinationChangedListener { _, _, _ ->
            // For some reason, IME is not always hidden when fragment is changed.
            currentFocus?.let {
                imm?.hideSoftInputFromWindow(it.windowToken, 0)
            }
        }

        handleIntent(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_PROCESS_TEXT) {
            val targetText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)

            val directions = HomeFragmentDirections.actionHomeToAddEditRecord(
                initialExpression = targetText?.toString()
            )

            navController.navigate(directions)
        }
    }

    /**
     * Makes the content draw behind the system bars: the app bar takes the status bar inset and the
     * nav host takes the navigation bar and IME insets, so every destination in the graph is inset
     * correctly without having to handle it fragment by fragment.
     */
    private fun applyWindowInsets() {
        val appBar = findViewById<View>(R.id.main_appBar)
        val navHostContainer = findViewById<View>(R.id.main_container_view)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { _, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())

            appBar.updatePadding(left = bars.left, top = bars.top, right = bars.right)

            // Below API 30 adjustResize shrinks the window itself, so the IME inset is reported as
            // 0 there and max() collapses to the navigation bar inset. From API 30 on, the window
            // keeps its size and the IME inset is the one that matters.
            navHostContainer.updatePadding(
                left = bars.left,
                right = bars.right,
                bottom = maxOf(bars.bottom, ime.bottom)
            )

            windowInsets
        }
    }
}
