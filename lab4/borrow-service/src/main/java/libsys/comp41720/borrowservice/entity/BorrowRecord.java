package libsys.comp41720.borrowservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "borrow_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BorrowRecord {
    
    @Id
    private String id;
    
    private String memberId;
    private String memberName;
    private String memberEmail;
    
    private String bookIsbn;
    private String bookTitle;
    
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    
    private BorrowStatus status;
    
    private String notes;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public enum BorrowStatus {
        ACTIVE,
        RETURNED,
        OVERDUE,
        RENEWED,
        LOST
    }
    
    public boolean isOverdue() {
        return status == BorrowStatus.ACTIVE && 
               LocalDate.now().isAfter(dueDate);
    }
}

