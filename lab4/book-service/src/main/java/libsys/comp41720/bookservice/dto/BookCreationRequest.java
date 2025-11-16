package libsys.comp41720.bookservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookCreationRequest {
    
    @NotBlank(message = "ISBN is required")
    private String isbn;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotBlank(message = "Author is required")
    private String author;
    
    @NotBlank(message = "Publisher is required")
    private String publisher;
    
    private Integer publicationYear;
    
    @NotBlank(message = "Category is required")
    private String category;
    
    @NotNull(message = "Total copies is required")
    @Min(value = 1, message = "Total copies must be at least 1")
    private Integer totalCopies;
}

