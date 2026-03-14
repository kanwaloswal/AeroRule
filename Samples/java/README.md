# Java AeroRule Samples

This directory contains standalone Java sample classes that demonstrate how to use `aero-core` to evaluate JSON rules defined in `Samples/rules`. They represent financial service scenarios like Loan Origination and AML Analysis.

## Setup Instructions

Since `aero-core` is a local library not available on Maven Central, you first need to build and install it to your local Maven repository.

1. Navigate to the core library directory and install:
```bash
cd ../../aero-java
mvn clean install -DskipTests
```

2. Return to this `Samples/java` path once it succeeds.
```bash
cd ../Samples/java
```

## Running the Samples

You can run each sample main method using maven:

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.example.samples.LoanOriginationSample"
mvn exec:java -Dexec.mainClass="com.example.samples.AmlTransactionSample"
```
