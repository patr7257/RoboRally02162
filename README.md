
# Running the project locally

## Requirements
* Java: JDK 23
* Maven: 3.9.9
* Node.js + npm: for the client


## Install dependencies

### Client
Navigate to `client`and run:
```bash
npm install
```

This installs all required dependencies for the client.


### Host and Gateway

You need JDK 23 and Maven to run both the Host and the Gateway.

#### Without nix
Install JDK 23 and Maven on your system, then verify:

#### With nix
From each module folder (gateway and host), run:
```
nix-shell -p openjdk23 maven
```

## Running the project

Be in the root on the project.

Step 1: Start the Gateway
```bash
cd gateway
mvn spring-boot:run
```

Step 2: Start the Host
```bash
cd host
mvn spring-boot:run
```

Step 3: Start the Client
```bash
cd client
npm start
```
* If prompted to run on a different port, type y.
* If it fails, make sure you ran npm install first.

Multiple clients
To open multiple clients, open a new terminal and repeat Step 3.

# Deploy program

## Requirements
The following are requirements for setting up the service on a server and hosting it. Most of these utilities can be installed using a package manager. 

- Linux machine
- Java version 23
- maven version 3.9.9
- npm & npx for the react app
- nginx
- sudo permission
- potentially cron for automatic deployment.
- a license to use HTTPS, can be self signed.
- a mysql database
    

## Installation
The **scripts** folder contains bash scripts for deploying, stopping, checking status and restarting the entire service. Before they can be run, update the file permissions with 

chmod +x fileNameHere

so that they are executeable.

The following ports need to be allowed and denied for the application to work:

    ufw allow 80/tcp
    ufw allow 443/tcp
    ufw deny 8080/tcp
    ufw deny 2948/tcp


Clone the repository should be in the /opt/roborally/app directory. If it does not exist, create it with 

    mkdir /opt/roborally/app

The scripts folder also contains a nginx config that is needed for allowing communication between the organs of the total service. This config should be moved to the 

    /etc/nginx/sites-available/se2-f.compute.dtu.dk 

directory. After moving, create a symbolic link via 

    sudo ln -s /etc/nginx/sites-available/se2-f.compute.dtu.dk /etc/nginx/sites-enabled/

Lastly remove the default page

    sudo rm /etc/nginx/sites-enabled/default

If another URL rather than __se2-f.compute.dtu.dk__ changes should be made in the following files:


- Gateway
    - SecurityConfig.java
    - CorsConfig.java

- The nginx config

- ./env.production


The mysql database should be created with the name RoboRallyDatabase, and have a mysql user with the name RoboRallyUser, and the password RoboRallyDatabaseUser. When the gateway launches, it will use these credentials to create necessary tables in the database.


Lastly, restart nginx and run the deploy script.