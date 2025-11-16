package libsys.comp41720.borrowservice.dto;

import libsys.comp41720.borrowservice.entity.BorrowRecord;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BorrowRecordDTO {
    
    private String id;
    private String memberId;
    private String memberName;
    private String memberEmail;
    private String bookIsbn;
    private String bookTitle;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private String status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean overdue;
    
    public static BorrowRecordDTO fromEntity(BorrowRecord record) {
        return new BorrowRecordDTO(
            record.getId(),
            record.getMemberId(),
            record.getMemberName(),
            record.getMemberEmail(),
            record.getBookIsbn(),
            record.getBookTitle(),
            record.getBorrowDate(),
            record.getDueDate(),
            record.getReturnDate(),
            record.getStatus().name(),
            record.getNotes(),
            record.getCreatedAt(),
            record.getUpdatedAt(),
            record.isOverdue()
        );
    }
}

