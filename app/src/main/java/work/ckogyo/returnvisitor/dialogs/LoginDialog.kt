package work.ckogyo.returnvisitor.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.login_dialog.view.*
import work.ckogyo.returnvisitor.MainActivity
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.utils.setOnClick

class LoginDialog : DialogFrameFragment() {

    private val mainActivity: MainActivity?
        get() = context as? MainActivity

    override fun onOkClick() {}

    override fun inflateContentView(): View {
        return View.inflate(context, R.layout.login_dialog, null).also {
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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        closeButtonStyle = CloseButtonStyle.None
        isCancelable = false
    }

}