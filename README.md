# Kie Gradle Plugin
A Gradle plugin for building Kie JARs.

## Defining A Kie JAR Project
Using the plugins DSL:

```gradle
plugins {
  id 'io.github.kevin-wimmer.kjar' version '7.73.0.Final'
}
```

Using legacy plugin application:

```gradle
buildscript {
  repositories {
    maven {
      url 'https://plugins.gradle.org/m2/'
    }
  }
  dependencies {
    classpath 'io.github.kevin-wimmer:kie-gradle-plugin:7.73.0.Final'
  }
}

apply plugin: 'io.github.kevin-wimmer.kjar'
```

## Restrictions
Because the Drools compiler, itself, assumes that projects are using the Maven
[Standard Directory Layout](https://maven.apache.org/guides/introduction/introduction-to-the-standard-directory-layout.html),
projects using this plugin are bound by the same constraints. As such, Drools source files must
reside under the `src/main/resources` directory.

## Drools Compatibility
This plugin is versioned in such a manner as to match the version of Drools with which it is known
to be compatible (i.e. version 7.73.0.Final is compatible with Drools 7.73.0.Final). And while a
given version of this plugin may work with other Drools releases, such compatibility is not guaranteed.

## License
Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)

## Copyright
Copyright (c) 2023 Kevin Wimmer
