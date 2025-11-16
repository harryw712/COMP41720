package libsys.comp41720.borrowservice.controller;

import jakarta.validation.Valid;
import libsys.comp41720.borrowservice.dto.BorrowRecordDTO;
import libsys.comp41720.borrowservice.dto.BorrowRequest;
import libsys.comp41720.borrowservice.service.BorrowManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/borrows")
@RequiredArgsConstructor
@Slf4j
public class BorrowRestController {
    
    private final BorrowManagementService borrowManagementService;
    
    @PostMapping
    public ResponseEntity<?> borrowBook(@Valid @RequestBody BorrowRequest request) {
        log.info("REST: Received borrow request for member: {}, book: {}", 
                request.getMemberId(), request.getBookIsbn());
        try {
            BorrowRecordDTO borrowRecord = borrowManagementService.borrowBook(request);
            return new ResponseEntity<>(borrowRecord, HttpStatus.CREATED);
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.error("REST: Failed to process borrow request: {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("REST: Unexpected error processing borrow request", e);
            return new ResponseEntity<>("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/{borrowId}/return")
    public ResponseEntity<?> returnBook(@PathVariable String borrowId) {
        log.info("REST: Received return request for borrow ID: {}", borrowId);
        try {
            BorrowRecordDTO borrowRecord = borrowManagementService.returnBook(borrowId);
            return new ResponseEntity<>(borrowRecord, HttpStatus.OK);
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.error("REST: Failed to process return request: {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("REST: Unexpected error processing return request", e);
            return new ResponseEntity<>("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<BorrowRecordDTO>> getBorrowsByMember(@PathVariable String memberId) {
        log.info("REST: Fetching borrows for member: {}", memberId);
        List<BorrowRecordDTO> borrows = borrowManagementService.getBorrowsByMember(memberId);
        return new ResponseEntity<>(borrows, HttpStatus.OK);
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<BorrowRecordDTO>> getActiveBorrows() {
        log.info("REST: Fetching all active borrows");
        List<BorrowRecordDTO> borrows = borrowManagementService.getActiveBorrows();
        return new ResponseEntity<>(borrows, HttpStatus.OK);
    }
    
    @GetMapping("/overdue")
    public ResponseEntity<List<BorrowRecordDTO>> getOverdueBorrows() {
        log.info("REST: Fetching overdue borrows");
        List<BorrowRecordDTO> borrows = borrowManagementService.getOverdueBorrows();
        return new ResponseEntity<>(borrows, HttpStatus.OK);
    }
}

