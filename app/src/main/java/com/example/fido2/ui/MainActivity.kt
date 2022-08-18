package com.example.fido2.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.example.fido2.Fido2App
import com.example.fido2.R
import com.example.fido2.repository.SignInState
import com.example.fido2.ui.auth.AuthFragment
import com.example.fido2.ui.home.HomeFragment
import com.example.fido2.ui.username.UsernameFragment
import com.google.android.gms.fido.Fido
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import okhttp3.OkHttpClient


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.hide()


        lifecycleScope.launchWhenStarted {
            viewModel.signInState.collect { state ->
                when (state) {
                    is SignInState.SignedOut -> {
                        println("User signed out: Showing userName Fragment")
                        showFragment(UsernameFragment::class.java) { UsernameFragment() }
                    }
                    is SignInState.SigningIn -> {
                        println("User signing In: Showing Auth Fragment")
                        showFragment(AuthFragment::class.java) { AuthFragment() }
                    }
                    is SignInState.SignInError -> {
                        println("User signedInError: error: $state.error")
                       // Toast.makeText(this@MainActivity, state.error, Toast.LENGTH_LONG).show()
                        // return to username prompt
                        showFragment(UsernameFragment::class.java) { UsernameFragment() }
                    }
                    is SignInState.SignedIn -> {
                        println("User signed in: Showing HomeFragment")
                        showFragment(HomeFragment::class.java) { HomeFragment() }
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