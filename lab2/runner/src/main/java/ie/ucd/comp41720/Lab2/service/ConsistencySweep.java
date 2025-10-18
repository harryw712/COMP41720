package ie.ucd.comp41720.Lab2.service;

import ie.ucd.comp41720.Lab2.domain.UserProfile;
import com.mongodb.ClientSessionOptions;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class ConsistencySweep {
    private static final Logger log = LoggerFactory.getLogger(ConsistencySweep.class);
    private static final String COLL = "consistency_test";
    private static final String TAG = "[LAB2-C]";
    private final MongoTemplate mongoTemplate;
    public ConsistencySweep(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }
    public void performConsistencyExperiments() {
        log.info("{} start", TAG);
        prepare();
        strong();
        eventual();
        causal();
        cleanup();
        log.info("{} done", TAG);
    }
    private void strong() {
        log.info("{} strong begin", TAG);
        try {
            MongoCollection<Document> c = mongoTemplate.getCollection(COLL).withWriteConcern(WriteConcern.MAJORITY).withReadConcern(ReadConcern.MAJORITY);
            String id = "strong_" + System.currentTimeMillis();
            UserProfile u = makeDoc(id, "strong", 1);
            long t0 = System.nanoTime();
            c.insertOne(asDocument(u));
            long writeNs = System.nanoTime() - t0;
            MongoCollection<Document> readFromSecondary = mongoTemplate.getCollection(COLL).withReadPreference(ReadPreference.secondary()).withReadConcern(ReadConcern.MAJORITY);
            long r0 = System.nanoTime();
            Document got = readFromSecondary.find(Filters.eq("_id", id)).first();
            long readNs = System.nanoTime() - r0;
            if (got != null) {
                log.info("{} strong ok write={}ms read={}ms user={}", TAG, ms(writeNs), ms(readNs), got.getString("username"));
            } else {
                log.warn("{} strong miss on secondary (majority read).", TAG);
            }
            strongDuringStepdown(id);
        } catch (Exception e) {
            log.warn("{} strong error: {}", TAG, e.getMessage());
        }
    }
    private void strongDuringStepdown(String probeId) {
        log.info("{} strong during stepdown", TAG);
        CompletableFuture<Void> writes = majorityWrites();
        CompletableFuture<Void> reads = majorityReads(probeId);
        stepDownPrimary();
        try {
            writes.get(20, TimeUnit.SECONDS);
            reads.get(20, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.info("{} stepdown window affected ops: {}", TAG, e.getMessage());
        }
    }
    private void eventual() {
        log.info("{} eventual begin", TAG);
        try {
            MongoCollection<Document> c = mongoTemplate.getCollection(COLL).withWriteConcern(WriteConcern.W1).withReadConcern(ReadConcern.DEFAULT);
            String id = "eventual_" + System.currentTimeMillis();
            UserProfile u = makeDoc(id, "eventual", 2);
            long t0 = System.nanoTime();
            c.insertOne(asDocument(u));
            long writeNs = System.nanoTime() - t0;
            MongoCollection<Document> secondary = mongoTemplate.getCollection(COLL).withReadPreference(ReadPreference.secondary());
            boolean seen = false;
            int attempts = 0;
            long start = System.nanoTime();
            while (!seen && attempts < 12) {
                Document r = secondary.find(Filters.eq("_id", id)).first();
                if (r != null) {
                    seen = true;
                    long propMs = ms(System.nanoTime() - start);
                    log.info("{} eventual propagated in {}ms (attempt #{}) write={}ms user={}", TAG, propMs, attempts + 1, ms(writeNs), r.getString("username"));
                    break;
                }
                attempts++;
                sleep(25 + attempts * 15);
            }
            if (!seen) {
                log.warn("{} eventual not visible on secondary within {} attempts", TAG, attempts);
            }
        } catch (Exception e) {
            log.warn("{} eventual error: {}", TAG, e.getMessage());
        }
    }
    private void causal() {
        log.info("{} causal begin", TAG);
        try (ClientSession session = mongoTemplate.getMongoDatabaseFactory().getSession(ClientSessionOptions.builder().causallyConsistent(true).build())) {
            String a = "c_a_" + System.currentTimeMillis();
            String b = "c_b_" + System.currentTimeMillis();
            mongoTemplate.insert(makeDoc(a, "cause", 100), COLL);
            UserProfile effect = makeDoc(b, "effect", 101);
            effect.setEmail("ref_" + a + "@example.com");
            mongoTemplate.insert(effect, COLL);
            verifyCausal(a, b);
        } catch (Exception e) {
            log.info("{} causal skipped or error: {}", TAG, e.getMessage());
        }
    }
    private void verifyCausal(String firstId, String secondId) {
        MongoCollection<Document> sec = mongoTemplate.getCollection(COLL).withReadPreference(ReadPreference.secondary());
        int rounds = 6;
        boolean ok = true;
        for (int i = 0; i < rounds; i++) {
            Document d1 = sec.find(Filters.eq("_id", firstId)).first();
            Document d2 = sec.find(Filters.eq("_id", secondId)).first();
            if (d2 != null && d1 == null) {
                ok = false;
                log.warn("{} causal violation at round {}", TAG, i + 1);
                break;
            }
            if (d1 != null && d2 != null) break;
            sleep(40);
        }
        if (ok) {
            log.info("{} causal ok", TAG);
        }
    }
    private CompletableFuture<Void> majorityWrites() {
        return CompletableFuture.runAsync(() -> {
            MongoCollection<Document> c = mongoTemplate.getCollection(COLL).withWriteConcern(WriteConcern.MAJORITY).withReadConcern(ReadConcern.MAJORITY);
            int ok = 0, fail = 0;
            for (int i = 0; i < 6; i++) {
                try {
                    c.insertOne(asDocument(makeDoc("maj_w_" + i, "maj", 1000 + i)));
                    ok++;
                } catch (Exception e) {
                    fail++;
                }
                sleep(180);
            }
            log.info("{} majority writes ok={} fail={}", TAG, ok, fail);
        });
    }
    private CompletableFuture<Void> majorityReads(String id) {
        return CompletableFuture.runAsync(() -> {
            MongoCollection<Document> c = mongoTemplate.getCollection(COLL).withWriteConcern(WriteConcern.MAJORITY).withReadConcern(ReadConcern.MAJORITY);
            int ok = 0, miss = 0;
            for (int i = 0; i < 6; i++) {
                try {
                    Document d = c.find(Filters.eq("_id", id)).first();
                    if (d != null) ok++; else miss++;
                } catch (Exception e) {
                    miss++;
                }
                sleep(180);
            }
            log.info("{} majority reads ok={} miss={}", TAG, ok, miss);
        });
    }
    private void prepare() {
        try {
            mongoTemplate.dropCollection(COLL);
        } catch (Exception ignore) {}
        mongoTemplate.createCollection(COLL);
        log.info("{} collection ready: {}", TAG, COLL);
    }
    private void cleanup() {
        try {
            mongoTemplate.dropCollection(COLL);
            log.info("{} collection dropped: {}", TAG, COLL);
        } catch (Exception e) {
            log.info("{} cleanup error: {}", TAG, e.getMessage());
        }
    }
    private void stepDownPrimary() {
        try {
            MongoDatabase admin = mongoTemplate.getMongoDatabaseFactory().getMongoDatabase("admin");
            Document cmd = new Document("replSetStepDown", 12).append("force", true);
            admin.runCommand(cmd);
            log.info("{} stepDown invoked", TAG);
        } catch (Exception e) {}
    }
    private static long ms(long nanos) {
        return nanos / 1_000_000L;
    }
    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
    private static UserProfile makeDoc(String id, String suffix, int idx) {
        return new UserProfile(id, "user_id_" + suffix, "username_" + suffix, "email_" + idx + "@example.com", Instant.now().toEpochMilli());
    }
    private static Document asDocument(UserProfile p) {
        Document d = new Document();
        d.put("_id", p.getId());
        d.put("userId", p.getUserId());
        d.put("username", p.getUsername());
        d.put("email", p.getEmail());
        d.put("last_login_time", p.getLastLoginTime());
        return d;
    }
}