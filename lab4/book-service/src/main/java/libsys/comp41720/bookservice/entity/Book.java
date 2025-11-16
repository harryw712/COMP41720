package libsys.comp41720.bookservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "books")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Book {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String isbn;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false)
    private String author;
    
    @Column(nullable = false)
    private String publisher;
    
    @Column(name = "publication_year")
    private Integer publicationYear;
    
    @Column(nullable = false)
    private String category;
    
    @Column(name = "total_copies", nullable = false)
    private Integer totalCopies;
    
    @Column(name = "available_copies", nullable = false)
    private Integer availableCopies;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private BookStatus status;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum BookStatus {
        AVAILABLE,
        BORROWED,
        RESERVED,
        MAINTENANCE,
        LOST
    }
}

