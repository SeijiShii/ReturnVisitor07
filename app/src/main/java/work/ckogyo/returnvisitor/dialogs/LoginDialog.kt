package work.ckogyo.returnvisitor.dialogs

import android.os.Bundle
import android.view.View
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
                mainActivity?.signInAnonymously()
            }
            it.googleSignInHelpButton.setOnClick { v ->
                mainActivity?.showTextPopupDialog(v, R.string.google_login_description)
            }
            it.anonymousLoginHelpButton.setOnClick { v ->
                mainActivity?.showTextPopupDialog(v, R.string.anonymous_login_description)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        closeButtonStyle = CloseButtonStyle.None
        isCancelable = false
    }

}