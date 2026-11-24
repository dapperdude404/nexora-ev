# Read-only version handshake

Reference: user-provided ola-s1-mods-1.5.zip, copyright 2026 Anand (@Asjohn720) and Contributors; archive LICENSE begins MIT. Archive Android versionName is 1.2 despite archive name 1.5; public repository licensing/version may differ. NEXORA independently implements the observed GetSwVersion wire contract only, not privileged commands or bundled applications. Upstream: https://github.com/Asjohn720/ola-s1-mods.

Parked testing: join normal hotspot; enter scooter's link-local IPv6 address using the phone's interface scope; request software version. Expect either a nonempty version response or explicit failure within RPC deadline. This local plaintext protocol does not authenticate peer identity; do not treat it as live telemetry. No shell RPC, security modifications, or remote installs. Scoped-address resolution on Android must be tested. Battery data unavailable until a dedicated authorized read-only interface is verified.

50 MB budget enforced in CI; actual build and device compatibility unverified.
