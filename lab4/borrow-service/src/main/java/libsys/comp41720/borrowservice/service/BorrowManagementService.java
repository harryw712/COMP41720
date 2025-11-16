package libsys.comp41720.borrowservice.service;

import libsys.comp41720.borrowservice.dto.BorrowRecordDTO;
import libsys.comp41720.borrowservice.dto.BorrowRequest;
import libsys.comp41720.borrowservice.entity.BorrowRecord;
import libsys.comp41720.borrowservice.grpc.BookServiceGrpcClient;
import libsys.comp41720.borrowservice.repository.BorrowRecordRepository;
import libsys.comp41720.events.BorrowEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BorrowManagementService {
    
    private final BorrowRecordRepository borrowRecordRepository;
    private final BookServiceGrpcClient bookServiceGrpcClient;
    private final KafkaTemplate<String, BorrowEvent> kafkaTemplate;
    
    private static final int DEFAULT_BORROW_DAYS = 14;
    private static final String BORROW_TOPIC = "borrow-events";
    
    @Transactional
    public BorrowRecordDTO borrowBook(BorrowRequest request) {
        log.info("Processing borrow request for member: {}, book: {}", 
                request.getMemberId(), request.getBookIsbn());
        
        // Check if book is available via gRPC
        boolean isAvailable = bookServiceGrpcClient.checkBookAvailability(request.getBookIsbn());
        if (!isAvailable) {
            throw new IllegalStateException("Book is not available for borrowing");
        }
        
        // Check if member already has an active borrow for this book
        List<BorrowRecord> existingBorrows = borrowRecordRepository
                .findActiveBorrowByMemberAndBook(request.getMemberId(), request.getBookIsbn());
        
        if (!existingBorrows.isEmpty()) {
            throw new IllegalStateException("Member already has an active borrow for this book");
        }
        
        // Reserve book via gRPC
        boolean reserved = bookServiceGrpcClient.reserveBook(request.getBookIsbn(), 1);
        if (!reserved) {
            throw new IllegalStateException("Failed to reserve book");
        }
        
        try {
            // Create borrow record
            BorrowRecord borrowRecord = new BorrowRecord();
            borrowRecord.setId(UUID.randomUUID().toString());
            borrowRecord.setMemberId(request.getMemberId());
            borrowRecord.setMemberName(request.getMemberName());
            borrowRecord.setMemberEmail(request.getMemberEmail());
            borrowRecord.setBookIsbn(request.getBookIsbn());
            borrowRecord.setBookTitle(request.getBookTitle());
            borrowRecord.setBorrowDate(LocalDate.now());
            
            int borrowDays = request.getBorrowDays() != null ? request.getBorrowDays() : DEFAULT_BORROW_DAYS;
            borrowRecord.setDueDate(LocalDate.now().plusDays(borrowDays));
            
            borrowRecord.setStatus(BorrowRecord.BorrowStatus.ACTIVE);
            borrowRecord.setCreatedAt(LocalDateTime.now());
            borrowRecord.setUpdatedAt(LocalDateTime.now());
            
            BorrowRecord savedRecord = borrowRecordRepository.save(borrowRecord);
            log.info("Borrow record created successfully: {}", savedRecord.getId());
            
            // Send Kafka event
            publishBorrowEvent(savedRecord, "BORROW");
            
            return BorrowRecordDTO.fromEntity(savedRecord);
            
        } catch (Exception e) {
            // Compensate: release the reserved book
            log.error("Failed to create borrow record, releasing reserved book", e);
            bookServiceGrpcClient.releaseBook(request.getBookIsbn(), 1);
            throw new RuntimeException("Failed to complete borrow transaction", e);
        }
    }
    
    @Transactional
    public BorrowRecordDTO returnBook(String borrowId) {
        log.info("Processing return for borrow ID: {}", borrowId);
        
        BorrowRecord borrowRecord = borrowRecordRepository.findById(borrowId)
                .orElseThrow(() -> new IllegalArgumentException("Borrow record not found"));
        
        if (borrowRecord.getStatus() != BorrowRecord.BorrowStatus.ACTIVE) {
            throw new IllegalStateException("Borrow record is not active");
        }
        
        // Release book via gRPC
        boolean released = bookServiceGrpcClient.releaseBook(borrowRecord.getBookIsbn(), 1);
        if (!released) {
            throw new IllegalStateException("Failed to release book");
        }
        
        // Update borrow record
        borrowRecord.setReturnDate(LocalDate.now());
        borrowRecord.setStatus(BorrowRecord.BorrowStatus.RETURNED);
        borrowRecord.setUpdatedAt(LocalDateTime.now());
        
        BorrowRecord updatedRecord = borrowRecordRepository.save(borrowRecord);
        log.info("Book returned successfully: {}", borrowId);
        
        // Send Kafka event
        publishBorrowEvent(updatedRecord, "RETURN");
        
        return BorrowRecordDTO.fromEntity(updatedRecord);
    }
    
    public List<BorrowRecordDTO> getBorrowsByMember(String memberId) {
        log.info("Fetching borrows for member: {}", memberId);
        return borrowRecordRepository.findByMemberId(memberId)
                .stream()
                .map(BorrowRecordDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    public List<BorrowRecordDTO> getActiveBorrows() {
        log.info("Fetching all active borrows");
        return borrowRecordRepository.findByStatus(BorrowRecord.BorrowStatus.ACTIVE)
                .stream()
                .map(BorrowRecordDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    public List<BorrowRecordDTO> getOverdueBorrows() {
        log.info("Fetching overdue borrows");
        return borrowRecordRepository.findOverdueBorrows(LocalDate.now())
                .stream()
                .map(BorrowRecordDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    private void publishBorrowEvent(BorrowRecord record, String eventType) {
        try {
            BorrowEvent event = BorrowEvent.newBuilder()
                    .setBorrowId(record.getId())
                    .setMemberName(record.getMemberName())
                    .setMemberEmail(record.getMemberEmail())
                    .setBookTitle(record.getBookTitle())
                    .setBookIsbn(record.getBookIsbn())
                    .setBorrowDate(record.getBorrowDate().toString())
                    .setDueDate(record.getDueDate().toString())
                    .setEventType(eventType)
                    .build();
            
            log.info("Publishing borrow event to Kafka: {}", event);
            kafkaTemplate.send(BORROW_TOPIC, record.getId(), event);
            log.info("Borrow event published successfully");
            
        } catch (Exception e) {
            log.error("Failed to publish borrow event to Kafka", e);
            // Don't fail the transaction if Kafka publish fails
        }
    }
}

