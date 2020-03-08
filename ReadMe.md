# Azure Functions (Java) sample  that sends binary payload (zip file) to IoTHub
 
This sample code demos how to some binary payload (zip file) from Azure Functions to IoTHub (Cloud to Device) using Java.

Like most message exchange system, there are max message size limitations when using Azure IoTHub. For Device to Cloud message, it's 256 KB and for Cloud to Device message, it's 64 KB. ( [IoT Hub quotas and throttling](https://docs.microsoft.com/zh-tw/azure/iot-hub/iot-hub-devguide-quotas-throttling) )

If you have a big payload to transfer, the best practice is to upload the data to Azure Blob storage and use IoTHub to send a notification with messages about how to download the file.
In some cases, you just want to send the data payload in compressed format (if they can stay within 64 KB limitation) in IoTHub so you can prevent the extra implementation.

This project is the implementation in Java.







