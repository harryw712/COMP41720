package libsys.comp41720.borrowservice.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import libsys.comp41720.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
@Slf4j
public class BookServiceGrpcClient {
    
    @Value("${book.grpc.host:localhost}")
    private String grpcHost;
    
    @Value("${book.grpc.port:50052}")
    private int grpcPort;
    
    private ManagedChannel channel;
    private BookServiceGrpc.BookServiceBlockingStub blockingStub;
    
    @PostConstruct
    public void init() {
        log.info("Initializing gRPC client for Book Service at {}:{}", grpcHost, grpcPort);
        channel = ManagedChannelBuilder
                .forAddress(grpcHost, grpcPort)
                .usePlaintext()
                .build();
        blockingStub = BookServiceGrpc.newBlockingStub(channel);
        log.info("gRPC client initialized successfully");
    }
    
    @PreDestroy
    public void cleanup() {
        if (channel != null && !channel.isShutdown()) {
            log.info("Shutting down gRPC channel");
            channel.shutdown();
        }
    }
    
    public boolean checkBookAvailability(String isbn) {
        try {
            log.info("gRPC Client: Checking availability for ISBN: {}", isbn);
            
            BookAvailabilityRequest request = BookAvailabilityRequest.newBuilder()
                    .setIsbn(isbn)
                    .build();
            
            BookAvailabilityResponse response = blockingStub.checkBookAvailability(request);
            
            log.info("gRPC Client: Availability check result - ISBN: {}, Available: {}, Copies: {}", 
                    isbn, response.getAvailable(), response.getAvailableCopies());
            
            return response.getAvailable();
            
        } catch (StatusRuntimeException e) {
            log.error("gRPC Client: Failed to check book availability for ISBN: {}", isbn, e);
            return false;
        }
    }
    
    public boolean reserveBook(String isbn, int copiesNeeded) {
        try {
            log.info("gRPC Client: Reserving book - ISBN: {}, Copies: {}", isbn, copiesNeeded);
            
            BookReservationRequest request = BookReservationRequest.newBuilder()
                    .setIsbn(isbn)
                    .setCopiesNeeded(copiesNeeded)
                    .build();
            
            BookReservationResponse response = blockingStub.reserveBook(request);
            
            log.info("gRPC Client: Reservation result - ISBN: {}, Success: {}, Message: {}", 
                    isbn, response.getSuccess(), response.getMessage());
            
            return response.getSuccess();
            
        } catch (StatusRuntimeException e) {
            log.error("gRPC Client: Failed to reserve book - ISBN: {}", isbn, e);
            return false;
        }
    }
    
    public boolean releaseBook(String isbn, int copiesReturned) {
        try {
            log.info("gRPC Client: Releasing book - ISBN: {}, Copies: {}", isbn, copiesReturned);
            
            BookReleaseRequest request = BookReleaseRequest.newBuilder()
                    .setIsbn(isbn)
                    .setCopiesReturned(copiesReturned)
                    .build();
            
            BookReleaseResponse response = blockingStub.releaseBook(request);
            
            log.info("gRPC Client: Release result - ISBN: {}, Success: {}, Message: {}", 
                    isbn, response.getSuccess(), response.getMessage());
            
            return response.getSuccess();
            
        } catch (StatusRuntimeException e) {
            log.error("gRPC Client: Failed to release book - ISBN: {}", isbn, e);
            return false;
        }
    }
}

