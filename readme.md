# The Java Machine Learning Library. 

This document covers the very basic documentation of the library. 

The Java Machine Learning Library is licensed under GNU-GPL.

More elaborate documentation can be found on the [web site](http://java-ml.sourceforge.net/)

## Overview

Java-ML in a nutshell:

 * A collection of machine learning algorithms
 * Common interface for each type of algorithms
 * Library aimed at software engineers and programmers, so no GUI, but clear interfaces
 * Reference implementations for algorithms described in the scientific literature.
 * Well documented source code.
 * Plenty of code samples and tutorials.

## How to get started

When you are reading this, you most probably already downloaded the library. 
To use it, include the `javaml-<version>.jar` in your classpath, as well as the 
jars that are available in `lib/`.  

See bellow how to get started with Maven.

How to get started, code samples, tutorials on various tasks can be found
at [http://java-ml.sourceforge.net](http://java-ml.sourceforge.net)

## Requirements

Java 6

## Dependencies

**Required libraries:**
 - [Apache Commons Math](http://commons.apache.org/math/): used in some algorithms, version 1.2
 - [Abeel Java Toolkit](http://sourceforge.net/projects/ajt/): used in some classes, version 2.11 is included. AJT is distributed under GNU LGPL 2 or later
 - [Jama](http://math.nist.gov/javanumerics/jama/): used in some algorithms, version 1.0.3 
	
**Optional libraries:**
 - [Weka](http://www.cs.waikato.ac.nz/ml/weka/): if you like to use algorithms from Weka, version 3.6.0	
 - [libsvm](http://www.csie.ntu.edu.tw/~cjlin/libsvm/): if you like to use the libsvm algoriths, version 3.17

## Build Maven project

 1. clone repository
 2. install ajt dependency: `mvn validate`
 3. build project and run unit test: `mvn clean install`

## Contact

You can contact us by using the [Sourceforge contact page](http://sourceforge.net/users/thomasabeel/) or send an email to me
thomas@abeel.be


