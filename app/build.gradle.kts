plugins {
    id("com.android.application")
}

android {
    namespace = "com.nulllab.ble.coding.central"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nulllab.ble.coding.central"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    applicationVariants.all {
        outputs.all {
            when (this) {
                is com.android.build.gradle.internal.api.ApkVariantOutputImpl -> {
                    if (name.endsWith("debug")) {
                        outputFileName =
                            "ble_coding_central-debug-v" + defaultConfig.versionName + ".apk"
                    }
                    if (name.endsWith("release")) {
                        outputFileName =
                            "ble_coding_central-release-v" + defaultConfig.versionName + ".apk"
                    }
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

tasks.register("checkExampleCodeAssets") {
    doFirst {
        val requiredAssets = listOf(
            "example_code/dc_motor/main.py",
            "example_code/read_digital/main.py",
            "example_code/read_digital/main.py",
            "example_code/write_digital/main.py",
            "example_code/read_adc/main.py",
            "example_code/read_digital_and_adc/main.py",
            "example_code/stdio/main.py",
            "example_code/joystick/main.py",
            "example_code/dht11/main.py",
            "example_code/passive_buzzer/main.py",
            "example_code/ir_remote_control_receiver/main.py",
            "example_code/dc_motor/main.py",
            "example_code/geek_servo_270/main.py",
            "example_code/servo/main.py",
            "example_code/servo360/main.py",
            "example_code/ws2812/main.py",
            "example_code/ds18b20/main.py",
            "example_code/ultrasonic_one_wire/main.py",
            "example_code/rgb_led/main.py",
            "example_code/tm1650_four_digit_led/main.py",
            "example_code/x16k33_matrix_led_5x5/main.py",
            "example_code/ssd1306_i2c_128x64/main.py",
            "example_code/qma6100p/measure_gesture/main.py",
            "example_code/qma6100p/read_acceleration/main.py",
            "example_code/traffic_lights/main.py",
            "example_code/adc_keyboard/main.py",
            )
        val missingAssets = requiredAssets.filter { asset ->
            !File("app/src/main/assets/$asset").exists()
        }

        if (missingAssets.isNotEmpty()) {
            throw GradleException("Required assets not found: ${missingAssets.joinToString()}")
        }
    }
}

tasks.getByName("preBuild") {
    dependsOn("checkExampleCodeAssets")
}