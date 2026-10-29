package com.maralyrics.laitei.presentation.account

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.maralyrics.laitei.BuildConfig
import com.maralyrics.laitei.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AccountWebMode {
    /** Creating an account, or passing the security check: close as soon as the page has signed in. */
    SIGN_IN,

    /** The signed-in account page (photo, password, data, deleting the account). */
    MANAGE
}

/**
 * The website's own account page, inside the app. See [AccountWeb] for why: it is where the
 * security check, account creation and account deletion live. The session token is moved between
 * the page and the app so the reader signs in once, not twice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AccountWebScreen(
    mode: AccountWebMode,
    onClose: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loading by remember { mutableStateOf(true) }
    val tokenAtStart = remember { viewModel.tokenForWebsite() }
    var lastToken by remember { mutableStateOf(tokenAtStart) }
    // The page starts with no sign-in until the app writes ours in, so an empty page only means
    // "signed out there" after it has been seen holding our token.
    var pageHeldOurToken by remember { mutableStateOf(false) }

    // Read what the page has saved once a second: the site signs in with JavaScript, with no
    // page change the app could otherwise notice.
    LaunchedEffect(webView) {
        val view = webView ?: return@LaunchedEffect
        while (true) {
            delay(1_000)
            if (!AccountWeb.isAllowedPage(view.url)) continue
            view.evaluateJavascript(AccountWeb.readTokenScript) { raw ->
                val pageToken = AccountWeb.parseScriptResult(raw) ?: return@evaluateJavascript
                scope.launch {
                    when {
                        pageToken == lastToken -> pageHeldOurToken = true
                        pageToken.isNotEmpty() -> {
                            if (viewModel.adoptWebsiteToken(pageToken)) {
                                lastToken = pageToken
                                if (mode == AccountWebMode.SIGN_IN) onClose()
                            }
                        }
                        // Signed out (or the account was deleted) on the page: do the same here.
                        mode == AccountWebMode.MANAGE && lastToken != null && pageHeldOurToken -> {
                            viewModel.signOut()
                            onClose()
                        }
                    }
                }
            }
        }
    }

    // Whatever the page changed about the account (a new username, a new photo) is picked up on the way out.
    DisposableEffect(Unit) {
        onDispose {
            webView?.apply { stopLoading(); destroy() }
            viewModel.refresh()
        }
    }

    BackHandler(enabled = webView?.canGoBack() == true) { webView?.goBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.account_web_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.account_web_close))
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        // Debug builds only: lets the page be inspected from a computer, and logs its JavaScript errors.
                        if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                                if (BuildConfig.DEBUG) {
                                    Log.d("AccountWeb", "${message.messageLevel()} ${message.message()} (${message.sourceId()}:${message.lineNumber()})")
                                }
                                return true
                            }
                        }
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.setSupportMultipleWindows(false)
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                if (AccountWeb.isAllowedPage(request.url.toString())) return false
                                // Anything that isn't the website opens in the browser, not in this window.
                                if (request.url.scheme == "https" || request.url.scheme == "http") {
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(request.url.toString()))) }
                                }
                                return true
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                if (url == AccountWeb.PREPARE_URL) {
                                    // Step one done: write into the site's storage, then open the account page.
                                    val token = tokenAtStart.takeIf { mode == AccountWebMode.MANAGE }
                                    view.evaluateJavascript(AccountWeb.prepareScript(token)) {
                                        view.loadUrl(AccountWeb.ACCOUNT_URL)
                                    }
                                    return
                                }
                                if (AccountWeb.isAllowedPage(url)) view.evaluateJavascript(AccountWeb.hideGoogleSignInScript, null)
                                loading = false
                            }
                        }
                        loadUrl(AccountWeb.PREPARE_URL)
                        webView = this
                    }
                }
            )
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
