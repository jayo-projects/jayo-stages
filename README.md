[![License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?logo=apache&style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0)
[![Version](https://img.shields.io/maven-central/v/dev.jayo/jayo-stages?logo=apache-maven&color=&style=flat-square)](https://search.maven.org/artifact/dev.jayo/jayo-stages)

# Jayo stages

* `Promesse` is a cancellable CompletionStage implementation inspired by the
[Lukáš Křečan CompletionStage implementation](https://github.com/lukas-krecan/completion-stage).
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

The `CompletableFuture` class is the only implementation of the CompletionStage in the JVM. This class is tightly
coupled with the fork-join feature that aims to handle CPU-intensive tasks. Unless configured otherwise, by default a
CompletableFuture uses the `ForkJoinPool.commonPool()` to execute all its async tasks.

But we often use want to asynchronously process IO tasks, `Promesse` simplifies that use-case by providing a minimal
cancelable CompletionStage implementation.

## License

[Apache-2.0](https://opensource.org/license/apache-2-0)

Copyright (c) 2026-present, pull-vert and Jayo contributors
