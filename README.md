# Datawire sentinel"

Microservice to watch GitHub source repositories and trigger the build-release process.

# User Testing Instructions

A Docker container can be started by executing `make docker`

# Developer Instructions

Developers will likely be using a familiar Java IDE to interact with the codebase rather than relying on the Docker image.

## IntelliJ IDEA

Add a new `Application` Run/Debug Configuration `Run > Edit Configurations`. Then click the plus symbol to create a new Application configuration.

```text
Main-Class: io.vertx.core.Launcher
VM Options:
    -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory
    -Dvertx.hazelcast.config=config/cluster.xml
    -Dapp.env=develop
Program Arguments: run io.datawire.sentinel.ServiceVerticle -conf config/sentinel-develop.json -cluster
Working Directory: $MODULE_DIR$
Use Classpath of Module: sentinel-web_main
```

# License

Datawire Sentinel is open-source software licensed under **Apache 2.0**. Please see [LICENSE](LICENSE) for further details.
