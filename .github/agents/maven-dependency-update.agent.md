---
name: maven-dependency-update
description: Check for and update Maven dependencies and plugins to newer versions using versions-maven-plugin. Use when asked to update dependencies, check for outdated packages, or upgrade Maven project dependencies.
---

# Maven Dependency Update

Use the Maven versions plugin to look for dependency and plugin version updates.

Run `mvn versions:display-dependency-updates` to check for dependency version updates.

Run `mvn versions:display-plugin-updates` to check for plugin version updates.

Summarize and present to the user. Ask the user if they want to apply the updates.

If they want to apply updates, run `mvn dependency:resolve`. Fix any errors. After all the dependencies have been resolved, run `mvn clean verify`. If there are any errors, investigate and make suggestions, but do not modify any code.
