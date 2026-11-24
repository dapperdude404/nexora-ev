# Adaptive Lite Pro performance budget

Approved APK target: at most 50 MB; smaller is preferred when functionality is equivalent. Core active PSS target 40–80 MB; diagnostics target below 120 MB; cold startup target 1–2 seconds. These are unverified engineering targets, not measurements. Use lazy module initialization, bounded caches, lifecycle-aware resource cleanup and no idle polling. Benchmark on phone first and supported parked dashboard only. Preserve MoveOS stability; no system process killing or vehicle control.
