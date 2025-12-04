import com.google.protobuf.gradle.id

plugins {
    alias(libs.plugins.protobuf)
}

dependencies {
    api(project(":common:common-core"))
    api(libs.grpc.protobuf)
    api(libs.grpc.stub)
    api(libs.grpc.kotlin.stub)
    api(libs.protobuf.kotlin)
    api(libs.kotlinx.coroutines.reactor)
}

protobuf {
    protoc { artifact = libs.protoc.get().toString() }
    plugins {
        id("grpc") {
            artifact =
                libs.grpc.protoc.gen.java
                    .get()
                    .toString()
        }
        id("grpckt") { artifact = "${libs.grpc.protoc.gen.kotlin.get()}:jdk8@jar" }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("grpc")
                id("grpckt")
            }
            task.builtins {
                id("kotlin")
            }
        }
    }
}

ktlint {
    filter {
        exclude { element -> element.file.path.contains("generated/") }
    }
}
