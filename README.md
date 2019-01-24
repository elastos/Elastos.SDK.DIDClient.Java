Elastos.SDK.DIDClient
==============

## Summary

This repo provide JAVA API to develop DID service application.

## Build with maven

In project directory, use maven command:
```shell
$uname mvn clean compile package install
```
If there is build success, Then the package will be in your local maven Repository.

## Add to project

With Maven：
```xml
<dependency>
    <groupId>org.elastos</groupId>
    <artifactId>did_service.lib</artifactId>
    <version>0.0.1</version>
</dependency>
```

## Run

Those API should run with its backend, first configure and start the [DID service backend](https://github.com/elastos/Elastos.ORG.DID.Service/tree/refactor_backend).

Then configure those API to backend with the configuration file ./conf/ela.did.properties beside your application directory. Change the "node.prefix" to your backend service URL.

## DID Service API

Please see it in https://did-service-api.readthedocs.io/en/refactor_frontend/
