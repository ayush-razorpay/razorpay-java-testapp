package com.razorpay;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import io.dropwizard.views.View;

import java.util.HashMap;
import java.util.Map;

@Path("/payment")
public class PaymentResource {

  private ObjectMapper mapper = new ObjectMapper();
  private RazorpayClient client;
  private int amount = 500;

  private String apiKey;
  private String secretKey;
  
  public PaymentResource(String apiKey, String secretKey) {
    this.apiKey = apiKey;
    this.secretKey = secretKey;
    try {
      this.client = new RazorpayClient(this.apiKey, this.secretKey);
    } catch (RazorpayException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  public View getPaymentForm() throws RazorpayException {
    JSONObject options = new JSONObject();
    options.put("amount", amount); // Note: The amount should be in paise.
    options.put("currency", "INR");
    options.put("receipt", "txn_123456");
    options.put("payment_capture", 1);
    Order order = client.Orders.create(options);
    return new PaymentView(amount, (String) order.get("id"));
  }

  @POST
  @Path("/charge")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.APPLICATION_JSON)
  public Response charge(MultivaluedMap<String, String> formParams) {


    String paymentId = formParams.getFirst("razorpay_payment_id");
    String razorpaySignature = formParams.getFirst("razorpay_signature");
    String orderId = formParams.getFirst("razorpay_order_id");
    Map<String,String> toReturn = new HashMap<>();
    JSONObject options = new JSONObject();

    if (StringUtils.isNotBlank(paymentId) && StringUtils.isNotBlank(razorpaySignature)
            && StringUtils.isNotBlank(orderId)) {
      try {
        options.put("razorpay_payment_id", paymentId);
        options.put("razorpay_order_id", orderId);
        options.put("razorpay_signature", razorpaySignature);
        boolean isEqual = Utils.verifyPaymentSignature(options, this.secretKey);

        toReturn.put("signature checked","signature validated");
        toReturn.put("data",formParams.toString());
        if (isEqual) {

        return   Response.ok().entity(toReturn.toString()).build();
        }
        return Response.status(Status.BAD_REQUEST).build();
      } catch (RazorpayException e) {
        System.out.println("Exception caused because of " + e.getMessage());
        return Response.status(Status.BAD_REQUEST).build();
      }
    } else {
      //todo: Add you logic here if needed

     System.out.println("Payment has failed. Processing payment failed callback " );
     toReturn.put("signature checked","signature not validated");;
     toReturn.put("data",formParams.toString());

     return Response.ok().entity(toReturn.toString()).build();

    }

  }


}
