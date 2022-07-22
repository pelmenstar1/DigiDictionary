package io.github.pelmenstar1.digiDict.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.SupportMenuInflater
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.onNavDestinationSelected
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.data.AppDatabase
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {
    @Inject
    lateinit var appDatabase: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.main_container_view) as NavHostFragment
        val navController = navHostFragment.navController
        val appBarConfiguration = AppBarConfiguration(
            navController.graph,
            fallbackOnNavigateUpListener = ::onSupportNavigateUp
        )

        val toolbar = findViewById<MaterialToolbar>(R.id.main_toolbar)

        toolbar.setOnMenuItemClickListener {
            if(it.itemId != R.id.home_menu_more) {
                it.onNavDestinationSelected(navController)
            } else {
                false
            }
        }

        NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration)

        val menuInflater = SupportMenuInflater(this)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val menu = toolbar.menu
            if(destination.id == R.id.homeFragment) {
                menuInflater.inflate(R.menu.home_menu, menu)
            } else {
                menu.clear()
            }
        }
    }
}