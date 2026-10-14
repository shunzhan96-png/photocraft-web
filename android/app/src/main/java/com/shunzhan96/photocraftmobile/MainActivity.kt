package com.shunzhan96.photocraftmobile

import android.app.Activity
import android.app.DownloadManager
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var editorWebView: WebView
    private lateinit var editorPanel: FrameLayout
    private lateinit var directorPanel: ScrollView
    private lateinit var imageStatus: TextView
    private lateinit var goalInput: EditText
    private var selectedImageUri: Uri? = null
    private var webFileCallback: ValueCallback<Array<Uri>>? = null

    companion object {
        private const val PHOTOCRAFT_URL = "https://shunzhan96-png.github.io/photocraft-web/"
        private const val REQUEST_WEB_FILE = 4101
        private const val REQUEST_DIRECTOR_IMAGE = 4102
        private const val BLUE = 0xFF356AE6.toInt()
        private const val TEXT = 0xFF1D2433.toInt()
        private const val MUTED = 0xFF667085.toInt()
        private const val SURFACE = 0xFFF5F6F8.toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = SURFACE
        window.navigationBarColor = SURFACE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        buildUi()
        configureEditor()
        if (savedInstanceState == null) editorWebView.loadUrl(PHOTOCRAFT_URL)
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(SURFACE)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(Color.WHITE)
        }
        header.addView(TextView(this).apply {
            text = "PhotoCraft"
            textSize = 20f
            setTextColor(TEXT)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(2), dp(4), dp(8))
        }, LinearLayout.LayoutParams(-1, -2))

        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        nav.addView(navButton("编辑器") { showEditor() }, weightedParams())
        nav.addView(navButton("AI 创意导演") { showDirector() }, weightedParams())
        nav.addView(navButton("Chrome 打开") { openInChrome() }, weightedParams())
        nav.addView(navButton("原生测试") { startActivity(Intent(this, NativeEngineTestActivity::class.java)) }, weightedParams())
        header.addView(nav, LinearLayout.LayoutParams(-1, -2))
        root.addView(header, LinearLayout.LayoutParams(-1, -2))

        editorPanel = FrameLayout(this)
        editorWebView = WebView(this)
        editorPanel.addView(editorWebView, FrameLayout.LayoutParams(-1, -1))
        root.addView(editorPanel, LinearLayout.LayoutParams(-1, 0, 1f))

        directorPanel = buildDirectorPanel()
        root.addView(directorPanel, LinearLayout.LayoutParams(-1, 0, 1f))
        directorPanel.visibility = View.GONE
        setContentView(root)
    }

    private fun buildDirectorPanel(): ScrollView {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(24))
        }
        content.addView(TextView(this).apply {
            text = "先分析，再确认，再编辑"
            textSize = 22f
            setTextColor(TEXT)
        }, LinearLayout.LayoutParams(-1, -2))
        content.addView(TextView(this).apply {
            text = "选择一张参考照片，写下你想要的效果，然后把照片和导演指令分享给 ChatGPT 或其他支持图片的 AI。AI 先给创意方向；你确认后，再按步骤在 PhotoCraft 里操作。"
            textSize = 14f
            setTextColor(MUTED)
            setPadding(0, dp(8), 0, dp(16))
        }, LinearLayout.LayoutParams(-1, -2))
        content.addView(Button(this).apply {
            text = "选择参考照片"
            setOnClickListener { pickDirectorImage() }
        }, LinearLayout.LayoutParams(-1, -2))
        imageStatus = TextView(this).apply {
            text = "尚未选择照片（可选）"
            textSize = 13f
            setTextColor(MUTED)
            setPadding(dp(4), dp(4), dp(4), dp(12))
        }
        content.addView(imageStatus, LinearLayout.LayoutParams(-1, -2))
        content.addView(TextView(this).apply {
            text = "这张图想做成什么效果？"
            textSize = 15f
            setTextColor(TEXT)
        }, LinearLayout.LayoutParams(-1, -2))
        goalInput = EditText(this).apply {
            hint = "例如：自然真实的人像精修，保留皮肤质感，适合视频号发布"
            minLines = 3
            maxLines = 5
            gravity = Gravity.TOP or Gravity.START
            textSize = 15f
        }
        content.addView(goalInput, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(6)
            bottomMargin = dp(12)
        })
        content.addView(Button(this).apply {
            text = "把照片和需求发送给 AI"
            setTextColor(Color.WHITE)
            setBackgroundColor(BLUE)
            setOnClickListener { shareWithAi() }
        }, LinearLayout.LayoutParams(-1, -2))
        content.addView(TextView(this).apply {
            text = "当前版本说明\n• AI 在你选择的外部 AI 应用中分析照片，不需要在本 App 内配置 API。\n• PhotoCraft 编辑器仍在本机浏览器内核中运行。\n• 本版不会擅自替你执行编辑。AI 给出方案后，你确认方向并按步骤操作。直接驱动编辑器的自动化还需要额外验证。\n• 编辑器加载或保存异常时，可点顶部「Chrome 打开」使用你已经验证过的手机浏览器。"
            textSize = 13f
            setTextColor(MUTED)
            setPadding(0, dp(18), 0, 0)
        }, LinearLayout.LayoutParams(-1, -2))
        return ScrollView(this).apply { addView(content) }
    }

    private fun configureEditor() {
        editorWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = true
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
        }
        editorWebView.addJavascriptInterface(DownloadBridge(this), "AndroidDownloads")
        editorWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (uri.scheme == "https" && uri.host == "shunzhan96-png.github.io") return false
                return try {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                    true
                } catch (_: Exception) {
                    Toast.makeText(this@MainActivity, "无法打开这个链接", Toast.LENGTH_SHORT).show()
                    true
                }
            }
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                if (url.startsWith(PHOTOCRAFT_URL)) installBlobDownloadBridge()
            }
        }
        editorWebView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                webFileCallback?.onReceiveValue(null)
                webFileCallback = filePathCallback
                return try {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                    }
                    startActivityForResult(intent, REQUEST_WEB_FILE)
                    true
                } catch (_: Exception) {
                    webFileCallback = null
                    filePathCallback?.onReceiveValue(null)
                    Toast.makeText(this@MainActivity, "无法打开文件选择器", Toast.LENGTH_SHORT).show()
                    false
                }
            }
        }
        editorWebView.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            if (!url.startsWith("blob:")) {
                try {
                    val request = DownloadManager.Request(Uri.parse(url))
                    request.setTitle("PhotoCraft 导出文件")
                    request.setDescription("正在保存到下载目录")
                    request.setMimeType(mimeType)
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        "PhotoCraft/" + safeFilename(contentDisposition, url)
                    )
                    CookieManager.getInstance().getCookie(url)?.let { request.addRequestHeader("Cookie", it) }
                    if (userAgent.isNotBlank()) request.addRequestHeader("User-Agent", userAgent)
                    (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                    Toast.makeText(this, "已开始下载", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    openInChrome()
                }
            }
        })
    }

    private fun installBlobDownloadBridge() {
        val script = """
            (function() {
              if (window.__photoCraftNativeDownloadPatch) return;
              window.__photoCraftNativeDownloadPatch = true;
              const originalCreate = URL.createObjectURL.bind(URL);
              const blobs = new Map();
              URL.createObjectURL = function(blob) {
                const url = originalCreate(blob);
                if (blob instanceof Blob) blobs.set(url, blob);
                return url;
              };
              document.addEventListener('click', function(event) {
                const link = event.target && event.target.closest ? event.target.closest('a[download]') : null;
                if (!link || !link.href || !link.href.startsWith('blob:')) return;
                const blob = blobs.get(link.href);
                if (!blob || !window.AndroidDownloads) return;
                event.preventDefault();
                const reader = new FileReader();
                reader.onload = function() {
                  const dataUrl = String(reader.result || '');
                  const comma = dataUrl.indexOf(',');
                  if (comma < 0) return;
                  window.AndroidDownloads.saveBlob(link.download || 'PhotoCraft-export.bin',
                    blob.type || 'application/octet-stream', dataUrl.substring(comma + 1));
                };
                reader.readAsDataURL(blob);
              }, true);
            })();
        """.trimIndent()
        editorWebView.evaluateJavascript(script, null)
    }

    private fun showEditor() {
        directorPanel.visibility = View.GONE
        editorPanel.visibility = View.VISIBLE
    }

    private fun showDirector() {
        editorPanel.visibility = View.GONE
        directorPanel.visibility = View.VISIBLE
    }

    private fun openInChrome() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PHOTOCRAFT_URL)))
        } catch (_: Exception) {
            Toast.makeText(this, "没有可用的浏览器", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickDirectorImage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent, REQUEST_DIRECTOR_IMAGE)
    }

    private fun shareWithAi() {
        val goal = goalInput.text.toString().trim()
        val prompt = "你现在是我的 PhotoCraft AI 创意导演与修图教练。请先分析我分享的参考照片，结合我的目标提出 2–3 个真正不同的创意方向，并说明各自效果、适用场景、风险和难度。第一轮先不要给冗长的操作清单，也不要直接替我决定。等我确认方向后，再按手机端 PhotoCraft 的实际界面，每次给不超过 3–5 个可执行步骤，写清目的、要寻找的工具、观察标准和如何回退。不要编造没有确认的菜单或按钮；如果不确定，请先问我或让我发界面截图。不要声称你已操作编辑器。" +
            if (goal.isNotBlank()) "\n\n我的创作目标：" + goal else "\n\n如果我没有补充目标，请先根据照片本身给出适合的创作方向。"
        val uri = selectedImageUri
        val share = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, prompt)
            if (uri != null) {
                type = contentResolver.getType(uri) ?: "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(contentResolver, "PhotoCraft reference image", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
        }
        try {
            startActivity(Intent.createChooser(share, "选择 AI 助手"))
        } catch (_: Exception) {
            Toast.makeText(this, "没有可用于分享的应用", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_WEB_FILE -> {
                val callback = webFileCallback
                webFileCallback = null
                val result = if (resultCode == Activity.RESULT_OK && data?.data != null) arrayOf(data.data!!) else null
                callback?.onReceiveValue(result)
            }
            REQUEST_DIRECTOR_IMAGE -> {
                if (resultCode == Activity.RESULT_OK) {
                    selectedImageUri = data?.data
                    imageStatus.text = if (selectedImageUri != null) {
                        "已选择参考照片：" + displayName(selectedImageUri!!)
                    } else "尚未选择照片（可选）"
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (directorPanel.visibility == View.VISIBLE) {
            showEditor()
        } else if (::editorWebView.isInitialized && editorWebView.canGoBack()) {
            editorWebView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private fun displayName(uri: Uri): String {
        var name = "已选照片"
        contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index >= 0) name = cursor.getString(index) ?: name
            }
        }
        return name
    }

    private fun safeFilename(contentDisposition: String?, url: String): String {
        val fromHeader = contentDisposition?.substringAfter("filename=", "")?.trim('"', ' ', ';')
        val fromUrl = Uri.parse(url).lastPathSegment
        val candidate = fromHeader?.takeIf { it.isNotBlank() } ?: fromUrl ?: "photocraft-download.bin"
        return candidate.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[^A-Za-z0-9._ -]"), "_").take(100).ifBlank { "photocraft-download.bin" }
    }

    private fun navButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 12f
            setOnClickListener { action() }
        }

    private fun weightedParams() = LinearLayout.LayoutParams(0, dp(46), 1f).apply {
        marginStart = dp(2)
        marginEnd = dp(2)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private class DownloadBridge(private val activity: MainActivity) {
        @JavascriptInterface
        fun saveBlob(filename: String, mimeType: String, base64Data: String) {
            try {
                val safeName = filename.substringAfterLast('/').substringAfterLast('\\')
                    .replace(Regex("[^A-Za-z0-9._ -]"), "_").take(100).ifBlank { "PhotoCraft-export.png" }
                val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType.ifBlank { "application/octet-stream" })
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PhotoCraft")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = activity.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("Could not create download entry")
                activity.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    ?: throw IllegalStateException("Could not open download stream")
                val complete = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                activity.contentResolver.update(uri, complete, null, null)
                activity.runOnUiThread {
                    Toast.makeText(activity, "已保存到 下载/PhotoCraft/" + safeName, Toast.LENGTH_LONG).show()
                }
            } catch (error: Exception) {
                activity.runOnUiThread {
                    Toast.makeText(activity, "保存失败：" + (error.message ?: "未知错误") + "，请尝试 Chrome 打开", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
