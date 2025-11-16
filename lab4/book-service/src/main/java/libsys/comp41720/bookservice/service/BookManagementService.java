package libsys.comp41720.bookservice.service;

import libsys.comp41720.bookservice.dto.BookCreationRequest;
import libsys.comp41720.bookservice.dto.BookDTO;
import libsys.comp41720.bookservice.entity.Book;
import libsys.comp41720.bookservice.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookManagementService {
    
    private final BookRepository bookRepository;
    
    @Transactional
    public BookDTO createBook(BookCreationRequest request) {
        log.info("Creating new book with ISBN: {}", request.getIsbn());
        
        if (bookRepository.existsByIsbn(request.getIsbn())) {
            throw new IllegalArgumentException("Book with ISBN " + request.getIsbn() + " already exists");
        }
        
        Book book = new Book();
        book.setIsbn(request.getIsbn());
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPublisher(request.getPublisher());
        book.setPublicationYear(request.getPublicationYear());
        book.setCategory(request.getCategory());
        book.setTotalCopies(request.getTotalCopies());
        book.setAvailableCopies(request.getTotalCopies());
        book.setStatus(Book.BookStatus.AVAILABLE);
        
        Book savedBook = bookRepository.save(book);
        log.info("Successfully created book: {}", savedBook.getIsbn());
        
        return BookDTO.fromEntity(savedBook);
    }
    
    @Transactional(readOnly = true)
    public List<BookDTO> getAllBooks() {
        log.info("Fetching all books");
        return bookRepository.findAll()
                .stream()
                .map(BookDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public BookDTO getBookByIsbn(String isbn) {
        log.info("Fetching book with ISBN: {}", isbn);
        return bookRepository.findByIsbn(isbn)
                .map(BookDTO::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with ISBN: " + isbn));
    }
    
    @Transactional(readOnly = true)
    public List<BookDTO> searchBooksByTitle(String title) {
        log.info("Searching books by title: {}", title);
        return bookRepository.findByTitleContainingIgnoreCase(title)
                .stream()
                .map(BookDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<BookDTO> searchBooksByAuthor(String author) {
        log.info("Searching books by author: {}", author);
        return bookRepository.findByAuthorContainingIgnoreCase(author)
                .stream()
                .map(BookDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<BookDTO> getBooksByCategory(String category) {
        log.info("Fetching books in category: {}", category);
        return bookRepository.findByCategory(category)
                .stream()
                .map(BookDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<BookDTO> getAvailableBooks() {
        log.info("Fetching all available books");
        return bookRepository.findAllAvailableBooks()
                .stream()
                .map(BookDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public boolean reserveBook(String isbn, int copiesNeeded) {
        log.info("Attempting to reserve {} copies of book: {}", copiesNeeded, isbn);
        
        Book book = bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new IllegalArgumentException("Book not found: " + isbn));
        
        if (book.getAvailableCopies() < copiesNeeded) {
            log.warn("Insufficient copies available. Requested: {}, Available: {}", 
                    copiesNeeded, book.getAvailableCopies());
            return false;
        }
        
        book.setAvailableCopies(book.getAvailableCopies() - copiesNeeded);
        
        if (book.getAvailableCopies() == 0) {
            book.setStatus(Book.BookStatus.BORROWED);
        }
        
        bookRepository.save(book);
        log.info("Successfully reserved {} copies of book: {}", copiesNeeded, isbn);
        
        return true;
    }
    
    @Transactional
    public boolean releaseBook(String isbn, int copiesReturned) {
        log.info("Releasing {} copies of book: {}", copiesReturned, isbn);
        
        Book book = bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new IllegalArgumentException("Book not found: " + isbn));
        
        book.setAvailableCopies(book.getAvailableCopies() + copiesReturned);
        
        if (book.getAvailableCopies() > 0 && book.getStatus() == Book.BookStatus.BORROWED) {
            book.setStatus(Book.BookStatus.AVAILABLE);
        }
        
        bookRepository.save(book);
        log.info("Successfully released {} copies of book: {}", copiesReturned, isbn);
        
        return true;
    }
}

