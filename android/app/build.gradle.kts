plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android { namespace="com.cluecompanion"; compileSdk=35
 defaultConfig {
  applicationId="com.cluecompanion"; minSdk=26; targetSdk=35; versionCode=1; versionName="1.0"
  buildConfigField("String", "BASE_URL", "\"https://jubilant-doodle-4gjj5qx9pj7h7rwp-8080.app.github.dev/\"")
 }
 buildFeatures { compose=true; buildConfig=true }
 composeOptions { kotlinCompilerExtensionVersion="1.5.14" }
 compileOptions {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies {
 implementation("androidx.core:core-ktx:1.13.1")
 implementation("androidx.activity:activity-compose:1.9.1")
 implementation(platform("androidx.compose:compose-bom:2024.06.00"))
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.ui:ui-tooling-preview")
 debugImplementation("androidx.compose.ui:ui-tooling")
}
