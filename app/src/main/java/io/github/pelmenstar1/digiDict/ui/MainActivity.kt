package io.github.pelmenstar1.digiDict.ui

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.onNavDestinationSelected
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.ui.home.GlobalSearchQueryProvider
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {
    @Inject
    lateinit var appDatabase: AppDatabase

    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.main_container_view) as NavHostFragment
        val navController = navHostFragment.navController
        val appBarConfiguration = AppBarConfiguration(
            navController.graph,
            fallbackOnNavigateUpListener = ::onSupportNavigateUp
        )

        toolbar = findViewById(R.id.main_toolbar)

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.homeMenu_more, R.id.homeMenu_search -> false
                else -> {
                    it.onNavDestinationSelected(navController)
                }
            }
        }

        NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration)

        // TODO: Hide IME when dest fragment is changed.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val menu = toolbar.menu
            val destId = destination.id

            menu.clear()

            if (destId == R.id.homeFragment) {
                menuInflater.inflate(R.menu.home_menu, menu)

                initMenu(menu)
            } else {
                // If the destination isn't home, search should be active.
                GlobalSearchQueryProvider.isActive = false
            }
        }
    }

    override fun onStop() {
        super.onStop()

        val searchItem = toolbar.menu.findItem(R.id.homeMenu_search)

        if (searchItem == null || !searchItem.isActionViewExpanded) {
            // If the activity is stopped, action view in the toolbar is collapsed but for some reason,
            // appropriate event is not raised.
            //
            // When the action view is collapsed, the search should be no longer considered as active.
            GlobalSearchQueryProvider.isActive = false
        }
    }

    private fun initMenu(menu: Menu) {
        val item = menu.findItem(R.id.homeMenu_search)

        if (item != null) {
            val actionView = item.actionView as SimpleSearchView

            actionView.addTextChangedListener { text ->
                GlobalSearchQueryProvider.query = text ?: ""
            }

            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

            item.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                    GlobalSearchQueryProvider.isActive = true

                    actionView.requestFocusFromTouch()

                    // It's deprecated, but at least it works. The same can't be said about setSoftInputMode().
                    // For some unknown reason, it does not work.
                    @Suppress("DEPRECATION")
                    imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0)

                    return true
                }

                override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                    GlobalSearchQueryProvider.isActive = false

                    // In the next time the active view is expanded, text should be empty.
                    actionView.setText("")

                    imm.hideSoftInputFromWindow(actionView.windowToken, 0)

                    return true
                }
            })
        }
    }
}