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

