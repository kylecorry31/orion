# Orion

`orion` is a Kotlin library of custom detekt rulesets.

## Usage

Available on [JitPack](https://jitpack.io/#kylecorry31/orion). Add it to your Detekt plugin configuration:

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    detektPlugins("com.github.kylecorry31:orion:<version>")
}
```

Enable the rule in your `detekt.yml`:

```yaml
orion:
  NoRecursion:
    active: true
  WhyComment:
    active: true
```
