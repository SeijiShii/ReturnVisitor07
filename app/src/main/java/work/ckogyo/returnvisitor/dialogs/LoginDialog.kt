package work.ckogyo.returnvisitor.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.login_dialog.view.*
import work.ckogyo.returnvisitor.MainActivity
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.utils.setOnClick

class LoginDialog : DialogFragment() {

    private val mainActivity: MainActivity?
        get() = context as? MainActivity

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val v = View.inflate(context, R.layout.login_dialog, null).also {
            it.googleSignInButton.setOnClickListener {
                mainActivity?.signIn()
            }
            it.anonymousLoginButton.setOnClickListener {

            }
            it.googleSignInHelpButton.setOnClick {
                mainActivity?.showTextPopupDialog()
            }
            it.anonymousLoginHelpButton.setOnClick {
                mainActivity?.showTextPopupDialog()
            }
        }

        isCancelable = false

        return AlertDialog.Builder(context).also {
            it.setView(v)
        }.create()
    }


}