package edu.cmu.bookstore.web_app_bff;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

    @GetMapping(value = "/status", produces = "text/plain")
    public ResponseEntity<String> status() {
        return ResponseEntity.ok("OK");
    }
}
