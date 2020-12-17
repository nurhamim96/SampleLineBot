package com.tutorial.ImplementFiturJava;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.client.LineSignatureValidator;
import com.linecorp.bot.model.objectmapper.ModelObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
public class Controller {

    @Autowired
    @Qualifier("lineMessagingClient")
    private LineMessagingClient lineMessagingClient;

    @Autowired
    @Qualifier("lineSignatureValidator")
    private LineSignatureValidator lineSignatureValidator;

    @RequestMapping(value = "/webhook", method = RequestMethod.POST)
    public ResponseEntity<String> callBack(
            @RequestHeader("X-Line-Signature") String xLineSignature,
            @RequestBody String eventPayload) {
        try {
//            if(!lineSignatureValidator.validateSignature(eventPayload.getBytes(), xLineSignature)) {
//                throw new RuntimeException("Invalid Signature Validation");
//            }

            // parsing event
            ObjectMapper objectMapper = ModelObjectMapper.createNewObjectMapper();
            EventModel eventModel = objectMapper.readValue(eventPayload, EventModel.class);

            eventModel.getEvents().forEach((event)->{
                // kode replay message disini.
            });

            return new ResponseEntity<>(HttpStatus.OK);

        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
