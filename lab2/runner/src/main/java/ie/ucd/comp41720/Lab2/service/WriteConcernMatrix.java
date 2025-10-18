package ie.ucd.comp41720.Lab2.service;

import ie.ucd.comp41720.Lab2.domain.UserProfile;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class WriteConcernMatrix {
    private static final Logger log = LoggerFactory.getLogger(WriteConcernMatrix.class);
    private static final String COLL = "write_concern_perf_test";
    private static final int TESTS = 500;
    private static final int WARM = 5;
    private static final int BATCH = 400;
    private static final String TAG = "[LAB2-W]";
    private final MongoTemplate mongoTemplate;
    public WriteConcernMatrix(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }
    public void performWriteConcernExperiments() {
        log.info("{} start", TAG);
        prepare();
        warmup();
        runMatrix(WriteConcern.W3.withWTimeout(5, TimeUnit.SECONDS), "w:all");
        runMatrix(WriteConcern.ACKNOWLEDGED, "w:1");
        runMatrix(WriteConcern.MAJORITY, "w:majority");
        cleanup();
        log.info("{} done", TAG);
    }
    private void prepare() {
        try { mongoTemplate.dropCollection(COLL); } catch (Exception ignored) {}
        mongoTemplate.createCollection(COLL);
        log.info("{} collection ready", TAG);
    }
    private void cleanup() {
        try { mongoTemplate.dropCollection(COLL); } catch (Exception ignored) {}
        log.info("{} collection cleaned", TAG);
    }
    private void warmup() {
        log.info("{} warmup", TAG);
        for (int i = 0; i < WARM; i++) {
            runBatch(WriteConcern.ACKNOWLEDGED, "w1_warm_" + i, false);
            runBatch(WriteConcern.MAJORITY, "wmj_warm_" + i, false);
            runBatch(WriteConcern.W3.withWTimeout(5, TimeUnit.SECONDS), "wall_warm_" + i, false);
        }
        mongoTemplate.getCollection(COLL).deleteMany(new Document());
    }
    private void runMatrix(WriteConcern wc, String label) {
        log.info("{} {} begin tests={} batch={}", TAG, label, TESTS, BATCH);
        long total = 0L;
        List<Long> lat = new ArrayList<>(TESTS);
        int ok = 0;
        for (int i = 0; i < TESTS; i++) {
            long t = runBatch(wc, label + "_run_" + (i + 1), false);
            if (t > 0) {
                total += t;
                lat.add(t);
                ok++;
            }
            if (i + 1 < TESTS) try { Thread.sleep(60 + (i % 5) * 15); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        }
        if (ok == 0) {
            log.warn("{} {} no successful runs", TAG, label);
            return;
        }
        long avg = total / ok;
        long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
        for (long v : lat) { if (v < min) min = v; if (v > max) max = v; }
        double s = 0d;
        for (long v : lat) { double d = v - avg; s += d * d; }
        double std = Math.sqrt(s / ok);
        log.info("{} {} avg={}ms min={}ms max={}ms std={}.2ms ok={}/{} docs={}", TAG, label, avg, min, max, String.format("%.2f", std), ok, TESTS, ok * (long) BATCH);
        mongoTemplate.getCollection(COLL).deleteMany(new Document());
    }
    private long runBatch(WriteConcern wc, String id, boolean verbose) {
        try {
            MongoCollection<Document> c = mongoTemplate.getCollection(COLL).withWriteConcern(wc);
            List<Document> docs = new ArrayList<>(BATCH);
            long base = System.nanoTime();
            for (int i = 0; i < BATCH; i++) {
                UserProfile u = new UserProfile(
                        "id_" + id + "_" + base + "_" + i,
                        "uid_" + id + "_" + base + "_" + i,
                        "u_" + base + "_" + i,
                        "t_" + base + "_" + i + "@example.com",
                        Instant.now().toEpochMilli()
                );
                Document d = new Document();
                mongoTemplate.getConverter().write(u, d);
                docs.add(d);
            }
            long t0 = System.nanoTime();
            c.insertMany(docs);
            long ms = (System.nanoTime() - t0) / 1_000_000L;
            if (verbose) log.info("{} {} {}ms", TAG, id, ms);
            return ms;
        } catch (Exception e) {
            if (verbose) log.warn("{} {} error {}", TAG, id, e.getMessage());
            return -1L;
        }
    }
}