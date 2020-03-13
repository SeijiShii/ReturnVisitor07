package work.ckogyo.returnvisitor.dialogs

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

class WebViewDialog() : DialogFrameFragment() {

    private var urlString = ""
    constructor(urlString: String): this() {
        this.urlString = urlString
    }

    private lateinit var webView: WebView

    override fun onOkClick() {}

    override fun inflateContentView(): View {

        webView = WebView(context).also {
            it.layoutParams = ViewGroup.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        return webView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webView.webViewClient = WebViewClient()
        closeButtonStyle = CloseButtonStyle.CloseOnly

        if (urlString.isEmpty()) return
        webView.loadUrl(urlString)
    }
}