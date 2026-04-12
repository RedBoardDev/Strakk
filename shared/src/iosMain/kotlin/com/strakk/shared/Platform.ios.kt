package com.strakk.shared

import platform.UIKit.UIDevice

actual val Platform: PlatformInfo = object : PlatformInfo {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}
