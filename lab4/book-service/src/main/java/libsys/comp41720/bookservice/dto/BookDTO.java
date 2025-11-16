package libsys.comp41720.bookservice.dto;

import libsys.comp41720.bookservice.entity.Book;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookDTO {
    
    private Long id;
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private Integer publicationYear;
    private String category;
    private Integer totalCopies;
    private Integer availableCopies;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static BookDTO fromEntity(Book book) {
        return new BookDTO(
            book.getId(),
            book.getIsbn(),
            book.getTitle(),
            book.getAuthor(),
            book.getPublisher(),
            book.getPublicationYear(),
            book.getCategory(),
            book.getTotalCopies(),
            book.getAvailableCopies(),
            book.getStatus().name(),
            book.getCreatedAt(),
            book.getUpdatedAt()
        );
    }
}

