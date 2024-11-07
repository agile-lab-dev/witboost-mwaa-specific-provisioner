<p align="center">
    <a href="https://www.witboost.com/">
        <img src="docs/img/witboost_logo.svg" alt="witboost" width=600 >
    </a>
</p>

Designed by [Agile Lab](https://www.agilelab.it/), witboost is a versatile platform that addresses a wide range of sophisticated data engineering challenges. It enables businesses to discover, enhance, and productize their data, fostering the creation of automated data platforms that adhere to the highest standards of data governance. Want to know more about witboost? Check it out [here](https://www.witboost.com/) or [contact us!](https://witboost.com/contact-us).

This repository is part of our [Starter Kit](https://github.com/agile-lab-dev/witboost-starter-kit) meant to showcase witboost's integration capabilities and provide a "batteries-included" product.

# MWAA Specific Provisioner

- [Overview](#overview)
- [Building](#building)
- [Running](#running)
- [Configuring](#configuring)
- [Deploying](#deploying)
- [HLD](docs/HLD.md)
- [API specification](docs/API.md)

## Overview

This project implements a simple Specific Provisioner for Amazon Managed Workflows for Apache Airflow. After deploying this microservice and configuring witboost to use it, the platform can deploy components that use Airflow to orchestrate other components in a Data Product.

### What's a Specific Provisioner?

A Specific Provisioner is a microservice which is in charge of deploying components that use a specific technology. When the deployment of a Data Product is triggered, the platform generates it descriptor and orchestrates the deployment of every component contained in the Data Product. For every such component the platform knows which Specific Provisioner is responsible for its deployment, and can thus send a provisioning request with the descriptor to it so that the Specific Provisioner can perform whatever operation is required to fulfill this request and report back the outcome to the platform.

You can learn more about how the Specific Provisioners fit in the broader picture [here](https://docs.witboost.agilelab.it/docs/p2_arch/p1_intro/#deploy-flow).

### MWAA

Amazon Managed Workflows for Apache Airflow is an AWS service that allows users to orchestrate their workflows using Airflow Directed Acyclic Graphs (DAGs) written in Python without having to manage the Airflow infrastructure themselves. You can find more information on it [here](https://aws.amazon.com/managed-workflows-for-apache-airflow/).

### Software stack

This microservice is written in Scala 2.13, using Akka HTTP (pre-license change) for the HTTP layer. Communication with AWS services is handled with AWS Java SDK. Project is built with SBT and supports packaging as JAR, fat-JAR and Docker image, ideal for Kubernetes deployments (which is the preferred option).

## Building

**Requirements:**

- Java 17 (11 works as well)
- SBT
- Node
- Docker (for building images only)

**Generating sources:** this project uses OpenAPI as standard API specification and the [OpenAPI Generator](https://openapi-generator.tech) CLI to generate server code from the specification.

In a terminal, install the OpenAPI Generator CLI and run the `generateCode` SBT task:

```bash
npm install @openapitools/openapi-generator-cli -g
sbt generateCode
```

*Note:* the `generateCode` SBT task needs to be run again if `clean` or similar tasks are executed.

**Compiling:** is handled by the standard task:

```bash
sbt compile
```

**Tests:** are handled by the standard task as well:

```bash
sbt test
```

**Artifacts & Docker image:** the project uses SBT Native Packager for packaging. Build artifacts with:

```
sbt package
```

The Docker image can be built with:

```
sbt docker:publishLocal
```

*Note:* the version for the project is automatically computed using information gathered from Git, using branch name and tags. Unless you are on a release branch `1.2.x` or a tag `v1.2.3` it will end up being `0.0.0`. You can follow this branch/tag convention or update the version computation to match your preferred strategy.

**CI/CD:** the pipeline is based on GitLab CI as that's what we use internally. It's configured by the `.gitlab-ci.yaml` file in the root of the repository. You can use that as a starting point for your customizations.

## Running

To run the server locally, use:

```bash
sbt generateCode compile run
```

By default, the server binds to port 8093 on localhost. After it's up and running you can make provisioning requests to this address. You can also check the API documentation served [here](http://127.0.0.1:8093/datamesh.mwaaspecificprovisioner/0.0/swagger/docs/index.html).

### Run Airflow locally

During development, it can be useful to have a local instance of Airflow available for testing. The project includes a Docker Compose file to do this; in order to launch it:
 
```bash
cd docker                      # to move to the docker folder
docker-compose up airflow-init # to build the database
docker-compose up              # to start airflow
```

Airflow will bind to port 8080 on localhost; you can open the UI [here](http://127.0.0.1:8080).

Make sure to edit the Docker Compose file to match the Airflow version to the same one used by your MWAA environment of interest. If you feel like you need to really mimic the MWAA environment as closely as possible (eg, you are having conflicts among the Python dependencies needed to run your DAGs) you might want to look into [aws-mwaa-local-runner](https://github.com/aws/aws-mwaa-local-runner).

## Configuring

Most application configurations are handled with the Typesafe Config library. You can find the default settings in the `reference.conf` and some `application.conf` examples in the Helm chart (see below). Customize them and use the `config.file` system property or the other options provided by Typesafe Config according to your needs.

S3 client configuration is based on standard AWS SDK configuration approach, meaning it honors the settings in the AWS config file (either at the default path, or at the one specified by `AWS_CONFIG_FILE`).

This means that if you need to set up authentication to S3 in a way that is not the default approach taken by the AWS SDK, you should update the config file by either including your custom one in the Docker image or mount a proper one in the container at runtime. Some settings can also be changed using environment variables (eg, AWS Access Key settings), again following standard AWS SDK behavior.

Logging is handled with Logback, you can find an example `logback.xml` in the Helm chart. Customize it and pass it using the `logback.configurationFile` system property.

## Deploying

This microservice is meant to be deployed to a Kubernetes cluster with the included Helm chart and the scripts that can be found in the `helm` subdirectory.

## License

This project is available under the [Apache License, Version 2.0](https://opensource.org/licenses/Apache-2.0); see [LICENSE](LICENSE) for full details.

## About Witboost

[Witboost](https://witboost.com/) is a cutting-edge Data Experience platform, that streamlines complex data projects across various platforms, enabling seamless data production and consumption. This unified approach empowers you to fully utilize your data without platform-specific hurdles, fostering smoother collaboration across teams.

It seamlessly blends business-relevant information, data governance processes, and IT delivery, ensuring technically sound data projects aligned with strategic objectives. Witboost facilitates data-driven decision-making while maintaining data security, ethics, and regulatory compliance.

Moreover, Witboost maximizes data potential through automation, freeing resources for strategic initiatives. Apply your data for growth, innovation and competitive advantage.

[Contact us](https://witboost.com/contact-us) or follow us on:

- [LinkedIn](https://www.linkedin.com/showcase/witboost/)
- [YouTube](https://www.youtube.com/@witboost-platform)


