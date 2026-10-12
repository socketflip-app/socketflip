plugins {
    id("com.android.application") version "9.4.1" apply false
    // AGP 9 compiles Kotlin itself (built-in Kotlin); this only pins the Kotlin version it uses.
    id("org.jetbrains.kotlin.android") version "2.4.20" apply false
}
