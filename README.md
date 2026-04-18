# Orion

`orion` is a Kotlin library of custom detekt rulesets.

## Usage

Add the published artifact to your Detekt plugin configuration:

```kotlin
dependencies {
    detektPlugins("com.kylecorry:orion:<version>")
}
```

Enable the rule in your `detekt.yml`:

```yaml
orion:
  NoRecursion:
    active: true
```
