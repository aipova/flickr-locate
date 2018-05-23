package ru.aipova.locatr

import android.support.v4.app.Fragment
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

class LocatrActivity : SingleFragmentActivity() {
    override fun createFragment(): Fragment {
        return LocatrFragment.newInstance()
    }

    override fun onResume() {
        super.onResume()
        val apiAvailability = GoogleApiAvailability.getInstance()
        val errorCode = apiAvailability.isGooglePlayServicesAvailable(this)
        if (errorCode != ConnectionResult.SUCCESS) {
            val errorDialog = apiAvailability.getErrorDialog(this, errorCode, REQUEST_ERROR, { finish() })
            errorDialog.show()
        }
    }

    companion object {
        private const val REQUEST_ERROR = 0
    }
}
