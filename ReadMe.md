# Java Sample Code that Send payload Zip File to IoTHub
 
This project demos how to send Zip File from Azure Functions to IoTHub (Cloud to Device) using Java.

Like most message transfering system, there are max message size limitations when using Azure IoTHub. 
For Device to Cloud message, it's 256 KB and for Cloud to Device message, it's 64 KB. 

If you have big payload to transfer, the best practice is upload the data to Azure Blob storage  and use IoTHub to send send a notification with messages about how to download the file. 

In some cases you just want to send the data payload in compressed format (if they can stay within 64 KB limitation) in IoTHub so you can prevent the extra implementation. 

This project is the implementation in Java.    






