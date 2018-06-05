package ru.aipova.locatr.screen.map

import android.support.v4.app.Fragment
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import ru.aipova.locatr.screen.SingleFragmentActivity

class LocatrActivity : SingleFragmentActivity() {
    override fun createFragment(): Fragment {
        return LocatrFragment.newInstance()
    }

    override fun onResume() {
        super.onResume()
        val apiAvailability = GoogleApiAvailability.getInstance()
        val errorCode = apiAvailability.isGooglePlayServicesAvailable(this)
        if (errorCode != ConnectionResult.SUCCESS) {
            val errorDialog = apiAvailability.getErrorDialog(this, errorCode,
                REQUEST_ERROR, { finish() })
            errorDialog.show()
        }
    }

    companion object {
        private const val REQUEST_ERROR = 0
    }
}
