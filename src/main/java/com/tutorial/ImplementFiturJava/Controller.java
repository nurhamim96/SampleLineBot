package com.tutorial.ImplementFiturJava;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.client.LineSignatureValidator;
import com.linecorp.bot.model.Multicast;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.StickerMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.objectmapper.ModelObjectMapper;
import com.linecorp.bot.model.profile.UserProfileResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

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
            if(!lineSignatureValidator.validateSignature(eventPayload.getBytes(), xLineSignature)) {
                throw new RuntimeException("Invalid Signature Validation");
            }

            // parsing event
            ObjectMapper objectMapper = ModelObjectMapper.createNewObjectMapper();
            EventModel eventModel = objectMapper.readValue(eventPayload, EventModel.class);

            eventModel.getEvents().forEach((event)->{
                // kode replay message disini.
                if (event instanceof MessageEvent) {
                    MessageEvent messageEvent = (MessageEvent) event;
                    TextMessageContent textMessageContent = (TextMessageContent) messageEvent.getMessage();
                    replyText(messageEvent.getReplyToken(), textMessageContent.getText());
                }
            });

            return new ResponseEntity<>(HttpStatus.OK);

        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value="/pushmessage/{id}/{message}", method=RequestMethod.GET)
    public ResponseEntity<String> pushMessage(
            @PathVariable("id") String userId,
            @PathVariable("message") String textMsg
    ) {
        TextMessage textMessage = new TextMessage(textMsg);
        PushMessage pushMessage = new PushMessage(userId, textMessage);
        push(pushMessage);

        return new ResponseEntity<>("Push message: " + textMsg + "\n sent to: " + userId, HttpStatus.OK );
    }

    @RequestMapping(value="/multicast", method=RequestMethod.GET)
    public ResponseEntity<String> multicast() {
        String[] userList = {
                "Uf26c938720a4b57c45880ba3965631ad",
                "Uf26c938720a4b57c45880ba3965631ad",
                "Uf26c938720a4b57c45880ba3965631ad",
                "Uf26c938720a4b57c45880ba3965631ad",
                "Uf26c938720a4b57c45880ba3965631ad"
        };

        Set<String> listUsers = new HashSet<>(Arrays.asList(userList));
        if (listUsers.size() > 0) {
            String textMsg = "Halo, Have a nice day!";
            sendMulticast(listUsers, textMsg);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/profile/{id}", method = RequestMethod.GET)
    public ResponseEntity<String> profile(
            @PathVariable("id") String userId
    ) {
        UserProfileResponse profile = getProfile(userId);

        if (profile != null) {
            String profileName = profile.getDisplayName();
            TextMessage textMessage = new TextMessage("Helo, " + profileName + ". Apakabar?");
            PushMessage pushMessage = new PushMessage(userId, textMessage);
            push(pushMessage);

            return new ResponseEntity<String>("Hello, " +profileName, HttpStatus.OK);
        }

        return new ResponseEntity<String >(HttpStatus.NOT_FOUND);
    }

    private UserProfileResponse getProfile(String userId) {
        try {
            return lineMessagingClient.getProfile(userId).get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void sendMulticast(Set<String> sourceUsers, String textMessage) {
        TextMessage message = new TextMessage(textMessage);
        Multicast multicast = new Multicast(sourceUsers, message);

        try {
            lineMessagingClient.multicast(multicast).get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void push(PushMessage pushMessage) {
        try {
            lineMessagingClient.pushMessage(pushMessage).get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void replyText(String replyToken, String messageToUser) {
        TextMessage textMessage = new TextMessage(messageToUser);
        ReplyMessage replyMessage = new ReplyMessage(replyToken, textMessage);
        reply(replyMessage);
    }

    private void reply(ReplyMessage replyMessage) {
        try {
            lineMessagingClient.replyMessage(replyMessage).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void replySticker(String replyToken, String packageId, String stickerId) {
        StickerMessage stickerMessage = new StickerMessage(packageId, stickerId);
        ReplyMessage replyMessage = new ReplyMessage(replyToken, stickerMessage);
        reply(replyMessage);
    }
}
