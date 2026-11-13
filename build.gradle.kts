plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// AGP 9 خودش Kotlin را کامپایل می‌کند (Built-in Kotlin) و به‌صورت پیش‌فرض
// KGP داخل خودش را استفاده می‌کند. برای اینکه از جدیدترین کاتلین (طبق کاتالوگ)
// استفاده شود، آن را روی buildscript classpath می‌آوریم – بدون apply کردن پلاگین.
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    }
}
