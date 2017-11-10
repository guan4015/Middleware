# Middleware
This project creates a middleware that deals with the simulation of stock prices and the computation of option prices based on those 
simulations. Simply, the server generates a request to the client for generating a price sample path and compute a price. Then the server 
receives the price and update the corresponding statistics. Finally, the server decides when to stop.

## Function/Class Description

This project consists of four .java files. 

### Functions

*  The MonteCarloServer.java encapsulates the process of creating connections, sessions and messages as well as receving the ones generated from client.
*  The MonteCarloClient.java encapsulates the process of creating connections, sessions and messages that are connected to the queue created by Server. It processed the request from server and send the result back.
*  The RequestListners.java processes the message received from server. It generates the sample stock paths and compute the price for the paths.
*  The Simulation.java generates two options and simulate the interactions between Server and Client, which computed the prices.


## Implementation of the Middleware

The simulation of the Middleware is wrapped up in the Simulation.java. Once we click on the Debug, it will automatically generate four threads
and compute the prices for us. Note that before running Simulation.java, one should complie the ActiveMQ accordingly.

## Authors

* **Xiao Guan** - *Initial work* - [Exchange](https://github.com/guan4015/Middleware)


## Acknowledgments

The author thanks Professor Eron Fishler for his help on this assignment.
