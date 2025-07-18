package com.ssginc8.docto.fcm.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssginc8.docto.fcm.dto.AddFcmToken;
import com.ssginc8.docto.fcm.dto.FcmMessageRequest;
import com.ssginc8.docto.fcm.service.FirebaseCloudMessageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/fcm")
@RequiredArgsConstructor
public class FirebaseController {

   private final FirebaseCloudMessageService firebaseCloudMessageService;

   @PostMapping("/sendMessage")
   public ResponseEntity<Void> sendMessage(@RequestBody FcmMessageRequest request) {
      firebaseCloudMessageService.sendMessage(request.getUserId(), request.getTitle(), request.getBody());

      return ResponseEntity.ok().build();
   }


   @PutMapping("/token")
   public ResponseEntity<Void> registerToken(@RequestBody AddFcmToken.Request request) {
      firebaseCloudMessageService.saveToken(request.getUserId(), request.getToken());

      return ResponseEntity.ok().build();
   }
}
