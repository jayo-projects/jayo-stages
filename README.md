[![License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?logo=apache&style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0)
[![Version](https://img.shields.io/maven-central/v/dev.jayo/jayo-stages?logo=apache-maven&color=&style=flat-square)](https://search.maven.org/artifact/dev.jayo/jayo-stages)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white&style=flat-square)](https://www.java.com/en/download/help/whatis_java.html)

# Jayo stages

* `Promesse` is a cancellable CompletionStage implementation inspired by the
[CompletionStage implementation by Lukáš Křečan](https://github.com/lukas-krecan/completion-stage).
* `StagesAsyncStream` is an AsyncStream that streams the results of several CompletionStages producing the same result
type.
* `Stages` offer a few util static methods for CompletionStage.

Jayo stages is available on Maven Central.

Gradle:
```groovy
dependencies {
    implementation("dev.jayo:jayo-stages:X.Y.Z")
}
```

Maven:
```xml

<dependency>
    <groupId>dev.jayo</groupId>
    <artifactId>jayo-stages</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

The Jayo stages code is written in Java without the use of any external dependencies, to be as light as possible.

Jayo stages requires Java 17 or more recent.

*Contributions are very welcome, simply clone this repo and submit a PR when your fix, new feature, or optimization is
ready!*

## The CompletableFuture paradox

The `CompletableFuture` class is the only implementation of the `CompletionStage` interface provided by the JVM. Unless
configured otherwise, by default a CompletableFuture uses the `ForkJoinPool.commonPool()` to schedule all its async
tasks, which is shared across all other CompletableFutures and Parallel Streams in your application.

The Fork/Join Pool uses a work-stealing algorithm: each thread has its own queue of tasks, and idle threads "steal"
tasks from busy threads' queues. This minimizes contention and maximizes CPU utilization.

**Implication:** This makes CompletableFutures efficient for divide-and-conquer tasks (e.g., recursive computations),
but less so for I/O-bound tasks.

`Promesse` fills this gap by providing a cancellable `CompletionStage` implementation, adapted to run I/O-bound async
tasks.

## License

[Apache-2.0](https://opensource.org/license/apache-2-0)

Copyright (c) 2026-present, pull-vert and Jayo contributors
