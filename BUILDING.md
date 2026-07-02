# Building SparkTraits

## 中文

请从 SparkTraits 的源码根目录构建。这个目录里应该能看到：

```text
build.gradle
settings.gradle
gradle.properties
gradlew
gradlew.bat
src/
wathe-*.jar
noellesroles-*.jar
```

Windows:

```bat
gradlew.bat clean build
```

macOS / Linux:

```sh
./gradlew clean build
```

项目可以放在任意父级路径下，源码根目录本身也可以改名；但不要从缺少这些文件的外层目录直接运行 `gradle build`。源码包需要同时保留根目录下的 `wathe-*.jar` 和 `noellesroles-*.jar`，否则 Gradle 能启动但编译依赖会缺失。

## English

Build from the SparkTraits source root. Use the checked-in Gradle Wrapper:

```sh
./gradlew clean build
```

On Windows:

```bat
gradlew.bat clean build
```

The project may live under any parent path, and the source root folder may be renamed. Keep the root-level `wathe-*.jar` and `noellesroles-*.jar` files in source packages because they are local compile dependencies.
