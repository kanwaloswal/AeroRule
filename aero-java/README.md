# AeroRule Java Core (`aero-core`)

The Java implementation of the AeroRule engine. Designed for high-performance, concurrent rule evaluation using Google CEL.

## Build and Install

```bash
mvn clean install
```

## Usage

Include the dependency in your application, then use the `RuleEvaluator` or `RuleSetEngine`:

```java
import com.aerorule.core.engine.*;

// Load multiple rules orchestrated by a RuleSet
RuleSetEngine engine = RuleSetEngine.fromFile("path/to/ruleset.json");

// Evaluate
RuleSetTrace trace = engine.evaluate(Map.of(
    "transaction", Map.of("amount", 2000)
));

System.out.println("Result: " + trace.isPassed());
```
