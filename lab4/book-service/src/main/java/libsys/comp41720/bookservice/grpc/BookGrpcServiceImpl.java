package libsys.comp41720.bookservice.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import libsys.comp41720.bookservice.entity.Book;
import libsys.comp41720.bookservice.repository.BookRepository;
import libsys.comp41720.bookservice.service.BookManagementService;
import libsys.comp41720.grpc.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class BookGrpcServiceImpl extends BookServiceGrpc.BookServiceImplBase {
    
    private final BookManagementService bookManagementService;
    private final BookRepository bookRepository;
    
    @Override
    public void checkBookAvailability(BookAvailabilityRequest request,
                                     StreamObserver<BookAvailabilityResponse> responseObserver) {
        try {
            log.info("gRPC: Checking availability for book ISBN: {}", request.getIsbn());
            
            if (request.getIsbn().isBlank()) {
                String errorMsg = "ISBN cannot be empty";
                log.warn("gRPC: {}", errorMsg);
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription(errorMsg)
                        .asRuntimeException());
                return;
            }
            
            Book book = bookRepository.findByIsbn(request.getIsbn())
                    .orElse(null);
            
            BookAvailabilityResponse response;
            if (book == null) {
                response = BookAvailabilityResponse.newBuilder()
                        .setAvailable(false)
                        .setAvailableCopies(0)
                        .setMessage("Book not found")
                        .build();
            } else {
                boolean available = book.getAvailableCopies() > 0;
                response = BookAvailabilityResponse.newBuilder()
                        .setAvailable(available)
                        .setAvailableCopies(book.getAvailableCopies())
                        .setMessage(available ? "Book is available" : "Book is not available")
                        .build();
            }
            
            log.info("gRPC: Book availability check completed - ISBN: {}, Available: {}", 
                    request.getIsbn(), response.getAvailable());
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("gRPC: Error checking book availability for ISBN: {}", 
                    request.getIsbn(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage())
                    .asRuntimeException());
        }
    }
    
    @Override
    public void reserveBook(BookReservationRequest request,
                           StreamObserver<BookReservationResponse> responseObserver) {
        try {
            log.info("gRPC: Reserving book - ISBN: {}, Copies: {}", 
                    request.getIsbn(), request.getCopiesNeeded());
            
            if (request.getIsbn().isBlank() || request.getCopiesNeeded() <= 0) {
                String errorMsg = "Invalid request: ISBN cannot be empty and copies must be positive";
                log.warn("gRPC: {}", errorMsg);
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription(errorMsg)
                        .asRuntimeException());
                return;
            }
            
            boolean success = bookManagementService.reserveBook(
                    request.getIsbn(), 
                    request.getCopiesNeeded()
            );
            
            Book book = bookRepository.findByIsbn(request.getIsbn()).orElse(null);
            
            BookReservationResponse response = BookReservationResponse.newBuilder()
                    .setSuccess(success)
                    .setMessage(success ? "Book reserved successfully" : "Failed to reserve book - insufficient copies")
                    .setBookId(book != null ? book.getId() : 0L)
                    .build();
            
            log.info("gRPC: Book reservation completed - ISBN: {}, Success: {}", 
                    request.getIsbn(), success);
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("gRPC: Error reserving book - ISBN: {}", request.getIsbn(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage())
                    .asRuntimeException());
        }
    }
    
    @Override
    public void releaseBook(BookReleaseRequest request,
                           StreamObserver<BookReleaseResponse> responseObserver) {
        try {
            log.info("gRPC: Releasing book - ISBN: {}, Copies: {}", 
                    request.getIsbn(), request.getCopiesReturned());
            
            if (request.getIsbn().isBlank() || request.getCopiesReturned() <= 0) {
                String errorMsg = "Invalid request: ISBN cannot be empty and copies must be positive";
                log.warn("gRPC: {}", errorMsg);
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription(errorMsg)
                        .asRuntimeException());
                return;
            }
            
            boolean success = bookManagementService.releaseBook(
                    request.getIsbn(), 
                    request.getCopiesReturned()
            );
            
            BookReleaseResponse response = BookReleaseResponse.newBuilder()
                    .setSuccess(success)
                    .setMessage(success ? "Book released successfully" : "Failed to release book")
                    .build();
            
            log.info("gRPC: Book release completed - ISBN: {}, Success: {}", 
                    request.getIsbn(), success);
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("gRPC: Error releasing book - ISBN: {}", request.getIsbn(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage())
                    .asRuntimeException());
        }
    }
    
    @Override
    public void updateBookStatus(BookStatusUpdateRequest request,
                                StreamObserver<BookStatusUpdateResponse> responseObserver) {
        try {
            log.info("gRPC: Updating book status - ISBN: {}, Status: {}", 
                    request.getIsbn(), request.getStatus());
            
            Book book = bookRepository.findByIsbn(request.getIsbn())
                    .orElse(null);
            
            if (book == null) {
                BookStatusUpdateResponse response = BookStatusUpdateResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Book not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }
            
            book.setStatus(Book.BookStatus.valueOf(request.getStatus()));
            bookRepository.save(book);
            
            BookStatusUpdateResponse response = BookStatusUpdateResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Book status updated successfully")
                    .build();
            
            log.info("gRPC: Book status updated - ISBN: {}", request.getIsbn());
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("gRPC: Error updating book status - ISBN: {}", request.getIsbn(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage())
                    .asRuntimeException());
        }
    }
}

