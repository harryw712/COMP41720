package libsys.comp41720.bookservice.repository;

import libsys.comp41720.bookservice.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    
    Optional<Book> findByIsbn(String isbn);
    
    List<Book> findByCategory(String category);
    
    List<Book> findByAuthorContainingIgnoreCase(String author);
    
    List<Book> findByTitleContainingIgnoreCase(String title);
    
    @Query("SELECT b FROM Book b WHERE b.availableCopies > 0")
    List<Book> findAllAvailableBooks();
    
    boolean existsByIsbn(String isbn);
}

