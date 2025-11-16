package libsys.comp41720.borrowservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BorrowRequest {
    
    @NotBlank(message = "Member ID is required")
    private String memberId;
    
    @NotBlank(message = "Member name is required")
    private String memberName;
    
    @NotBlank(message = "Member email is required")
    @Email(message = "Invalid email format")
    private String memberEmail;
    
    @NotBlank(message = "Book ISBN is required")
    private String bookIsbn;
    
    @NotBlank(message = "Book title is required")
    private String bookTitle;
    
    private Integer borrowDays;
}

