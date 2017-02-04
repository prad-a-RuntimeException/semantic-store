Build status: <a href="https://circleci.com/gh/prad-a-RuntimeException/semantic-store"><img src="https://circleci.com/gh/prad-a-RuntimeException/semantic-store.svg?style=svg"></a>

## Synopsis

Provides an insight in to the world of Recipes. This is a reference implementation for 
leveraging Semantic technology for data gathering, integration, modelling, storage, leveraging Tinkerpop graph library and Tinkerpop compatible graph database. 


## Motivation

The motivation is multi-fold:

- To provide a reference implementation for leveraging the latest/greatest in semantic
technology.

- Demonstrating NLP engines to map raw data to Ontological concepts.

- Perform semi-automatic transformation from Triples  to a property based graph infrastructure.

- Leveraging Graph Computing for performing advanced analytics.

- Demonstrate learning model based on Ontology. 

## Tools

- Apache Jena version 3.

- Jena TDB as FileBasedTripleStore

- Stardog as a alternative TripleStore

- Spark with GraphFrames support. 

- TinkerPop3 support for property graph

- DSE Graph as the data layer for the TinkerPop graph

## Installation

./gradlew clean build

## API Reference

## Tests

Contains both Java and ScalaModules and uses Junit and ScalaTest for running
tests. 

./gradlew test

## Contributors



