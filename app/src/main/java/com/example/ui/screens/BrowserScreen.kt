package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.browser.RomBookmarks
import com.example.data.model.AtariSystem
import com.example.data.model.DownloadStatus
import com.example.data.model.DownloadTaskEntity
import com.example.ui.components.AtariHeader
import com.example.ui.theme.AtariAmber
import com.example.ui.theme.AtariBackground
import com.example.ui.theme.AtariCyan
import com.example.ui.theme.AtariOrange
import com.example.ui.theme.AtariRed
import com.example.ui.theme.AtariSurface
import com.example.ui.theme.AtariTextPrimary
import com.example.ui.theme.AtariTextSecondary
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.LocalAppStrings

private const val PORTAL_URL = "file:///android_asset/rom_portal.html"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    downloads: List<DownloadTaskEntity>,
    onStartDownload: (url: String, contentDisposition: String?) -> Unit,
    onPauseDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    onDeleteDownload: (String) -> Unit,
    onPlayDownloadedGame: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    var addressInput by remember { mutableStateOf(PORTAL_URL) }
    var pageProgress by remember { mutableIntStateOf(0) }
    var showQueue by remember { mutableStateOf(false) }
    var webViewGeneration by remember { mutableIntStateOf(0) }
    var activeWebView by remember { mutableStateOf<WebView?>(null) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val triggerDownload: (String, String?) -> Unit = { url, disposition ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        onStartDownload(url, disposition)
        showQueue = true
    }

    val navigateToUrl: (String) -> Unit = { raw ->
        val trimmed = raw.trim()
        val url = when {
            trimmed == PORTAL_URL || trimmed.startsWith("file:///") -> trimmed
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.contains('.') && !trimmed.contains(' ') -> "https://$trimmed"
            else -> "https://archive.org/search?query=${android.net.Uri.encode("$trimmed atari rom")}"
        }
        addressInput = url
        if (RomBookmarks.isRomLink(url)) {
            triggerDownload(url, null)
        } else {
            activeWebView?.loadUrl(url)
            showQueue = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        AtariHeader(
            title = strings.browserTitle,
            trailingAction = {
                IconButton(
                    onClick = { showQueue = !showQueue },
                    modifier = Modifier.testTag("toggle_downloads_queue")
                ) {
                    val activeCount = downloads.count {
                        it.status == DownloadStatus.DOWNLOADING.name || it.status == DownloadStatus.QUEUED.name
                    }
                    BadgedBox(
                        badge = {
                            if (downloads.isNotEmpty()) {
                                Badge(containerColor = if (activeCount > 0) AtariCyan else AtariOrange) {
                                    Text("${downloads.size}", color = Color.Black, fontSize = 10.sp)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = strings.downloadsQueue,
                            tint = if (showQueue) AtariOrange else AtariAmber
                        )
                    }
                }
            }
        )

        // Browser Address Bar & Nav Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navigateToUrl(PORTAL_URL) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = "Home Portal", tint = AtariOrange)
            }
            IconButton(
                onClick = { if (activeWebView?.canGoBack() == true) activeWebView?.goBack() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            IconButton(
                onClick = { if (activeWebView?.canGoForward() == true) activeWebView?.goForward() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", tint = Color.White)
            }
            IconButton(
                onClick = { activeWebView?.reload() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = AtariAmber)
            }

            Surface(
                color = AtariSurface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = if (addressInput == PORTAL_URL) "" else addressInput,
                        onValueChange = { addressInput = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = AtariTextPrimary),
                        cursorBrush = SolidColor(AtariOrange),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { navigateToUrl(addressInput) }),
                        decorationBox = { inner ->
                            if (addressInput == PORTAL_URL || addressInput.isBlank()) {
                                Text(
                                    text = strings.browserHint,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AtariTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            inner()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("browser_url_input")
                    )
                }
            }
        }

        // Bookmarks Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(RomBookmarks.presets) { bm ->
                FilterChip(
                    selected = addressInput.startsWith(bm.url),
                    onClick = { navigateToUrl(bm.url) },
                    leadingIcon = {
                        Icon(Icons.Default.Bookmark, contentDescription = null, tint = AtariAmber, modifier = Modifier.size(16.dp))
                    },
                    label = { Text("${bm.title} (${bm.badge})", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AtariOrange.copy(alpha = 0.25f),
                        selectedLabelColor = Color.White,
                        containerColor = AtariSurface,
                        labelColor = AtariTextSecondary
                    ),
                    border = null
                )
            }
        }

        if (pageProgress in 1..99 && !showQueue) {
            LinearProgressIndicator(
                progress = { pageProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = AtariCyan,
                trackColor = Color.Transparent
            )
        }

        // Keep WebView mounted inside a Box and overlay Download Queue when toggled
        // so WebView isn't repeatedly destroyed/re-parented.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 72.dp)
        ) {
            key(webViewGeneration) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(android.graphics.Color.parseColor("#0B0B12"))
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            settings.mediaPlaybackRequiresUserGesture = true
                            settings.setSupportMultipleWindows(false)
                            setDownloadListener { url, _, contentDisposition, _, _ ->
                                triggerDownload(url, contentDisposition)
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    pageProgress = newProgress
                                }
                            }
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    if (RomBookmarks.isRomLink(url)) {
                                        triggerDownload(url, null)
                                        return true
                                    }
                                    addressInput = url
                                    return false
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    if (!url.isNullOrBlank()) addressInput = url
                                }

                                override fun onRenderProcessGone(
                                    view: WebView?,
                                    detail: RenderProcessGoneDetail?
                                ): Boolean {
                                    // Recover gracefully instead of letting Chromium renderer crash kill the app
                                    if (view != null) {
                                        (view.parent as? ViewGroup)?.removeView(view)
                                        runCatching { view.destroy() }
                                    }
                                    activeWebView = null
                                    addressInput = PORTAL_URL
                                    webViewGeneration++
                                    return true
                                }
                            }
                            loadUrl(addressInput)
                            activeWebView = this
                        }
                    },
                    onRelease = { view ->
                        if (activeWebView === view) activeWebView = null
                        view.stopLoading()
                        (view.parent as? ViewGroup)?.removeView(view)
                        runCatching { view.destroy() }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("rom_browser_webview")
                )
            }

            if (showQueue) {
                Surface(
                    color = AtariBackground,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.downloadsQueue,
                                style = MaterialTheme.typography.titleMedium,
                                color = AtariAmber
                            )
                            Text(
                                text = strings.browserTitle,
                                style = MaterialTheme.typography.labelLarge,
                                color = AtariCyan,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showQueue = false }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (downloads.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "اضغط على أي ملف ROM (zip, a26, bin, a78, lnx, j64) في المتصفح ليبدأ التحميل وفك الضغط تلقائياً",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AtariTextSecondary
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(downloads, key = { it.id }) { task ->
                                    DownloadTaskCard(
                                        task = task,
                                        onPause = { onPauseDownload(task.id) },
                                        onResume = { onResumeDownload(task.id) },
                                        onDelete = { onDeleteDownload(task.id) },
                                        onPlay = {
                                            if (task.importedGameId.isNotBlank()) {
                                                onPlayDownloadedGame(task.importedGameId)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadTaskCard(
    task: DownloadTaskEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
    onPlay: () -> Unit
) {
    val strings = LocalAppStrings.current
    val status = runCatching { DownloadStatus.valueOf(task.status) }.getOrDefault(DownloadStatus.QUEUED)
    val sys = AtariSystem.fromCode(task.detectedSystem)
    val progress = if (task.totalBytes > 0L) {
        (task.bytesDownloaded.toFloat() / task.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        color = AtariSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.fileName,
                        fontFamily = ChakraPetchFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${sys.displayName} • ${task.bytesDownloaded / 1024} KB" +
                            if (task.totalBytes > 0) " / ${task.totalBytes / 1024} KB" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtariTextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (status) {
                        DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> {
                            IconButton(onClick = onPause) {
                                Icon(Icons.Default.Pause, contentDescription = strings.pause, tint = AtariAmber)
                            }
                        }
                        DownloadStatus.PAUSED, DownloadStatus.FAILED -> {
                            IconButton(onClick = onResume) {
                                Icon(Icons.Default.PlayArrow, contentDescription = strings.resume, tint = AtariCyan)
                            }
                        }
                        DownloadStatus.COMPLETED -> {
                            if (task.importedGameId.isNotBlank()) {
                                IconButton(onClick = onPlay) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = strings.playNow, tint = AtariOrange)
                                }
                            }
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = AtariRed)
                    }
                }
            }

            if (status != DownloadStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { if (task.totalBytes > 0L) progress else 0.35f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (status == DownloadStatus.FAILED) AtariRed else AtariCyan,
                    trackColor = Color(0xFF0B0B12)
                )
            }
            if (task.errorMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(task.errorMessage, color = AtariRed, fontSize = 11.sp)
            }
        }
    }
}
