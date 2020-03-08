// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

'use strict';

// console library
const chalk = require('chalk');
const fs = require('fs');
const os = require('os');

// mqtt related variables
var Mqtt = require('azure-iot-device-mqtt').Mqtt;
var Amqp = require('azure-iot-device-amqp').Amqp;
var DeviceClient = require('azure-iot-device').Client
var Message = require('azure-iot-device').Message;
var Http = require('azure-iot-device-http').Http;
var uuid = require('uuid/v1');

// iot hub client devices
var clients = {};



var connectionStrings = {};

// timeout created by setInterval
var intervalLoop = null;
var gatewayMode = false;
var senderMode = false;
var certificatePath = '';
var options = {};
var jsonPayload = JSON.parse(fs.readFileSync('payloads/myevent.json'));
var messagesPerMinute = 100;
var messageInterval = 15;
var correlationId = uuid();

function main() {
  // grab gwmode arg
  if (process.argv.length >= 3 && process.argv[2] == 'gwmode') {
    gatewayMode = true;

    if (process.argv.length < 4) {
      console.log("Missing certificate file");
      process.exit(1);
    }
    else {
      certificatePath = process.argv[3];
      options = {
        ca : fs.readFileSync(certificatePath, 'utf-8'),
      };
    }
  } 
  
  // check for sender parameter
  for (var i = 0; i < process.argv.length; i++) {
    if (process.argv[i] == 'sender') {
      messagesPerMinute = parseInt(process.argv[i+1]);
      senderMode = true;
      break;
    }
  }

  // print mode
  if (gatewayMode) {
    console.log("Starting Agent in gateway mode...");
  }
  else {
    console.log("Starting Agent in direct mode");
  }

  loadConnectionStrings();
  configureConnectionStrings(gatewayMode);
  connectClients();  
}

// loads connection strings into file
function loadConnectionStrings() {
  var fileLines = fs.readFileSync("setup/connstrings.txt", "utf-8").split("\n");
  fileLines.forEach(function(value) {
    var deviceArray = value.split("|");

    if (deviceArray.length == 2 && typeof(deviceArray) != 'undefined') {  
      const device = deviceArray[0];
      const connString = deviceArray[1];    
      connectionStrings[device] = connString;
    }
  });
}

// configures connection strings for all devices
function configureConnectionStrings(gatewayMode) {
  Object.keys(connectionStrings).forEach(function(key) {
    if (gatewayMode) {
      connectionStrings[key] += ';GatewayHostName=EdgeGateway001';
    }
  });  
}

// connects all clients
function connectClients() {
  var counter = 0;

  Object.keys(connectionStrings).forEach(function(key) {
    const connectionString = connectionStrings[key];
    const client =  DeviceClient.fromConnectionString(connectionString, Mqtt);

    // set options and connect
    client.setOptions(options, function(err) {
      if (err) {
        console.log('SetOptions Error: ' + err);
      } 
      else {  
        client.open(function(err) { 
          // sets client          
          clients[key] = client;

          // check for error
          if (err) {
            console.log("Error occurred opening connection: " + err);
            process.exit(1);
          }
          else {
            console.log("Client connected " + key);
          }
          
          // sets up message receiver
          client.on('message', function (msg) {
            // console.log("Connection string is " + connectionStrings[key]);
            // console.log("Client is " + clients[key]);

            const fs = require('fs');
            const uuidv4 = require('uuid/v4'); // I chose v4 ‒ you can select others
            var filename = uuidv4();

            fs.writeFile("./tmp/test-"+filename, msg.data, function(err) {
                if(err) {
                    return console.log(err);
                }

                console.log("The file was saved!");
            }); 


              var message = new Message("Message Success");

              // Add a custom application property to the message.
              // An IoT hub can filter on these properties without access to the message body.
              message.properties.add('authreply', 'true');
              //console.log('Sending auth reply: ' + message.getData().toString().substring(0, 1024));

              client.sendEvent(message, function (err) {
                if (err) {
                  console.error('send error in auth reply: ' + err.toString());
                }  
              });
              
            });  //  end:  client.on('m      
        });

          // on error
        client.on('error', function (err) {
          console.error(err.message);
        });

        // increment counter
        counter++;

        if (counter == Object.keys(connectionStrings).length) {
          console.log("Connected all " + counter + " clients");

          if (senderMode) {
            console.log("Starting sender mode with " + messagesPerMinute + " messages per minute...");
            intervalLoop = setInterval(sendMessage, messageInterval * 1000);
          }
          else {
            console.log("Receiver mode only");
          }            
        }
        
      }
    });

  });     
}

// Send a telemetry message to your hub
function sendMessage() {  
  var numberOfMessages = parseInt((messagesPerMinute / messageInterval) * (60 / messageInterval), 10);
  var clientNames = [];
  var clientKeys = Object.keys(clients);
  var batchId = uuid();

  for (var i = 0; i < numberOfMessages; i++) {
    var index = Math.floor(Math.random() * clientKeys.length);
    console.log("Pushing index " + clientKeys[index]);
    clientNames.push(index);
  }

  // Send the message.
  clientNames.forEach(function(value, index) {
    console.log("Sending with client " + value);
    var client = clients[clientKeys[value]];

    // Simulate telemetry.
    var freshPayload = jsonPayload;
    freshPayload['createTS'] =  parseInt(new Date().getTime() / 1000, 10); // will make it easier to query on cosmos
    freshPayload['correlationId'] = correlationId;
    freshPayload['batchId'] = batchId;

    // message
    var message = new Message(JSON.stringify(freshPayload));

    // Add a custom application property to the message.
    // An IoT hub can filter on these properties without access to the message body.
    message.properties.add('agentSimulator', 'true');

    console.log(value + ' sending message: ' + message.getData());

    client.sendEvent(message, function (err) {
      if (err) {
        console.error('send error: ' + err.toString());
      }  
    });
  });
}

// start application
main();
