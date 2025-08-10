@file:Suppress("DEPRECATION")

plugins {
    id("com.android.library") // IMPORTANTE: este é um módulo de biblioteca!
    id("org.jetbrains.kotlin.android") // Para classes Kotlin
}
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21)) // <--- ISSO PODERIA SER UM PROBLEMA
    }
}

android {
    namespace = "com.onzeAppsSdkManage.AdKen" // <--- VERIFIQUE ESTE NOME. DEVE BATER COM O SEU PACOTE!
    compileSdk = 36 // A API Level com a qual seu SDK será compilado

    defaultConfig {
        minSdk = 24
        // A API Level mínima que seu SDK suporta

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        multiDexEnabled = false
        signingConfig =
            signingConfigs.getByName("debug")// Para regras de ProGuard para consumidores do SDK
    }

    buildTypes {
        release {
            isMinifyEnabled = false // Pode ser 'true' para otimizar o tamanho do SDK final
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8 // Java 8 é comum para Android
        targetCompatibility = JavaVersion.VERSION_1_8
    }


    kotlinOptions {
    jvmTarget = "1.8" // Kotlin também está configurado para 1.8
}


dependencies {
    // Dependências mínimas para um SDK Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material) // Se seu SDK usa componentes Material Design
    implementation(libs.androidx.constraintlayout) // Se seu SDK usa ConstraintLayout

    // Dependências para comunicação de rede (OkHttp e Retrofit, se usado)
    // Se seu AdNetworkClient usa OkHttp diretamente:
    implementation(libs.okhttp)
    // Se seu AdNetworkClient usa Retrofit, adicione também o conversor para JSON:
    // implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // implementation("com.squareup.retrofit2:converter-gson:2.9.0") // Ou outro conversor

    // Dependências para Coroutines (se usar no seu SDK para operações assíncronas)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // Dependências para Google Play Services Ads Identifier (GAID)
    implementation(libs.play.services.ads.identifier)

    // Dependências para ExoPlayer (para vídeo ads)
    implementation(libs.exoplayer.core)
    implementation(libs.exoplayer.ui)

    // Dependência para carregar imagens de URLs (se seu SDK renderiza imagens de anúncios)
    implementation(libs.picasso) // Ou Glide, Coil, etc.
    // Retrofit e OkHttp para comunicação com a API
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)

    // Dependências de teste
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.v121)
    androidTestImplementation(libs.androidx.espresso.core)
}
}
dependencies {
    implementation(libs.androidx.ads.adservices)
}
