# opp-docker-orangehrm

**Note:** This docker script is based on **Dan Cinnamon**'s [opp-docker](https://github.com/dancinnamon-okta/opp-docker). Few modifications have been done, such as allowing HTTP and the OrangeHRM connector war file.


This repository contains an easy way to run Okta's on-premise provisioning agent within a docker container on your own machine.  Included are the following containers:

* A container running the on-premise provisioning agent. 
* A container running a pre-compiled version of the OrangeHRM connector sample. That sample uses the Okta SDK to host a SCIM service.


## How to install

> **Note**: The install instructions were tested on an M1 Mac.  They should work on any mac, but will require some minor adjustment for windows (openssl tools are used in the build on these images).

> **Pre-requisites**
> * Docker installed on your machine
> * OpenSSL (already installed on Mac/Linux)

### Step 1 - Download this repository
```console
git clone https://github.com/indranilokg/OrangeHRM-Connector.git
```

### Step 2 - Copy and fill in the .env file
```console
cp .env.example .env
```
Follow the instructions in the file to fill in the 3 variables needed.

### Step 3 - Build the images
```console
bash build.sh
```
During the build process, you'll be presented with a login URL (OAuth2 device-code flow) to your chosen Okta tenant. Follow the instructions at the prompt to authorize the OPP agent that is being built.

### Step 4 - Run!
```console
docker compose up
```
This command will run the built images, and you're all set to show off Okta on-premise provisioning!
In the Okta console in the dashboard->agents menu you can see that the OPP agent is connected.
To validate the operation of the SCIM service, visit the following URL:
[http://localhost:8080/okta-connector-orangehrm-1.0.war/Users]()

In Okta, when configuring OPP, the base URL is:
[http://localhost:8080/okta-connector-orangehrm-1.0.war]()
