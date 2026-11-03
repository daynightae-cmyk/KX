# Assets and dependency review

The owner supplied the immutable PNG design references. Their redistribution rights have not been independently verified. They remain design inputs; no reference PNG is packaged as an application screen. The native vector/canvas artwork is a derivative implementation candidate.

Dependency versions are recorded in `gradle/libs.versions.toml`. No tunnel library, provider SDK, telemetry SDK, network client or proprietary font is included in this launch-only stage. Gradle-generated wrapper scripts/JAR retain their upstream notices.

Full resolved dependency license inventory, security-advisory review, asset-rights confirmation, signing and store-policy review remain release gates. This development branch makes no licensing-audit, security-audit or production-readiness claim.
