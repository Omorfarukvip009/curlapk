# API Request Sender (Android)

Native Kotlin app for authorized HTTPS POST requests with JSON bodies and optional
authenticated HTTP CONNECT proxy support.

## Build using GitHub Actions (no Windows or Android Studio needed)

1. Create a new GitHub repository.
2. Upload all files from this project, preserving their folder structure.
3. Open the **Actions** tab and enable workflows if prompted.
4. Run **Build Android APK**, or push to the `main` branch.
5. Open the completed workflow run and download `api-request-sender-debug-apk`.
6. Extract the artifact ZIP to get `app-debug.apk`.

## Proxy compatibility

The app uses an HTTP proxy with CONNECT tunneling for HTTPS destinations. This is
compatible with common `http://proxy-host:port` proxy endpoints.

It does not currently support TLS-wrapped proxy connections (`https://proxy-host:port`)
or SOCKS proxies. Confirm the proxy protocol with your provider.

## Safety

Use only APIs and phone numbers you own or are authorized to test. Do not use the app
to evade rate limits, access controls, or anti-abuse protections. Proxy credentials are
entered at runtime and are not saved by the app. The project contains no third-party
OTP endpoint or hard-coded credentials.
