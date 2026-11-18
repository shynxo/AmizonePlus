package com.student.amizoneplus.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.widget.Toast
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.student.amizoneplus.data.Downloads
import com.student.amizoneplus.data.Http
import com.student.amizoneplus.ui.kit.CircleButton
import com.student.amizoneplus.ui.kit.Quips
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.student.amizoneplus.data.AmizoneClient
import com.student.amizoneplus.ui.kit.Ink
import com.student.amizoneplus.ui.kit.T
import com.student.amizoneplus.ui.kit.press
import com.student.amizoneplus.ui.theme.L
import com.student.amizoneplus.ui.theme.P
import com.student.amizoneplus.ui.theme.Type
import kotlinx.coroutines.launch

// Makes Amizone's own (responsive) layout kick in on a phone. Nothing is hidden:
// the page keeps its real header and hamburger menu, exactly like a mobile browser.
private const val VIEWPORT_JS = """
(function(){try{
  var m=document.querySelector('meta[name=viewport]');
  if(!m){m=document.createElement('meta');m.name='viewport';document.head.appendChild(m);}
  m.content='width=device-width, initial-scale=1';
}catch(e){}})();"""

// What the loaded page is: the login form, Amizone's "unauthorised" stub, or a real page.
private const val STATE_JS = """
(function(){
  if(document.querySelector('input[name="_Password"]'))return 'login';
  var t=(document.body&&document.body.innerText||'').slice(0,400).toLowerCase();
  if(t.indexOf('unauthori')>=0)return 'unauth';
  return 'ok';
})()"""

/**
 * Amizone's sections are partial pages that its Home shell loads into itself
 * (opening one directly gives bare, unstyled HTML). So: open Home, then ask the
 * page to load the section the way its own menu does.
 */
private fun navJs(path: String) = """
(function(p){
  // Home pops its notice board on every visit; when a section was asked for, close it.
  function hideModals(){try{if(window.jQuery&&jQuery.fn.modal)jQuery('.modal.in,.modal.show').modal('hide');}catch(e){}}
  hideModals();setTimeout(hideModals,900);setTimeout(hideModals,2000);
  var lp=p.toLowerCase();
  var links=[].slice.call(document.querySelectorAll('a[href]'));
  var a=links.find(function(x){var h=(x.getAttribute('href')||'').toLowerCase();return h==lp||h.endsWith(lp);});
  if(a){a.click();return 'click';}
  var c=document.querySelector('#Divload,#divload,#Div_Partial,.page-content,.main-content');
  if(window.jQuery&&c){
    jQuery.ajax({url:p,headers:{'X-Requested-With':'XMLHttpRequest'}}).done(function(h){jQuery(c).html(h);window.scrollTo(0,0);});
    return 'ajax';
  }
  location.href=p;return 'nav';
})(${org.json.JSONObject.quote(path)})"""

@Composable
private fun SheetHeader(title: String, onClose: () -> Unit) {
    Ink(Modifier.padding(start = L.page, end = L.page, bottom = L.block)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            T(title, Type.title, ink = true, maxLines = 1, modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.Close, null, tint = P.onInk, modifier = Modifier.size(26.dp).press(onClose))
        }
    }
}

@Composable
private fun WebBar(title: String, host: String, onBack: () -> Unit, onReload: () -> Unit, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = L.page, end = L.page, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CircleButton(Icons.AutoMirrored.Outlined.ArrowBack, onBack, size = 44.dp)
        Column(Modifier.weight(1f)) {
            T(title, Type.title, maxLines = 1)
            T(host, Type.caption, faint = true, maxLines = 1)
        }
        CircleButton(Icons.Outlined.Refresh, onReload, size = 44.dp)
        CircleButton(Icons.Outlined.Close, onClose, size = 44.dp)
    }
}

/**
 * A signed-in Amizone page, shown the way a phone browser shows it (with Amizone's
 * own menu). Cookies and the browser identity are shared with the app's connection,
 * so it is normally already signed in; if not, the app signs in again and retries.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AuthedWebPage(url: String, title: String, relogin: suspend () -> Boolean, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val amizone = url.startsWith(AmizoneClient.BASE)
    val path = if (amizone) url.removePrefix(AmizoneClient.BASE).ifEmpty { "/" } else null
    val viaHome = path != null && !path.equals("/Home", true) && path != "/"
    val start = if (viaHome) AmizoneClient.BASE + "/Home" else url

    var cover by remember { mutableStateOf(true) }
    var progress by remember { mutableStateOf(0) }
    var host by remember { mutableStateOf(runCatching { android.net.Uri.parse(url).host.orEmpty() }.getOrDefault("")) }
    var web by remember { mutableStateOf<WebView?>(null) }
    var pending by remember { mutableStateOf(if (viaHome) path else null) }
    var tries by remember { mutableStateOf(0) }
    var ready by remember { mutableStateOf(!amizone) }
    val quip = remember { Quips.loading() }

    fun back() { val w = web; if (w != null && w.canGoBack()) w.goBack() else onClose() }
    BackHandler(onBack = ::back)

    // Sign in natively first, so the very first page load is already authorised.
    LaunchedEffect(Unit) {
        if (amizone) runCatching { relogin() }
        ready = true
    }

    Column(Modifier.fillMaxSize().background(P.bg).statusBarsPadding().padding(top = 10.dp)) {
        WebBar(title, host, ::back, { web?.reload() }, onClose)
        Box(Modifier.fillMaxWidth().height(2.dp)) {
            if (progress in 1..99) Box(Modifier.fillMaxWidth(progress / 100f).height(2.dp).background(P.peri))
        }
        Box(Modifier.fillMaxSize()) {
            if (ready) AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.userAgentString = Http.UA
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        setBackgroundColor(android.graphics.Color.WHITE)
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) { progress = newProgress }
                        }
                        val self = this
                        setDownloadListener { dUrl, _, disp, mime, _ ->
                            val name = URLUtil.guessFileName(dUrl, disp, mime)
                            Toast.makeText(context, "Downloading $name…", Toast.LENGTH_SHORT).show()
                            scope.launch {
                                runCatching { Downloads.open(context, dUrl, name, referer = self.url) }
                                    .onFailure { Toast.makeText(context, it.message ?: "Couldn't download that file.", Toast.LENGTH_LONG).show() }
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView, u: String?, favicon: Bitmap?) {
                                u?.let { host = runCatching { android.net.Uri.parse(it).host.orEmpty() }.getOrDefault(host) }
                            }
                            override fun onPageFinished(view: WebView, u: String?) {
                                view.evaluateJavascript(VIEWPORT_JS, null)
                                view.evaluateJavascript(STATE_JS) { raw ->
                                    val state = raw.orEmpty().trim('"')
                                    if (state != "ok" && amizone && tries < 2) {
                                        // Session expired: sign in again natively, then start over.
                                        tries++
                                        cover = true
                                        scope.launch { if (relogin()) view.loadUrl(start) else cover = false }
                                        return@evaluateJavascript
                                    }
                                    if (state == "login") { cover = false; return@evaluateJavascript } // let the user sign in by hand
                                    val target = pending
                                    if (target != null && u?.startsWith(AmizoneClient.BASE) == true) {
                                        pending = null
                                        view.evaluateJavascript(navJs(target)) { how ->
                                            if (how.orEmpty().trim('"') == "nav") return@evaluateJavascript
                                            view.postDelayed({ cover = false }, 700)
                                        }
                                    } else {
                                        cover = false
                                    }
                                }
                            }
                        }
                        web = this
                        loadUrl(start, mapOf("Referer" to AmizoneClient.BASE + "/Home"))
                    }
                },
                onRelease = { it.stopLoading(); it.destroy() },
            )
            androidx.compose.animation.AnimatedVisibility(cover, enter = fadeIn(), exit = fadeOut(tween(260))) {
                Column(Modifier.fillMaxSize().background(P.bg), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = P.text)
                    T("Opening $title", Type.label, modifier = Modifier.padding(top = 16.dp))
                    T(quip, Type.caption, sub = true, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

/**
 * Fallback sign-in on the real Amizone login page (if Amizone ever rejects the
 * automatic sign-in). Pre-fills your details; closes itself once you're in.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebLoginSheet(username: String?, password: String?, onDone: () -> Unit, onClose: () -> Unit) {
    var done by remember { mutableStateOf(false) }
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().background(P.canvas).statusBarsPadding().padding(top = L.page)) {
        SheetHeader("Sign in to Amizone", onClose)
        T("The app is fine: Amizone just asks every new sign-in to prove you're human. Your details are filled in. Tick the box, tap Login, and this closes by itself.", Type.caption, sub = true, modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp))
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = Http.UA
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, u: String?) {
                            val path = runCatching { android.net.Uri.parse(u).path.orEmpty() }.getOrDefault("")
                            if (!done && u?.startsWith(AmizoneClient.BASE) == true && path.isNotEmpty() && path != "/" && !path.contains("login", true)) {
                                done = true
                                CookieManager.getInstance().flush()
                                onDone()
                                return
                            }
                            if (username != null && password != null) {
                                val js = "(function(){var u=document.querySelector('[name=\"_UserName\"]'),p=document.querySelector('[name=\"_Password\"]');" +
                                    "if(u)u.value=${org.json.JSONObject.quote(username)};if(p)p.value=${org.json.JSONObject.quote(password)};})();"
                                view.evaluateJavascript(js, null)
                            }
                        }
                    }
                    loadUrl(AmizoneClient.BASE + "/")
                }
            },
        )
    }
}

@Composable
fun Spinner(modifier: Modifier = Modifier) =
    Box(modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = P.ink) }
