package com.example.fido2.ui

import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.example.fido2.R
import com.example.fido2.repository.SignInState
import com.example.fido2.ui.FidoPicker.FidoPickerFragment
import com.example.fido2.ui.auth.AuthFragment
import com.example.fido2.ui.home.HomeFragment
import com.example.fido2.ui.username.UsernameFragment
import com.google.android.gms.fido.Fido
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        setSupportActionBar(findViewById(R.id.toolbar))


        lifecycleScope.launchWhenStarted {
            viewModel.signInState.collect { state ->
                when (state) {
                    is SignInState.SignedOut -> {
                        println("hit sign out")
                        showFragment(UsernameFragment::class.java) { UsernameFragment() }
                    }
                    is SignInState.SigningIn -> {
                        showFragment(AuthFragment::class.java) { AuthFragment() }
                    }
                    is SignInState.SignInError -> {
                        Toast.makeText(this@MainActivity, state.error, Toast.LENGTH_LONG).show()
                        // return to username prompt
                        showFragment(UsernameFragment::class.java) { UsernameFragment() }
                    }
                    is SignInState.SignedIn -> {
                        showFragment(HomeFragment::class.java) { HomeFragment() }
                    }
                    is SignInState.Other -> {
                        println("Hit sign in state")
                        showFragment(FidoPickerFragment:: class.java){FidoPickerFragment()}
                    }
                }
            }
        }
    }
    private fun showFragment(clazz: Class<out Fragment>, create: () -> Fragment) {
        val manager = supportFragmentManager
        if (!clazz.isInstance(manager.findFragmentById(R.id.container))) {
            manager.commit {
                replace(R.id.container, create())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.setFido2ApiClient(Fido.getFido2ApiClient(this))
    }

    override fun onPause() {
        super.onPause()
        viewModel.setFido2ApiClient(null)
    }
}