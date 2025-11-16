package libsys.comp41720.borrowservice.repository;

import libsys.comp41720.borrowservice.entity.BorrowRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BorrowRecordRepository extends MongoRepository<BorrowRecord, String> {
    
    List<BorrowRecord> findByMemberId(String memberId);
    
    List<BorrowRecord> findByBookIsbn(String bookIsbn);
    
    List<BorrowRecord> findByStatus(BorrowRecord.BorrowStatus status);
    
    @Query("{'memberId': ?0, 'status': 'ACTIVE'}")
    List<BorrowRecord> findActiveBorrowsByMember(String memberId);
    
    @Query("{'status': 'ACTIVE', 'dueDate': {$lt: ?0}}")
    List<BorrowRecord> findOverdueBorrows(LocalDate currentDate);
    
    @Query("{'memberId': ?0, 'bookIsbn': ?1, 'status': 'ACTIVE'}")
    List<BorrowRecord> findActiveBorrowByMemberAndBook(String memberId, String bookIsbn);
}

