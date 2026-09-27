package com.assignment.scicalc

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.assignment.scicalc.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(
            if (viewModel.darkTheme) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            showFragment(CalculatorFragment(), "calc")
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_calculator -> showFragment(CalculatorFragment(), "calc")
                R.id.nav_history -> showFragment(HistoryFragment(), "hist")
                R.id.nav_whatif -> showFragment(WhatIfFragment(), "what")
                R.id.nav_settings -> showFragment(SettingsFragment(), "set")
            }
            true
        }
    }

    private fun showFragment(fragment: Fragment, tag: String) {
        val existing = supportFragmentManager.findFragmentByTag(tag)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, existing ?: fragment, tag)
            .commit()
    }
}
