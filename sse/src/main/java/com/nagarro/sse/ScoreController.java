package com.nagarro.sse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class ScoreController{

 private final List<SseEmitter> clients = new CopyOnWriteArrayList<>();
 private final Map<String, Integer> teamScores = new ConcurrentHashMap<>();

@GetMapping(path = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter subscribe() {
    SseEmitter emitter = new SseEmitter(0L);
    clients.add(emitter);
    System.out.println("Client added");
    emitter.onCompletion(() -> clients.remove(emitter));
    emitter.onTimeout(() -> clients.remove(emitter));

    // Keep the connection open and send periodic updates
//    new Thread(() -> {
//        try {
//            while (true) {
//                Thread.sleep(5000);
//                sendScoreUpdateToAllClients(teamScores);
//            }
//        } catch (IOException e) {
//            emitter.complete(); // Remove emitter on error
//        } catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//    }).start();

    return emitter;
}

@PostMapping("/update-score")
public ResponseEntity<String> updateScore(@RequestBody Map<String, Object> payload) {
    try {
        String team = (String) payload.get("team");
        int score = (int) payload.get("score");
        teamScores.put(team, score);
        sendScoreUpdateToAllClients(teamScores);
        return ResponseEntity.ok("Score updated successfully");
    } catch (Exception e) {
        // Log the exception or handle it appropriately
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating score");
    }
}
    
private void sendScoreUpdateToAllClients(Map<String, Integer> teamScores) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    String json = objectMapper.writeValueAsString(teamScores);

    for (SseEmitter clientEmitter : clients) {
        clientEmitter.send(SseEmitter.event().data(json, MediaType.APPLICATION_JSON).name("score-update"));
    }
}


}


