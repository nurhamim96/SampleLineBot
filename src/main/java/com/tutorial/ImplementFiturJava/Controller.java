package com.tutorial.ImplementFiturJava;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.client.LineSignatureValidator;
import com.linecorp.bot.client.MessageContentResponse;
import com.linecorp.bot.model.Multicast;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.action.MessageAction;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.*;
import com.linecorp.bot.model.event.source.GroupSource;
import com.linecorp.bot.model.event.source.RoomSource;
import com.linecorp.bot.model.message.StickerMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.objectmapper.ModelObjectMapper;
import com.linecorp.bot.model.profile.UserProfileResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
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
//                    MessageEvent messageEvent = (MessageEvent) event;
//                    TextMessageContent textMessageContent = (TextMessageContent) messageEvent.getMessage();
//                    replyText(messageEvent.getReplyToken(), textMessageContent.getText());

                    if (event.getSource() instanceof GroupSource || event.getSource() instanceof RoomSource) {
                        eventModel.getEvents().forEach((event1) -> {
                            if (event1 instanceof MessageEvent) {
                                if (event1.getSource() instanceof GroupSource || event1.getSource() instanceof RoomSource) {
                                    handleGroupRoomChats((MessageEvent) event1);
                                }
                            }
                        });
                    } else {
                        if (((MessageEvent) event).getMessage() instanceof AudioMessageContent
                    || ((MessageEvent) event).getMessage() instanceof ImageMessageContent
                    || ((MessageEvent) event).getMessage() instanceof VideoMessageContent
                    || ((MessageEvent) event).getMessage() instanceof FileMessageContent
                    ) {
                        String baseUrl = "https://devjavalinechatboot.herokuapp.com";
                        String contentURL = baseUrl + "/content/" + ((MessageEvent) event).getMessage().getId();
                        String contentType = ((MessageEvent) event).getMessage().getClass().getSimpleName();
                        String textMsg = contentType.substring(0, contentType.length() -14)
                                + " yang kamu kirim bisa diakses dari link:\n "
                                + contentURL;

                        replyText(((MessageEvent) event).getReplyToken(), textMsg);
                    } else {
                        MessageEvent messageEvent = (MessageEvent) event;
                        TextMessageContent textMessageContent = (TextMessageContent) messageEvent.getMessage();
                        replyText(messageEvent.getReplyToken(), textMessageContent.getText());
                    }
                    }
                }
            });

            return new ResponseEntity<>(HttpStatus.OK);

        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    private void handleGroupRoomChats(MessageEvent event1) {
        if (!event1.getSource().getUserId().isEmpty()){
            String userId = event1.getSource().getUserId();
            UserProfileResponse profile = getProfile(userId);
            replyText(event1.getReplyToken(), "Hallo, " + profile.getDisplayName());
        } else {
            replyText(event1.getReplyToken(), "Hello, what is your name?");
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
                "Uf26c938720a4b57c45880ba3965631ad"
        };

        Set<String> listUsers = new HashSet<>(Arrays.asList(userList));
        if (listUsers.size() > 0) {
            String textMsg = "Halo, Have a nice day!";
            sendMulticast(listUsers, textMsg);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/profile", method = RequestMethod.GET)
    public ResponseEntity<String> getProfile() {
        String userId = "Uf26c938720a4b57c45880ba3965631ad";
        UserProfileResponse profile = getProfile(userId);

        if (profile != null) {
            String profileName = profile.getDisplayName();
            TextMessage textMessage = new TextMessage("Hello, " + profileName + ". Saya Chatboot");
            PushMessage pushMessage = new PushMessage(userId, textMessage);
            push(pushMessage);

            return new ResponseEntity<>("Hello, " + profileName, HttpStatus.OK);
        }

        return new ResponseEntity<>("Hello, " + profile, HttpStatus.NOT_FOUND);
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

    @RequestMapping(value = "/content/{id}", method = RequestMethod.GET)
    public ResponseEntity content(
            @PathVariable("id") String messageId
    ) {
        MessageContentResponse messageContentResponse = getContent(messageId);

        if (messageContentResponse != null) {
            HttpHeaders headers = new HttpHeaders();
            String[] mimeType = messageContentResponse.getMimeType().split("/");
            headers.setContentType(new MediaType(mimeType[0], mimeType[1]));

            InputStream inputStream = messageContentResponse.getStream();
            InputStreamResource inputStreamResource = new InputStreamResource(inputStream);
            
            return new ResponseEntity<>(inputStreamResource, headers, HttpStatus.OK);
        }
        
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    private MessageContentResponse getContent(String messageId) {
        try {
            return lineMessagingClient.getMessageContent(messageId).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
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
