# edhoc-fuzzer

## Contents

* [Description](#description)
* [Prerequisites](#prerequisites)
* [Initial Setup](#initial-setup)
* [How to Learn](#how-to-learn)
* [How to Test](#how-to-test)
* [How to Visualize](#how-to-visualize)
* [File Structure](#file-structure)

--------

## Description

edhoc-fuzzer is a Java tool that performs protocol state fuzzing of EDHOC clients and servers.
It is derived from the [dtls-fuzzer tool](https://github.com/assist-project/dtls-fuzzer).
The following functionality is supported:

1. A state machine model of an EDHOC client/server implementation can be learned
2. Tests (sequences of inputs) can be executed on an EDHOC client/server implementation

## Prerequisites

* java 17 JDK
* maven correctly setup to point to java 17 JDK
* graphviz library, containing the dot utility, which should be located in the systems PATH
* (suggested) python >=3.6 and installed pydot package, in order to use ./scripts/beautify_model
* (suggested) make utility, required by the setup of some suls
* (optional) openssl utility, required by ./scripts/gen_auth_hierarchy

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
The fetched source files are deleted after the installation. After the first installation of the library, the script can
be used without the `-l` flag, in order to rebuild the project. After a successful build, the softlink `edhoc_fuzzer.jar`
should have been created in the root directory.

3. Set up a System Under Learning (SUL)
```bash
./scripts/setup_sul 
```
This will show the usage message of the script, in order to provide a sul name as argument.
This process will fetch, patch and build the corresponding remote project and the following directories should be created:  
* **experiments/models/sources**, containing the remote project and the executables 
* **experiments/models/clients**, containing a directory structure with softlinks to the client executables
* **experiments/models/servers**, containing a directory structure with softlinks to the server executables


## How to Learn
After having set up the edhoc-fuzzer and the corresponding sul an argument file inside the **./experiments/args/**
subdirectories can be used or a similar one can be created. Command-line arguments can be also provided, in order to
overwrite those in the argument file. Notice the use of `@` before the argument file. The simplest high-level command is:
```bash
java -jar edhoc-fuzzer.jar @path/to/argfile
```


## How to Test
After having set up the edhoc-fuzzer and the corresponding sul an argument file inside the **./experiments/args/**
subdirectories can be used or a similar one can be created. The same applies to the test sequences inside the
**./experiments/tests/** subdirectories. Testing can be used prior to learning, in order to check that everything
runs as expected. The test command is:
```
java -jar edhoc-fuzzer.jar @path/to/arg/file -test path/to/test/file [-additional_param]

Additional Testing Parameters:

-times N
  Run each test sequence N number of times, defaults to 1

-testSpecification path/to/dot/model
  If a .dot model is provided as a specification, the resulting outputs are 
  compared against it. The test file will be run both on the implementation
  and on the specification model
  
-showTransitionSequence
  Shows the sequence of transitions at the end in a nicer format
```


## How to Visualize
The edhoc-fuzzer, after the learning process generated the **learnedModel.dot** file, tries to create the 
**learnedModel.pdf** file. In case the conversion to .pdf fails, the following command can be used:
```bash
dot -Tpdf path/to/in_model.dot > path/to/out_model.pdf
```

After a .dot model has been generated, it can be visually enhanced, in the form of merging same transitions and
replacing each label with a shorter one, using the following wrapper script:
```bash
./scripts/beautify_model
```
The label replacements can be found in the **./scripts/replacements.txt**. The optional arguments shown in the usage
message can be used when the model provided corresponds to a client implementation. This way the initial message, which a
client sends to start the EDHOC protocol, is added to the model. 

The above script is just a convenient wrapper of the following more customizable script:
```bash
python ./scripts/beautify_model.py -h
```


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
port and used when the sul accepts a port number as argument


* **default_mapper_connection.config** is the CoAP properties file containing the default values used by
the fetched and installed project used as library during the initial setup
