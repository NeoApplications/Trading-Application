package com.saggitt.omega.preferences.views

import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.android.launcher3.databinding.PreferencesActivityBinding
import com.saggitt.omega.PREFS_PROTECTED_APPS
import com.saggitt.omega.theme.ThemeManager
import com.saggitt.omega.theme.ThemeOverride
import com.saggitt.omega.util.omegaPrefs

class PreferencesActivity : AppCompatActivity(), ThemeManager.ThemeableActivity {
    private lateinit var binding: PreferencesActivityBinding
    private var currentTheme = 0
    private lateinit var themeOverride: ThemeOverride
    private val themeSet: ThemeOverride.ThemeSet get() = ThemeOverride.Settings()
    private var paused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        themeOverride = ThemeOverride(themeSet, this)
        themeOverride.applyTheme(this)
        currentTheme = themeOverride.getTheme(this)

        super.onCreate(savedInstanceState)
        binding = PreferencesActivityBinding.inflate(layoutInflater)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, PrefsMainFragment()).commit()
        setContentView(binding.root)
        setSupportActionBar(binding.actionBar)
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                binding.actionBar.setNavigationOnClickListener { super.onBackPressed() }
                binding.actionBar.navigationIcon = null
            } else {
                binding.actionBar.setNavigationOnClickListener { supportFragmentManager.popBackStack() }
                binding.actionBar.navigationIcon =
                    AppCompatResources.getDrawable(this, R.drawable.ic_sysbar_back)
            }
        }

        DEFAULT_HOME = resolveDefaultHome()
    }

    private fun resolveDefaultHome(): String? {
        val homeIntent: Intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
        val info: ResolveInfo? = packageManager
            .resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        return if (info?.activityInfo != null) {
            info.activityInfo.packageName
        } else {
            null
        }
    }

    // TODO should any of those sub classes get larger, then it should be moved to own class

    class PrefsMainFragment : PreferenceFragmentCompat() {
        private var mShowDevOptions = false
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            mShowDevOptions = Utilities.getOmegaPrefs(activity).developerOptionsEnabled
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_main, rootKey)
        }

        override fun onResume() {
            super.onResume()
            val dev = Utilities.getOmegaPrefs(activity).developerOptionsEnabled
            if (dev != mShowDevOptions) {
                activity?.recreate()
            }
        }
    }

    class PrefsDesktopFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_desktop, rootKey)
        }
    }

    class PrefsDockFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_dock, rootKey)
        }
    }

    class PrefsDrawerFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_drawer, rootKey)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            findPreference<SwitchPreferenceCompat>(PREFS_PROTECTED_APPS)?.apply {
                onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any ->
                        requireActivity().omegaPrefs.enableProtectedApps = newValue as Boolean
                        true
                    }
            }
        }
    }

    class PrefsSearchFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_search, rootKey)
        }
    }

    class PrefsGesturesFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_gestures, rootKey)
        }
    }

    class PrefsAdvancedFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_advanced, rootKey)
        }
    }

    class PrefsDevFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_dev, rootKey)
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            findPreference<Preference>("kill")?.setOnPreferenceClickListener {
                Utilities.killLauncher()
                false
            }
        }
    }

    override fun onThemeChanged() {
        if (currentTheme == themeOverride.getTheme(this)) return
        if (paused) {
            recreate()
        } else {
            finish()
            startActivity(
                createRelaunchIntent(), ActivityOptions.makeCustomAnimation(
                    this, android.R.anim.fade_in, android.R.anim.fade_out
                ).toBundle()
            )
        }
    }

    fun createRelaunchIntent(): Intent {
        val state = Bundle()
        onSaveInstanceState(state)
        return intent.putExtra("state", state)
    }

    companion object {

        var DEFAULT_HOME: String? = ""
    }
}