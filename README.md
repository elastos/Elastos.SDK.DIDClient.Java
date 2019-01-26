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
    <artifactId>did_client.lib</artifactId>
    <version>0.0.1</version>
</dependency>
```

## Run

This lib must communicate to did node.
So first you should configure those API to node with the configuration file ./conf/ela.did.properties beside your application directory. Change the "node.prefix" to your did node URL.

## DID Service API

Please see it in https://did-service-api.readthedocs.io/en/refactor_frontend/
