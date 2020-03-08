package com.herman.sample.functions;

import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.sdk.iot.service.DeliveryAcknowledgement;
import com.microsoft.azure.sdk.iot.service.IotHubServiceClientProtocol;
import com.microsoft.azure.sdk.iot.service.ServiceClient;

import com.microsoft.azure.sdk.iot.service.devicetwin.DeviceMethod;
import com.microsoft.azure.sdk.iot.service.devicetwin.MethodResult;
import com.microsoft.azure.sdk.iot.service.exceptions.IotHubException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import java.nio.charset.StandardCharsets;

import com.microsoft.azure.sdk.iot.service.Message;

import com.microsoft.azure.functions.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Azure Functions with HTTP Trigger.
 */
public class DeviceCommandSender {

  public static final String methodName = "DeviceCommandSender";
  public static final Long responseTimeout = TimeUnit.SECONDS.toSeconds(30);
  public static final Long connectTimeout = TimeUnit.SECONDS.toSeconds(5);
  // client connection via service sdk
  private static ServiceClient client = IoTHubServiceClient.getInstance().getClient();

  @FunctionName("DeviceCommandSender")
  public HttpResponseMessage run(
      @HttpTrigger(name = "req", 
        methods = {HttpMethod.GET, HttpMethod.POST}, 
        authLevel = AuthorizationLevel.ANONYMOUS) 
      HttpRequestMessage<Optional<java.lang.Byte[]>> request,      
      final ExecutionContext context) {
    
    context.getLogger().info("Java HTTP trigger processed a request.");

    String query = request.getQueryParameters().get("c2dmethod");
    String deviceprefix = request.getQueryParameters().get("deviceprefix");
    String totaldevicecount = request.getQueryParameters().get("totaldevicenum");

    context.getLogger().info("Sent message using " + query);
    // Parse query parameter
    
    java.lang.Byte[] body = request.getBody().get();
    //String body = request.getBody().get(); 
    //byte[] scanBytes = Base64.getDecoder().decode(base64 string from request);


    long bodyLength = body.length;
    String contentLength = request.getHeaders().get("content-length");

    context.getLogger().info("Save Message size  " + bodyLength);
    context.getLogger().info("content-lenght from header " + contentLength);
    //String contentLength = request.getHeaders().get("content-length");
    //InputStream inputStream = new ByteArrayInputStream(body.getBytes());

    Byte[]  payload = body; //body.getBytes();

 

 
    if (payload == null) {
      return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
          .body("Please pass payload in the message body").build();
    }
    else if(query==null){
      return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
          .body("Please pass query in the c2dmethod parameter").build();
    } 
    else if(deviceprefix==null){
      return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
          .body("Please pass deviceprefix in the query parameter").build();
    } 
    else if(totaldevicecount==null){
      return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
          .body("Please pass totaldevicenum in the query parameter").build();
    } 
    else {
      int totaldevicenum=Integer.parseInt(totaldevicecount);

      context.getLogger().info("Prepare to send  message ");
      // correlation id for the operation
      final String correlationId = UUID.randomUUID().toString();
      final int totalMessages = 3;
      DeviceCommandOperation op = new DeviceCommandOperation(correlationId, totalMessages);

      // check connection
      String json = createMessage(correlationId);

      //byte[] content=request.getBody().orElse(null);
      //byte[] binary =content.getBytes(StandardCharsets.UTF_8);

      String filename="ABC.zip";
      Path path = Paths.get(filename);
     
      byte[] bytes=new byte[payload.length];

      int t=0;
      for(Byte b: payload) {
        bytes[t++] = b.byteValue();
      }
      
      try {
        Files.write(path, bytes);	// Java 7+ only
        System.out.println("Successfully written data to the file");
      }
      catch (IOException e) {
        e.printStackTrace();
      }


      try {
        for (int j = 0; j < 3; j++) {
          for (int i = 1; i <= totaldevicenum; i++) {
            Message message = new Message(bytes);

            message.setCorrelationId(correlationId);          
            message.setTo(deviceprefix + i);
            message.setUserId("DeviceCommandSender");
            message.setMessageId(UUID.randomUUID().toString());
            // message.setDeliveryAcknowledgementFinal(DeliveryAcknowledgement.None);
            // message.setDeliveryAcknowledgementFinal(DeliveryAcknowledgement.Full);
            // client.sendAsync("device6", message);      
            if(query.equals("message")){
              client.send(deviceprefix + i, message);   
            }
            else if(query.equals("direct")){
              DeviceMethod methodClient = DeviceMethod.createFromConnectionString(IoTHubServiceClient.iotHubConnectionString);

              // Call the direct method.
              MethodResult result = methodClient.invoke("herman0" + i, methodName, responseTimeout, connectTimeout, payload);
        
              if (result == null) {
                throw new IOException("Direct method invoke returns null");
              }
        
            } 
            else {
              throw new Exception("Please specify connection type, eg: c2dmethod=message or c2dmethod=direct ");
            }    
            context.getLogger().info("Sent message " + i);
          }
        }
      }
      catch(Exception e) {
        context.getLogger().warning(e.getLocalizedMessage());
        return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
          .body("Error: " + e.getLocalizedMessage()).build();
      }

      return request.createResponseBuilder(HttpStatus.OK).body(json).build();
    }
  }

  private String createMessage(String correlationId) {
    String json = "{\"message\": \"Hello world-" + correlationId + "\"}";

    return json;
  }
}
