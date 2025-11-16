package libsys.comp41720.apigateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
@Slf4j
public class FallbackController {
    
    @GetMapping("/book-service")
    public ResponseEntity<Map<String, Object>> bookServiceFallback() {
        log.warn("Book Service fallback triggered at: {}", LocalDateTime.now());
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Book Service is temporarily unavailable");
        response.put("message", "Please try again later. The service might be experiencing high load or is under maintenance.");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        
        return new ResponseEntity<>(response, HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    @GetMapping("/borrow-service")
    public ResponseEntity<Map<String, Object>> borrowServiceFallback() {
        log.warn("Borrow Service fallback triggered at: {}", LocalDateTime.now());
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Borrow Service is temporarily unavailable");
        response.put("message", "Please try again later. The service might be experiencing high load or is under maintenance.");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        
        return new ResponseEntity<>(response, HttpStatus.SERVICE_UNAVAILABLE);
    }
}

