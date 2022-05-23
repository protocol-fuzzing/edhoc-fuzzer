# edhoc-fuzzer

## Install provided cf-edhoc.jar to local maven repository
```bash
mvn install:install-file \
    -Dfile=./src/main/resources/cf-edhoc.jar \
    -DgroupId=se.ri.org.eclipse.californium \
    -DartifactId=cf-edhoc \
    -Dversion=0.0.0 \
    -Dpackaging=jar \
    -DgeneratePom=true
```
