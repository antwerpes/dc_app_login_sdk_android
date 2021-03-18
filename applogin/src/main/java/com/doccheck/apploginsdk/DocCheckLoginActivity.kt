package com.doccheck.apploginsdk

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.Serializable

class DocCheckLoginActivity: AppCompatActivity() {

    private var webview: WebView? = null
    private var isConnected = false
    private var loginUrl: Uri? = null

    private val scope = CoroutineScope(
        Job() + Dispatchers.IO
    )

    private val uiScope = CoroutineScope(
        Job() + Dispatchers.Main
    )

    private val httpRequestManager = HTTPRequestManager()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_doccheck_app_login)

        val connectionManager = this.getSystemService(Context.CONNECTIVITY_SERVICE)

        // TODO: change to callbacks and react to network changes
        if (connectionManager is ConnectivityManager && connectionManager.activeNetworkInfo != null) {
            isConnected = connectionManager.activeNetworkInfo?.isConnectedOrConnecting() == true
        }

        if (!isConnected) {
            AlertDialog.Builder(this).apply {
                setTitle("Login")
                setMessage("The Internet connection appears to be offline")
                setOnDismissListener {
                    finishWebView(LoginState.CANCELED)
                }
            }.show()
            return
        }

        val extras = intent.extras
        if (extras == null || extras.isEmpty) {
            throw Exception("No extras added to the Intent")
        }

        val loginId = extras.getString("loginId")

        if (loginId.isNullOrEmpty()) {
            throw Exception("loginId is missing")
        }

        val language = extras.getString("language")

        if (language.isNullOrEmpty()) {
            throw Exception("countryCode is missing")
        }

        val templateName = extras.getString("templateName") ?: "s_mobile"

        if (templateName.isNullOrEmpty()) {
            Log.e("DocCheckLoginApp","templateName is missing and will be defaulted to \"s_mobile\"")
        }

        val loginUrl = "https://login.doccheck.com/code/$language/$loginId/$templateName"
        val applicationId = packageName

        webview = findViewById(R.id.dc_login_webview)
        webview?.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = DocCheckWebViewClient(
                applicationId,
                ::finishWebView,
                ::validateToken
            )
            loadUrl(loginUrl)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        if (menu == null) {
            return true
        }
        menu.add(0, Menu.FIRST, Menu.NONE, "Back").apply {
            setIcon(R.drawable.arrowleft)
        }
        menu.add(0, Menu.FIRST + 1, Menu.NONE, "Forward").apply {
            isEnabled = false
            setIcon(R.drawable.arrowright)
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (isConnected && menu != null) {
            menu.findItem(Menu.FIRST + 1).apply {
                isEnabled = webview?.canGoForward() == true
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            Menu.FIRST -> {
                if (webview?.canGoBack() == true) {
                    webview?.goBack()
                } else {
                    finishWebView(LoginState.CANCELED)
                }
            }
            Menu.FIRST + 1 -> {
                if (webview?.canGoForward() == true) {
                    webview?.goForward()
                }
            }
            else -> {}
        }
        return true
    }

    override fun onBackPressed() {
        if (webview?.canGoBack() == true) {
            webview?.goBack()
        } else {
            finishWebView(LoginState.CANCELED)
        }
    }

    override fun onDestroy() {
        if (webview != null) {
            webview = null
        }
        super.onDestroy()
    }

    fun finishWebView(loginState: LoginState) {
        uiScope.launch {
            val resultIntent = Intent().apply {
                when(loginState) {
                    LoginState.CANCELED -> putExtra("RESPONSE", "CANCEL")
                    LoginState.FAILED -> putExtra("RESPONSE", "ERROR")
                    LoginState.SUCCEEDED -> putExtra("RESPONSE", "SUCCEEDED")
                }
                putExtra("LOGIN_RESULT", loginState == LoginState.SUCCEEDED)
                if ( loginState == LoginState.SUCCEEDED && loginUrl != null) {
                    val params = mutableMapOf<String, List<String>?>()
                    loginUrl?.queryParameterNames?.forEach { key ->
                        params += key to loginUrl?.getQueryParameters(key)
                    }
                    putExtra("URLPARAMS", params.toMap() as Serializable)
                }
            }
            setResult(
                when(loginState) {
                    LoginState.CANCELED, LoginState.FAILED -> RESULT_CANCELED
                    LoginState.SUCCEEDED -> RESULT_OK
                },
                resultIntent)
            finish()
        }
    }


    private fun validateToken(token: String, loginUrl: Uri) {
        this.loginUrl = loginUrl
        scope.launch {
            httpRequestManager.get(
                requestUrl = "https://login.doccheck.com/soap/token/checkToken.php?json=1&strToken=$token",
                success = ::onTokenSuccess,
                failure = ::onTokenFailure
            )
        }
    }

    private fun onTokenSuccess(result: String) {
        Log.w("TEST_SUCCESS", result)
        val json = JSONObject(result)
        json.keys().forEach { key ->
            Log.w("TEST_SUCCESS", "$key: ${json.get(key)}")
        }

        Log.w("TEST_SUCCESS", "boolValid is ${json.get("boolValid") == 1}")
        if (json.get("boolValid") == 1) {
            finishWebView(LoginState.SUCCEEDED)
        }

    }

    private fun onTokenFailure(result: String, errorCode: Int) {
        Log.w("TEST_FAILED", result)
        Log.w("TEST_FAILED", "errorCode: $errorCode")
        finishWebView(LoginState.FAILED)
    }

    private class DocCheckWebViewClient(
        val applicationId: String,
        val finishWebView: (loginState: LoginState) -> Unit,
        val validateToken: (token: String, loginUrl: Uri) -> Unit
    ): WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            try {
                if (request != null) {
                    if (isDocCheckLoginRequest(request.url)) {
                        handleLoginForUrl(request.url)
                        return true
                    } else {
                        view?.loadUrl(request.url.toString())
                        return true
                    }
                }
            } catch (e: Exception) {
                finishWebView(LoginState.FAILED)
            }

            return false
        }

        private fun isDocCheckLoginRequest(url: Uri): Boolean {
            val scheme = url.scheme ?: ""
            val action = url.host ?: ""
            if (!"doccheck".equals(scheme) || !"login".equals(action)) {
                return false
            }

            return true
        }

        private fun handleLoginForUrl(url: Uri) {
            val appId = url.getQueryParameter("appid")

            if (appId.isNullOrEmpty()) {
                Log.e("DocCheckLoginApp", "No App id found - please make sure it is set in the return url, e.g. doccheck://login?appid=your.app.id")
                finishWebView(LoginState.FAILED)
                return
            }

            if (!applicationId.equals(appId)) {
                Log.e("DocCheckLoginApp", "login is not valid for app id \"$appId\" and application id \"$applicationId\" ")
                finishWebView(LoginState.FAILED)
                return
            }

            val token = url.getQueryParameter("dc_token")

            if (token.isNullOrEmpty()) {
                Log.e("DocCheckLoginApp", "received empty token")
                finishWebView(LoginState.FAILED)
                return
            }

            validateToken(token, url)
        }
    }
}

