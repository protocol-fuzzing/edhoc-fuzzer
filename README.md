# edhoc-fuzzer

## Contents

* [Description](#description)
* [Prerequisites](#prerequisites)
* [Initial Setup](#initial-setup)
* [How to Test](#how-to-test)
* [How to Learn](#how-to-learn)
* [File Structure](#file-structure)

--------

## Description

edhoc-fuzzer is a Java tool that performs protocol state fuzzing of EDHOC servers and clients. 
The following functionality is supported:

1. A state machine model of an EDHOC client/server implementation can be learned
2. Tests (sequences of inputs) can be executed on an EDHOC client/server implementation

## Prerequisites

* java 17 JDK
* maven correctly setup to point to java 17 JDK
* graphviz library, containing the dot utility, which should be located in the systems PATH
* (suggested) python >=3.6 and installed pydot package, in order to use the *beautify_model* script
* (suggested) make utility, required by the setup of some suls
* (optional) openssl utility, required by the ./scripts/gen_auth_hierarchy

## Initial Setup

Assuming the commands are executed from the root directory:

1. Check prerequisites
```bash
java --version
mvn --version
dot -V
``` 

2. Set up edhoc-fuzzer
```bash
./scripts/setup_fuzzer -l
```
The `-l` flag is used to fetch the remote project used as library, compile and install it in the local maven repository.
After the first installation of the library, omit the `-l` flag to skip the library step, rebuild the project and
create a softlink `edhoc_fuzzer.jar` in the root directory. The fetched source files are deleted after the installation

3. Set up an SUL
```bash
./scripts/setup_sul 
```
This will show the usage message of the script, in order to provide a sul name as argument.
This process will fetch, patch and build the corresponding remote project and the following directories should be created:  
* **experiments/models/sources**, containing the remote project and the executables 
* **experiments/models/clients**, containing a directory structure with softlinks to the client executables
* **experiments/models/servers**, containing a directory structure with softlinks to the server executables


## How to Test
After having set up the fuzzer and the corresponding sul we can use an argument file inside the **./experiments/args/** subdirectories, 
or create a similar one. The same applies to the test sequences inside the **./experiments/tests/** subdirectories.
Notice the use of `@` before the argument file. The simplest high-level test command is:
```bash
java -jar edhoc-fuzzer.jar @path/to/arg/file -test path/to/test/inputs/file
```

## How to Learn
After having set up the corresponding sul, the command is similar to the testing command, just omit the test options.
```bash
java -jar edhoc-fuzzer.jar @path/to/arg/file
```
This way the command-line options are provided through the argument file.


## File Structure

The most important directories are **scripts**, **experiments** and **src/main/resources**

### Inside ./scripts/ directory

* **setup_fuzzer**, used for installing the remote library and (re-)building the project


* **setup_sul**, used for fetching, patching and building each remote sul (system under learning)


* **beautify_model**, a wrapper for the **beautify_model.py** used for visually enhancing the resulting .dot files from 
the learning process utilizing the **replacements.txt** 


* **gen_auth_hierarchy**, script used optionally for generating dummy authentication files in .pem, .der format in triples of 
<private key, public key, x509 certificates>; corresponding to one for each EDHOC authentication method. 
The files in the .der format can be provided as input to the edhoc-fuzzer. The resulting directories are: 
  * **./experiments/authentication/mapper**
    * **/sig**, containing an ed25519 triple and a p256 triple
    * **/stat**, containing a p256 triple and a x25519 triple
  * **./experiments/authentication/sul**, same as the mapper directory with different keys


### Inside ./experiments/ directory

* **args/** contains the argument files for each sul provided to the edhoc-fuzzer


* **authentication/** initially contains the **test_vectors** directory with files in the test-vector json format.
The subdirectories *mapper/*, *sul/* can be generated using the *./scripts/gen_auth_hierarchy*


* **models/** initially contains the custom patches used in the setup of the suls. The sub-directories 
*sources/*, *clients/*, *servers/* can be generated using the *./scripts/setup_sul*


* **results/** is the output directory designated by the *./experiments/args* files containing the resulting 
subdirectories during a learning process. Initially it does not exist


* **tests/** contains a set of input test sequences used as input to the edhoc-fuzzer for testing them against
a client/server implementation   


### Inside ./src/main/resources/ directory

* **default_alphabet.xml** is the default alphabet in .xml format provided to the edhoc-fuzzer, which contains all the
currently supported message inputs and is used implicitly by the argument files in *./experiments/args* 


* **default_fuzzer.properties** contains the symbolic properties that are used in the argument files in the 
*./experiments/args* (can be used also in command line arguments) and are in the format `${property_name}`.
These are substituted in runtime. One implicit property name is `sul.port`, which is replaced with a randomly generated 
available port and used when the sul accepts a port number as argument.   


* **default_mapper_connection.config** is the CoAP Properties file containing the default values used by
the fetched and installed project used as library during the initial setup
