package libsys.comp41720.notificationservice.service;

import libsys.comp41720.events.BorrowEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {
    
    private final JavaMailSender javaMailSender;
    
    @KafkaListener(topics = "borrow-events", groupId = "notification-service-group")
    public void handleBorrowEvent(@Payload BorrowEvent event,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                 @Header(KafkaHeaders.OFFSET) long offset,
                                 Acknowledgment acknowledgment) {
        
        log.info("Received borrow event from Kafka - Topic: {}, Partition: {}, Offset: {}, Event: {}", 
                topic, partition, offset, event);
        
        try {
            if ("BORROW".equals(event.getEventType().toString())) {
                sendBorrowConfirmationEmail(event);
            } else if ("RETURN".equals(event.getEventType().toString())) {
                sendReturnConfirmationEmail(event);
            }
            
            // Manually acknowledge the message
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
                log.info("Message acknowledged successfully");
            }
            
        } catch (Exception e) {
            log.error("Failed to process borrow event", e);
            // In production, implement retry logic or dead letter queue
        }
    }
    
    private void sendBorrowConfirmationEmail(BorrowEvent event) {
        log.info("Preparing borrow confirmation email for: {}", event.getMemberEmail());
        
        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom("library-system@comp41720.edu");
            helper.setTo(event.getMemberEmail().toString());
            helper.setSubject("Library Book Borrowed: " + event.getBookTitle());
            
            String emailContent = String.format("""
                    Dear %s,
                    
                    This email confirms that you have successfully borrowed the following book:
                    
                    Book Title: %s
                    ISBN: %s
                    Borrow Date: %s
                    Due Date: %s
                    Borrow ID: %s
                    
                    Please return the book on or before the due date to avoid any late fees.
                    
                    Thank you for using our library system!
                    
                    Best Regards,
                    Library Management System
                    COMP41720 Project
                    """,
                    event.getMemberName(),
                    event.getBookTitle(),
                    event.getBookIsbn(),
                    event.getBorrowDate(),
                    event.getDueDate(),
                    event.getBorrowId()
            );
            
            helper.setText(emailContent);
        };
        
        try {
            javaMailSender.send(messagePreparator);
            log.info("Borrow confirmation email sent successfully to: {}", event.getMemberEmail());
        } catch (MailException e) {
            log.error("Failed to send borrow confirmation email to: {}", event.getMemberEmail(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    private void sendReturnConfirmationEmail(BorrowEvent event) {
        log.info("Preparing return confirmation email for: {}", event.getMemberEmail());
        
        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom("library-system@comp41720.edu");
            helper.setTo(event.getMemberEmail().toString());
            helper.setSubject("Library Book Returned: " + event.getBookTitle());
            
            String emailContent = String.format("""
                    Dear %s,
                    
                    This email confirms that you have successfully returned the following book:
                    
                    Book Title: %s
                    ISBN: %s
                    Borrow ID: %s
                    
                    Thank you for returning the book on time.
                    We hope you enjoyed reading it!
                    
                    Best Regards,
                    Library Management System
                    COMP41720 Project
                    """,
                    event.getMemberName(),
                    event.getBookTitle(),
                    event.getBookIsbn(),
                    event.getBorrowId()
            );
            
            helper.setText(emailContent);
        };
        
        try {
            javaMailSender.send(messagePreparator);
            log.info("Return confirmation email sent successfully to: {}", event.getMemberEmail());
        } catch (MailException e) {
            log.error("Failed to send return confirmation email to: {}", event.getMemberEmail(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}

