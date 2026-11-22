package com.example.apirequestsender

import android.app.Activity
import android.os.Bundle
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

class MainActivity : Activity() {
    private lateinit var urlInput: EditText
    private lateinit var bodyInput: EditText
    private lateinit var proxyHostInput: EditText
    private lateinit var proxyPortInput: EditText
    private lateinit var proxyUserInput: EditText
    private lateinit var proxyPassInput: EditText
    private lateinit var statusView: TextView
    private lateinit var logView: TextView
    private lateinit var sendButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 24)
            setBackgroundColor(0xFFF7F8FA.toInt())
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)

        content.addView(label("API Request Sender", 24f, true))
        content.addView(label("Send authorized HTTPS POST requests with optional proxy.", 14f, false))
        urlInput = field("Request URL (https://...)")
        content.addView(urlInput)
        bodyInput = field("JSON request body", 4).apply {
            setText("{\"test\":\"hello\"}")
            typeface = Typeface.MONOSPACE
        }
        content.addView(bodyInput)
        content.addView(label("Proxy settings (optional)", 18f, true))
        proxyHostInput = field("Proxy host")
        content.addView(proxyHostInput)
        proxyPortInput = field("Proxy port")
        content.addView(proxyPortInput)
        proxyUserInput = field("Proxy username")
        content.addView(proxyUserInput)
        proxyPassInput = field("Proxy password")
        proxyPassInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or
            android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        content.addView(proxyPassInput)

        sendButton = Button(this).apply {
            text = "SEND POST REQUEST"
            setOnClickListener { sendRequest() }
        }
        content.addView(sendButton)
        statusView = label("Status: Waiting", 16f, true)
        content.addView(statusView)
        content.addView(label("Response / log", 18f, true))
        logView = TextView(this).apply {
            text = "No request sent yet."
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            setPadding(16, 16, 16, 16)
            setBackgroundColor(0xFFE8EBF0.toInt())
        }
        content.addView(logView)
        outer.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(outer)
    }

    private fun label(text: String, size: Float, bold: Boolean) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(0xFF172033.toInt())
        if (bold) setTypeface(null, Typeface.BOLD)
        setPadding(0, 12, 0, 12)
    }

    private fun field(hintText: String, lines: Int = 1) = EditText(this).apply {
        hint = hintText
        textSize = 14f
        setSingleLine(lines == 1)
        if (lines > 1) {
            minLines = lines
            gravity = android.view.Gravity.TOP
        }
        setPadding(14, 10, 14, 10)
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            if (lines == 1) ViewGroup.LayoutParams.WRAP_CONTENT else 150
        )
        lp.setMargins(0, 5, 0, 8)
        layoutParams = lp
    }

    private fun sendRequest() {
        val url = urlInput.text.toString().trim()
        val json = bodyInput.text.toString()
        if (!url.startsWith("https://")) {
            Toast.makeText(this, "Enter an HTTPS URL.", Toast.LENGTH_SHORT).show()
            return
        }
        val builder = OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)

        val host = proxyHostInput.text.toString().trim()
        val portText = proxyPortInput.text.toString().trim()
        val username = proxyUserInput.text.toString()
        val password = proxyPassInput.text.toString()

        if (host.isNotEmpty() || portText.isNotEmpty()) {
            val port = portText.toIntOrNull()
            if (host.isEmpty() || port == null || port !in 1..65535) {
                Toast.makeText(this, "Enter a valid proxy host and port.", Toast.LENGTH_SHORT).show()
                return
            }
            builder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port)))
            if (username.isNotEmpty()) {
                builder.proxyAuthenticator { _, response ->
                    if (response.request.header("Proxy-Authorization") != null) null
                    else response.request.newBuilder()
                        .header("Proxy-Authorization", Credentials.basic(username, password))
                        .build()
                }
            }
        }

        val request = try {
            Request.Builder().url(url)
                .post(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .build()
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid URL or request: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }

        sendButton.isEnabled = false
        statusView.text = "Status: Sending..."
        logView.text = "Connecting..."
        Thread {
            try {
                builder.build().newCall(request).execute().use { response ->
                    val responseText = response.body?.string() ?: "(empty response body)"
                    runOnUiThread {
                        statusView.text = "HTTP status: ${response.code} ${response.message}"
                        logView.text = responseText
                        sendButton.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusView.text = "Status: Request failed"
                    logView.text = "${e.javaClass.simpleName}: ${e.message}"
                    sendButton.isEnabled = true
                }
            }
        }.start()
    }
}
