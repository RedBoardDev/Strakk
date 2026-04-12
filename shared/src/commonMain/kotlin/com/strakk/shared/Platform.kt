package com.strakk.shared

interface PlatformInfo {
    val name: String
}

expect val Platform: PlatformInfo
