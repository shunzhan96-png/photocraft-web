package com.shunzhan96.photocraftmobile

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.Executors

/**
 * Small, isolated on-device proof of concept. It deliberately does not replace the existing editor.
 */
class NativeEngineTestActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var status: TextView
    private lateinit var brightnessLabel: TextView
    private lateinit var contrastLabel: TextView
    private lateinit var preview: ImageView
    private lateinit var runButton: Button
    private var selectedBytes: ByteArray? = null
    private var resultBytes: ByteArray? = null
    private var brightness = 0
    private var contrast = 0

    companion object {
        private const val PICK_IMAGE = 5101
        private const val MAX_INPUT_BYTES = 30 * 1024 * 1024
        private const val BLUE = 0xFF356AE6.toInt()
        private const val TEXT = 0xFF1D2433.toInt()
        private const val MUTED = 0xFF667085.toInt()
        private const val SURFACE = 0xFFF5F6F8.toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "原生引擎测试"
        window.statusBarColor = SURFACE
        window.navigationBarColor = SURFACE
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(16))
            setBackgroundColor(SURFACE)
        }
        root.addView(TextView(this).apply {
            text = "PhotoCraft 原生引擎验证"
            textSize = 22f
            setTextColor(TEXT)
        })
        root.addView(TextView(this).apply {
            text = "选择一张图片，在手机本地运行亮度/对比度处理。此页面只用于验证原生引擎，不会修改原图。"
            textSize = 14f
            setTextColor(MUTED)
            setPadding(0, dp(8), 0, dp(12))
        })
        root.addView(Button(this).apply {
            text = "选择测试照片"
            setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                }
                startActivityForResult(intent, PICK_IMAGE)
            }
        }, LinearLayout.LayoutParams(-1, -2))

        status = TextView(this).apply {
            text = "请先选择一张照片"
            textSize = 13f
            setTextColor(MUTED)
            setPadding(0, dp(8), 0, dp(4))
        }
        root.addView(status)

        brightnessLabel = TextView(this).apply { setTextColor(TEXT); textSize = 14f }
        root.addView(brightnessLabel)
        root.addView(SeekBar(this).apply {
            max = 300
            progress = 150
            setOnSeekBarChangeListener(simpleListener { value ->
                brightness = value - 150
                brightnessLabel.text = "亮度：$brightness"
                resultBytes = null
            })
        })

        contrastLabel = TextView(this).apply { setTextColor(TEXT); textSize = 14f }
        root.addView(contrastLabel)
        root.addView(SeekBar(this).apply {
            max = 150
            progress = 50
            setOnSeekBarChangeListener(simpleListener { value ->
                contrast = value - 50
                contrastLabel.text = "对比度：$contrast"
                resultBytes = null
            })
        })

        preview = ImageView(this).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "原生处理结果预览"
        }
        root.addView(preview, LinearLayout.LayoutParams(-1, 0, 1f).apply {
            topMargin = dp(8)
            bottomMargin = dp(8)
        })

        runButton = Button(this).apply {
            text = "运行原生处理"
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(BLUE)
            isEnabled = false
            setOnClickListener { runNativeAdjustment() }
        }
        root.addView(runButton, LinearLayout.LayoutParams(-1, -2))
        root.addView(Button(this).apply {
            text = "保存处理结果 PNG"
            setOnClickListener { saveResult() }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
        root.addView(Button(this).apply {
            text = "返回 PhotoCraft"
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })

        brightnessLabel.text = "亮度：0"
        contrastLabel.text = "对比度：0"
        setContentView(root)
    }

    private fun simpleListener(onProgress: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) = onProgress(progress)
        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK_IMAGE || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try {
            val bytes = contentResolver.openInputStream(uri)?.use { readBounded(it) }
                ?: throw IllegalStateException("无法读取图片")
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: throw IllegalArgumentException("暂不支持读取这张图片")
            selectedBytes = bytes
            resultBytes = null
            preview.setImageBitmap(bitmap)
            status.text = "已载入图片（${bytes.size / 1024} KB）。点击「运行原生处理」开始验证。"
            runButton.isEnabled = true
        } catch (e: Exception) {
            selectedBytes = null
            runButton.isEnabled = false
            status.text = "读取失败：${e.message ?: "未知错误"}"
        }
    }

    private fun readBounded(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > MAX_INPUT_BYTES) throw IllegalArgumentException("图片超过 30 MB，请换一张较小的图片")
            out.write(buffer, 0, count)
        }
        return out.toByteArray()
    }

    private fun runNativeAdjustment() {
        val input = selectedBytes ?: return
        runButton.isEnabled = false
        status.text = "正在手机本地处理，请稍候……"
        executor.execute {
            try {
                val output = NativePhotoCraft.adjustPng(input, brightness, contrast)
                val bitmap = BitmapFactory.decodeByteArray(output, 0, output.size)
                    ?: throw IllegalStateException("引擎返回的数据无法解码为图片")
                runOnUiThread {
                    resultBytes = output
                    preview.setImageBitmap(bitmap)
                    status.text = "原生处理成功：输出 PNG，${output.size / 1024} KB。可保存结果。"
                    runButton.isEnabled = true
                }
            } catch (error: Throwable) {
                runOnUiThread {
                    status.text = "原生处理失败：${error.message ?: error.javaClass.simpleName}"
                    runButton.isEnabled = true
                    Toast.makeText(this, "请截图反馈此错误，便于继续排查", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun saveResult() {
        val bytes = resultBytes
        if (bytes == null) {
            Toast.makeText(this, "请先成功运行一次原生处理", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "PhotoCraft-native-${System.currentTimeMillis()}.png")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PhotoCraft")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri: Uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("无法创建保存文件")
            contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: throw IllegalStateException("无法写入保存文件")
            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            contentResolver.update(uri, done, null, null)
            Toast.makeText(this, "已保存到相册/PhotoCraft", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "保存失败：${e.message ?: "未知错误"}", Toast.LENGTH_LONG).show()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}
