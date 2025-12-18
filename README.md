
# Running code locally

The code is developed using java jdk-23

We are running Maven version 3.9.9

To run the code do the following steps:

STEP 1 Start gateway server
    Navigate to RoboRally02162\gateway and run the following command: mvn spring-boot:run

STEP 2 Start host logic
    Navigate to RoboRally02162\host and run the following command: mvn spring-boot:run

STEP 3 Setup npm
    Navigate to RoboRally02162\client and run the following commands to setup npm:

    npm install

    npm install --save-dev @types/jest @testing-library/react @testing-library/jest-dom
        
    npm install --save typescript @types/react @types/react-dom

STEP 4 Run client
    Navigate to RoboRally02162\client and run the following command: npm start

    If promted to use a different port type "y" in the terminal

    If it fails make sure you have run the commands in STEP 3

    To open multiple clients open a new terminal and repeat STEP 4




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