package ie.ucd.comp41720.Lab2.service;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserProfileService {
    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);
    private final MongoClient mongoClient;
    public UserProfileService(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }
    public void checkReplicaSetStatus() {
        try {
            MongoDatabase admin = mongoClient.getDatabase("admin");
            Document status = admin.runCommand(new Document("replSetGetStatus", 1));
            log.info("[LAB2-U] set={}", status.get("set"));
            List<Document> members = (List<Document>) status.get("members");
            for (Document m : members) {
                log.info("[LAB2-U] member={} state={} health={}", m.get("name"), m.get("stateStr"), m.get("health"));
            }
        } catch (Exception e) {
            log.warn("[LAB2-U] replica status error: {}", e.getMessage());
        }
    }
}