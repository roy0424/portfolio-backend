rootProject.name = "portfolio-ms"

// Common modules
include("common:common-core")
include("common:common-security")
include("common:common-event")
include("common:common-grpc")

// Infrastructure
include("infrastructure:gateway")

// Services
include("services:auth-service")
include("services:portfolio-service")
include("services:page-service")
include("services:asset-service")
include("services:event-processor")
