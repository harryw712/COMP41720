package libsys.comp41720.bookservice.controller;

import jakarta.validation.Valid;
import libsys.comp41720.bookservice.dto.BookCreationRequest;
import libsys.comp41720.bookservice.dto.BookDTO;
import libsys.comp41720.bookservice.service.BookManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Slf4j
public class BookRestController {
    
    private final BookManagementService bookManagementService;
    
    @PostMapping
    public ResponseEntity<BookDTO> createBook(@Valid @RequestBody BookCreationRequest request) {
        log.info("REST: Received request to create book with ISBN: {}", request.getIsbn());
        try {
            BookDTO createdBook = bookManagementService.createBook(request);
            return new ResponseEntity<>(createdBook, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            log.error("REST: Failed to create book: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
    }
    
    @GetMapping
    public ResponseEntity<List<BookDTO>> getAllBooks() {
        log.info("REST: Fetching all books");
        List<BookDTO> books = bookManagementService.getAllBooks();
        return new ResponseEntity<>(books, HttpStatus.OK);
    }
    
    @GetMapping("/{isbn}")
    public ResponseEntity<BookDTO> getBookByIsbn(@PathVariable String isbn) {
        log.info("REST: Fetching book with ISBN: {}", isbn);
        try {
            BookDTO book = bookManagementService.getBookByIsbn(isbn);
            return new ResponseEntity<>(book, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            log.error("REST: Book not found: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @GetMapping("/search/title")
    public ResponseEntity<List<BookDTO>> searchByTitle(@RequestParam String title) {
        log.info("REST: Searching books by title: {}", title);
        List<BookDTO> books = bookManagementService.searchBooksByTitle(title);
        return new ResponseEntity<>(books, HttpStatus.OK);
    }
    
    @GetMapping("/search/author")
    public ResponseEntity<List<BookDTO>> searchByAuthor(@RequestParam String author) {
        log.info("REST: Searching books by author: {}", author);
        List<BookDTO> books = bookManagementService.searchBooksByAuthor(author);
        return new ResponseEntity<>(books, HttpStatus.OK);
    }
    
    @GetMapping("/category/{category}")
    public ResponseEntity<List<BookDTO>> getBooksByCategory(@PathVariable String category) {
        log.info("REST: Fetching books in category: {}", category);
        List<BookDTO> books = bookManagementService.getBooksByCategory(category);
        return new ResponseEntity<>(books, HttpStatus.OK);
    }
    
    @GetMapping("/available")
    public ResponseEntity<List<BookDTO>> getAvailableBooks() {
        log.info("REST: Fetching available books");
        List<BookDTO> books = bookManagementService.getAvailableBooks();
        return new ResponseEntity<>(books, HttpStatus.OK);
    }
}

