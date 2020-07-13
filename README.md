# Swagger API changelog

[![CI Build](https://github.com/sonalake/swagger-changelog-gradle-plugin/workflows/CI%20Build/badge.svg)](https://github.com/sonalake/swagger-changelog-gradle-plugin)
[![codecov](https://codecov.io/gh/sonalake/swagger-changelog-gradle-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/sonalake/swagger-changelog-gradle-plugin)

A gradle pluging for generating an asciidoc docset for a
changelog across different versions of a swagger API.

Assumes that:

- There is a history of swagger API json documents on nexus
- The versions can be sorted by their version id
- It is only the *complete* versions that are to be considered (e.g only `1.4.1` and not any `1.4.1-RC1`)

However, you _can_ include a local file to act as the latest snapshot, that will 
be considered as the last version in the chain.


## What is generated?

The following files are created in the target directory:

 - `change-log.adoc` - an index doc that includes _all_ of the underlying generated documents.
 - `change-log-${oldVersion}-${newVersion}.adoc` - a specific file for each version step.
 
 Each _version-to-version_ file comes in this format:
 
 - A list of the new / deprecated endpoints
 - A list of all the endpoints with changes in _any_ of the parameters, return types. 
 
This includes both completely new parameters/properties, and also changes to existing entities.
 
For example:  

---

```asciidoc
 
=== Version 1.2.2 to 1.3.0

==== New Endpoints

* `POST` /admin/config/flushcache Manually flush the cache

==== Deprecated Endpoints

==== Changed Endpoints

`POST` /orders Creates new order +
 Parameters

----
    Insert orderRequest.hardwareDetails //Details on the hardware within the site
    Insert orderRequest.sourceSiteDetails //Details on the source site
    Insert orderRequest.targetSiteDetails //Details on the source site
    Delete orderRequest.endCustomerSiteDetails //Details on the hardware within the site
    Delete orderRequest.sourceCircuitDetails //Details on the source site
    Delete orderRequest.targetCircuitDetails //Details on the source site
----

```
---

## Gradle usage

> **NOTE:** depends on at least gradle `4.10`

### Task: generateChangeLog

The task to call that will generate the above changelog files is `generateChangeLog`


To use the plugin, you need to add this to your `build.gradle`

Using the plugins DSL:
```groovy
plugins {
  id "com.sonalake.swagger-changelog" version "1.0.0"
}
```

Or, using legacy plugin application:
```groovy
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "gradle.plugin.com.sonalake:swagger-changelog-gradle-plugin:1.0.0"
  }
}

apply plugin: "com.sonalake.swagger-changelog"
```


The configuration for this plugin is
```groovy
swaggerChangeLog {
   
    // this is assumed to be in the json format
    groupId = 'com.sonalake'
    artifactId = 'esqt-server-API'
    
    // where to find the nexus repo
    nexusHome = 'http://atlanta.sonalake.corp:8081/nexus'
    repositoryId = 'releases'
    
    // where to store the changelog
    targetdir = "${buildDir}/apidoc/swagger-changelog"
    
    // (optional) if your build has a "current snapshot 
    // version" then add it to the version history as 
    // the last version - it will be included no matter
    // what the version id is.
    snapshotVersionFile = "${buildDir}/swagger.json"
    
    
    // (optional) from what level should the chapters 
    // start at. By default, they will start at level 3, 
    // i.e. "==="
    baseChapterLevel = 3    
}

```
