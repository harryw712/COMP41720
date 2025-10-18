package ie.ucd.comp41720.Lab2.service;

import ie.ucd.comp41720.Lab2.domain.UserProfile;
import com.mongodb.ReadPreference;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class ReplicationProbe {
    private static final Logger log = LoggerFactory.getLogger(ReplicationProbe.class);
    private final MongoTemplate mongoTemplate;
    private static final String COLL = "replication_model_test";
    private static final int COUNT = 50;
    private static final String TAG = "[LAB2-R]";
    public ReplicationProbe(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }
    public void performReplicationExperiments() {
        log.info("{} start", TAG);
        prepare();
        writePropagation();
        readPreferences();
        failover();
        cleanup();
        log.info("{} done", TAG);
    }
    private void writePropagation() {
        log.info("{} write propagation begin", TAG);
        try {
            List<UserProfile> written = new ArrayList<>();
            long start = System.nanoTime();
            for (int i = 0; i < COUNT; i++) {
                UserProfile d = makeDoc("primary_write_" + i, i);
                mongoTemplate.insert(d, COLL);
                written.add(d);
            }
            log.info("{} {} docs inserted in {}ms", TAG, COUNT, ms(System.nanoTime() - start));
            verifyPropagation(written);
        } catch (Exception e) {
            log.warn("{} write propagation error: {}", TAG, e.getMessage());
        }
    }
    private void readPreferences() {
        log.info("{} read preference begin", TAG);
        UserProfile d = makeDoc("read_pref_test", 999);
        mongoTemplate.insert(d, COLL);
        testPref("primary", ReadPreference.primary(), d.getId());
        testPref("secondary", ReadPreference.secondary(), d.getId());
        testPref("primaryPreferred", ReadPreference.primaryPreferred(), d.getId());
    }
    private void failover() {
        log.info("{} failover begin", TAG);
        try {
            Document init = replStatus();
            String oldPrimary = currentPrimary(init);
            log.info("{} current primary {}", TAG, oldPrimary);
            if ("UNKNOWN".equals(oldPrimary)) return;
            CompletableFuture<Void> ops = concurrentOps();
            log.info("{} stepDown trigger", TAG);
            stepDown();
            monitorFailover(oldPrimary);
            try {
                ops.get(20, TimeUnit.SECONDS);
            } catch (Exception ignored) {}
            Document now = replStatus();
            String newP = currentPrimary(now);
            log.info("{} new primary {}", TAG, newP);
            if (!newP.equals(oldPrimary)) log.info("{} failover success", TAG);
            else log.warn("{} failover no change", TAG);
        } catch (Exception e) {
            log.warn("{} failover error: {}", TAG, e.getMessage());
        }
    }
    private void stepDown() {
        try {
            MongoDatabase admin = mongoTemplate.getMongoDatabaseFactory().getMongoDatabase("admin");
            Document cmd = new Document("replSetStepDown", 30).append("force", true);
            admin.runCommand(cmd);
            log.info("{} stepDown sent", TAG);
        } catch (Exception ignored) {}
    }
    private void monitorFailover(String oldP) {
        long start = System.nanoTime();
        long timeout = TimeUnit.SECONDS.toNanos(25);
        boolean ok = false;
        while (System.nanoTime() - start < timeout && !ok) {
            try {
                Document s = replStatus();
                String cur = currentPrimary(s);
                if (!"UNKNOWN".equals(cur) && !cur.equals(oldP)) {
                    ok = true;
                    log.info("{} new primary after {}ms", TAG, ms(System.nanoTime() - start));
                }
                Thread.sleep(1000);
            } catch (Exception ignored) {}
        }
        if (!ok) log.warn("{} no new primary within timeout", TAG);
    }
    private CompletableFuture<Void> concurrentOps() {
        return CompletableFuture.runAsync(() -> {
            int ok = 0, fail = 0;
            for (int i = 0; i < 20; i++) {
                try {
                    UserProfile d = makeDoc("failover_write_" + i, 2000 + i);
                    mongoTemplate.insert(d, COLL);
                    ok++;
                    Thread.sleep(200);
                } catch (Exception e) {
                    fail++;
                }
            }
            log.info("{} concurrent ops ok={} fail={}", TAG, ok, fail);
        });
    }
    private void verifyPropagation(List<UserProfile> expected) {
        MongoCollection<Document> sec = mongoTemplate.getCollection(COLL).withReadPreference(ReadPreference.secondary());
        int ok = 0;
        for (UserProfile e : expected) {
            try {
                Document d = sec.find(Filters.eq("_id", e.getId())).first();
                if (d != null) ok++;
            } catch (Exception ignored) {}
        }
        double rate = expected.isEmpty() ? 0 : ok * 100.0 / expected.size();
        log.info("{} propagation ok={}/{} ({:.2f}%)", TAG, ok, expected.size(), rate);
    }
    private void testPref(String name, ReadPreference pref, String id) {
        try {
            MongoCollection<Document> c = mongoTemplate.getCollection(COLL).withReadPreference(pref);
            long t0 = System.nanoTime();
            Document r = c.find(Filters.eq("_id", id)).first();
            long latency = ms(System.nanoTime() - t0);
            if (r != null) log.info("{} pref {} ok latency={}ms", TAG, name, latency);
            else log.info("{} pref {} miss", TAG, name);
        } catch (Exception e) {
            log.warn("{} pref {} error: {}", TAG, name, e.getMessage());
        }
    }
    private Document replStatus() {
        try {
            MongoDatabase admin = mongoTemplate.getMongoDatabaseFactory().getMongoDatabase("admin");
            return admin.runCommand(new Document("replSetGetStatus", 1));
        } catch (Exception e) {
            return new Document("members", new ArrayList<>());
        }
    }
    private String currentPrimary(Document st) {
        try {
            List<Document> members = (List<Document>) st.get("members");
            for (Document m : members) {
                if (m.getInteger("state") == 1) return m.getString("name");
            }
        } catch (Exception ignored) {}
        return "UNKNOWN";
    }
    private void prepare() {
        try {
            mongoTemplate.dropCollection(COLL);
        } catch (Exception ignored) {}
        mongoTemplate.createCollection(COLL);
        log.info("{} collection ready", TAG);
    }
    private void cleanup() {
        try {
            mongoTemplate.dropCollection(COLL);
            log.info("{} collection cleaned", TAG);
        } catch (Exception ignored) {}
    }
    private static long ms(long nanos) {
        return nanos / 1_000_000L;
    }
    private static UserProfile makeDoc(String suffix, int idx) {
        return new UserProfile("test_id_" + suffix + "_" + System.currentTimeMillis(), "user_id_" + suffix, "username_" + suffix, "email_" + idx + "@example.com", Instant.now().toEpochMilli());
    }
}