package com.example.customviews.utils.ext

inline fun <T1: Any, T2: Any, R> letIfNotNull(a: T1?, b: T2?, block: (T1, T2) -> R) {
    if (a != null && b != null) block(a, b)
}

inline fun <T1: Any, T2: Any, T3: Any, R> letIfNotNull(a: T1?, b: T2?, c: T3?, block: (T1, T2, T3) -> R) {
    if (a != null && b != null && c != null) block(a, b, c)
}
