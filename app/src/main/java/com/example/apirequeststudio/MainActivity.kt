package com.example.apirequeststudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Route
import okhttp3.Response
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

private val Background = Color(0xFF10142B)
private val CardColor = Color(0xFF1C2342)
private val Purple = Color(0xFF8B5CF6)
private val Cyan = Color(0xFF22D3EE)
private val Green = Color(0xFF34D399)
private val White = Color(0xFFF8FAFC)
private val Muted = Color(0xFFAAB3D0)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ApiRequestStudio()
        }
    }
}

private data class RequestResult(
    val code: Int,
    val message: String,
    val duration: Long,
    val headers: String,
    val body: String
)

@Composable
fun ApiRequestStudio() {
    val scope = rememberCoroutineScope()

    var url by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("GET") }
    var customMethod by remember { mutableStateOf("") }
    var methodMenu by remember { mutableStateOf(false) }

    var headers by remember { mutableStateOf("Accept: application/json") }
    var body by remember { mutableStateOf("") }

    var proxyHost by remember { mutableStateOf("") }
    var proxyPort by remember { mutableStateOf("8080") }
    var proxyUsername by remember { mutableStateOf("") }
    var proxyPassword by remember { mutableStateOf("") }

    var timeout by remember { mutableStateOf("30") }

    var loading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var responseHeaders by remember { mutableStateOf("") }
    var responseBody by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var elapsed by remember { mutableStateOf("") }

    val methods = listOf(
        "GET", "POST", "PUT", "PATCH",
        "DELETE", "HEAD", "OPTIONS", "TRACE", "CUSTOM"
    )

    val actualMethod =
        if (selectedMethod == "CUSTOM") customMethod.trim().uppercase()
        else selectedMethod

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Purple,
            secondary = Cyan,
            background = Background,
            surface = CardColor,
            onBackground = White,
            onSurface = White
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {

                // HEADER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF6D28D9),
                                    Color(0xFF2563EB),
                                    Color(0xFF0891B2)
                                )
                            ),
                            RoundedCornerShape(26.dp)
                        )
                        .padding(22.dp)
                ) {
                    Column {
                        Text(
                            "API REQUEST STUDIO",
                            fontSize = 23.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = White
                        )

                        Spacer(Modifier.height(7.dp))

                        Text(
                            "Powerful HTTP client for Android",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )

                        Spacer(Modifier.height(17.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(9.dp)
                                    .background(Green, RoundedCornerShape(50))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "READY TO SEND REQUESTS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                // REQUEST SECTION
                SectionCard(
                    title = "01  Request",
                    subtitle = "Choose a method and enter your endpoint"
                ) {
                    Box {
                        OutlinedButton(
                            onClick = { methodMenu = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(13.dp)
                        ) {
                            Text(
                                actualMethod.ifBlank { "SELECT METHOD" },
                                color = Cyan,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.weight(1f))
                            Text("▼", color = White)
                        }

                        DropdownMenu(
                            expanded = methodMenu,
                            onDismissRequest = { methodMenu = false }
                        ) {
                            methods.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method) },
                                    onClick = {
                                        selectedMethod = method
                                        methodMenu = false
                                    }
                                )
                            }
                        }
                    }

                    if (selectedMethod == "CUSTOM") {
                        Spacer(Modifier.height(12.dp))
                        AppTextField(
                            value = customMethod,
                            onValueChange = { customMethod = it },
                            label = "Custom HTTP method",
                            singleLine = true
                        )
                    }

                    Spacer(Modifier.height(13.dp))

                    AppTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = "Request URL (HTTPS)",
                        singleLine = true
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Examples: GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS, TRACE",
                        color = Muted,
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.height(15.dp))

                // HEADERS
                SectionCard(
                    title = "02  Headers",
                    subtitle = "One header per line: Name: Value"
                ) {
                    AppTextField(
                        value = headers,
                        onValueChange = { headers = it },
                        label = "Request headers",
                        minLines = 5
                    )
                }

                Spacer(Modifier.height(15.dp))

                // BODY
                SectionCard(
                    title = "03  Request Body",
                    subtitle = "JSON, text, or another raw request payload"
                ) {
                    AppTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = "Request body",
                        minLines = 7
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "GET and HEAD requests cannot contain a body.",
                        color = Muted,
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.height(15.dp))

                // PROXY
                SectionCard(
                    title = "04  Proxy Settings",
                    subtitle = "Optional HTTP proxy with authentication"
                ) {
                    AppTextField(
                        value = proxyHost,
                        onValueChange = { proxyHost = it },
                        label = "Proxy host",
                        singleLine = true
                    )

                    Spacer(Modifier.height(10.dp))

                    AppTextField(
                        value = proxyPort,
                        onValueChange = { proxyPort = it },
                        label = "Proxy port",
                        singleLine = true
                    )

                    Spacer(Modifier.height(10.dp))

                    AppTextField(
                        value = proxyUsername,
                        onValueChange = { proxyUsername = it },
                        label = "Proxy username",
                        singleLine = true
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = proxyPassword,
                        onValueChange = { proxyPassword = it },
                        label = { Text("Proxy password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(13.dp),
                        singleLine = true
                    )

                    Spacer(Modifier.height(10.dp))

                    AppTextField(
                        value = timeout,
                        onValueChange = { timeout = it },
                        label = "Timeout (seconds, 5–120)",
                        singleLine = true
                    )

                    Text(
                        "Leave the proxy host empty to connect directly.",
                        color = Muted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                // SEND BUTTON
                Button(
                    onClick = {
                        if (loading) return@Button

                        loading = true
                        errorMessage = ""
                        status = ""
                        responseHeaders = ""
                        responseBody = ""
                        elapsed = ""

                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    executeRequest(
                                        url = url,
                                        method = actualMethod,
                                        headersText = headers,
                                        bodyText = body,
                                        proxyHost = proxyHost,
                                        proxyPort = proxyPort,
                                        proxyUsername = proxyUsername,
                                        proxyPassword = proxyPassword,
                                        timeoutText = timeout
                                    )
                                }
                            }

                            result.onSuccess { response ->
                                status = "${response.code} ${response.message}"
                                responseHeaders = response.headers
                                responseBody = response.body
                                elapsed = "${response.duration} ms"
                            }.onFailure { error ->
                                errorMessage =
                                    error.message ?: "Unknown request error"
                            }

                            loading = false
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Purple,
                        disabledContainerColor = Color(0xFF51417C)
                    )
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("SENDING REQUEST...")
                    } else {
                        Text(
                            "➤  SEND REQUEST",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // RESPONSE
                SectionCard(
                    title = "05  Response",
                    subtitle = "Server response and request diagnostics"
                ) {
                    if (status.isNotEmpty()) {
                        Text(
                            status,
                            color = if (status.startsWith("2")) Green else Cyan,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Response time: $elapsed",
                            color = Muted,
                            fontSize = 12.sp
                        )

                        Spacer(Modifier.height(15.dp))

                        Text(
                            "RESPONSE HEADERS",
                            color = Cyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )

                        Spacer(Modifier.height(7.dp))

                        ResponsePanel(responseHeaders)

                        Spacer(Modifier.height(15.dp))

                        Text(
                            "RESPONSE BODY",
                            color = Cyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )

                        Spacer(Modifier.height(7.dp))

                        ResponsePanel(responseBody)
                    } else if (errorMessage.isNotEmpty()) {
                        Text(
                            "REQUEST FAILED",
                            color = Color(0xFFFF7373),
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        ResponsePanel(errorMessage)
                    } else {
                        Text(
                            "Your response will appear here after sending a request.",
                            color = Muted,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(Modifier.height(25.dp))

                Text(
                    "API REQUEST STUDIO  •  VERSION 2.0",
                    color = Muted,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardColor
        )
    ) {
        Column(
            modifier = Modifier.padding(17.dp)
        ) {
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = White
            )

            Spacer(Modifier.height(4.dp))

            Text(
                subtitle,
                fontSize = 11.sp,
                color = Muted
            )

            Spacer(Modifier.height(16.dp))

            content()
        }
    }
}

@Composable
private fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = false,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(13.dp)
    )
}

@Composable
private fun ResponsePanel(value: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFF10172D),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            text = value.ifEmpty { "(empty)" },
            color = Color(0xFFD1D9F0),
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}

private fun executeRequest(
    url: String,
    method: String,
    headersText: String,
    bodyText: String,
    proxyHost: String,
    proxyPort: String,
    proxyUsername: String,
    proxyPassword: String,
    timeoutText: String
): RequestResult {

    require(url.isNotBlank()) {
        "Please enter a request URL."
    }

    require(url.trim().startsWith("https://", ignoreCase = true)) {
        "Only HTTPS URLs are supported."
    }

    require(method.matches(Regex("[A-Z][A-Z0-9_-]*"))) {
        "Invalid HTTP method."
    }

    val timeoutSeconds = timeoutText
