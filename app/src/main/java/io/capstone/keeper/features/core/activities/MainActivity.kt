package io.capstone.keeper.features.core.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation
import dagger.hilt.android.AndroidEntryPoint
import io.capstone.keeper.R
import io.capstone.keeper.databinding.ActivityMainBinding
import io.capstone.keeper.features.auth.AuthViewModel
import io.capstone.keeper.features.shared.components.BaseActivity

@AndroidEntryPoint
class MainActivity: BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var controller: NavController

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        android.util.Log.e("DEBUG", intent?.action.toString())
        android.util.Log.e("DEBUG", intent?.data.toString())

        controller = Navigation.findNavController(this, R.id.navHostFragment)

        if (authViewModel.checkCurrentUser() != null)
            controller.navigate(R.id.to_navigation_root)
        else controller.navigate(R.id.to_navigation_auth)
    }

    override fun onSupportNavigateUp(): Boolean = controller.navigateUp()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        controller.currentDestination?.let {
            outState.putInt(EXTRA_CURRENT_DESTINATION, it.id)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        savedInstanceState.getInt(EXTRA_CURRENT_DESTINATION).let {
            controller.navigate(it)
        }
    }

    companion object {
        const val EXTRA_CURRENT_DESTINATION = "extra:current:destination"
    }
}