package com.strakk.shared

actual val Platform: PlatformInfo = object : PlatformInfo {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
}
