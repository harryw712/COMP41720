package ie.ucd.comp41720.Lab2;

import ie.ucd.comp41720.Lab2.service.ConsistencySweep;
import ie.ucd.comp41720.Lab2.service.ReplicationProbe;
import ie.ucd.comp41720.Lab2.service.UserProfileService;
import ie.ucd.comp41720.Lab2.service.WriteConcernMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.InetAddress;
import java.time.ZoneId;

@SpringBootApplication(scanBasePackages = "ie.ucd.comp41720.Lab2")
public class LabHarness implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LabHarness.class);
    private static final String TAG = "[LAB2]";

    private final UserProfileService userProfileService;
    private final WriteConcernMatrix writeConcernMatrix;
    private final ReplicationProbe replicationProbe;
    private final ConsistencySweep consistencySweep;

    public LabHarness(UserProfileService userProfileService,
                      WriteConcernMatrix writeConcernMatrix,
                      ReplicationProbe replicationProbe,
                      ConsistencySweep consistencySweep) {
        this.userProfileService = userProfileService;
        this.writeConcernMatrix = writeConcernMatrix;
        this.replicationProbe = replicationProbe;
        this.consistencySweep = consistencySweep;
    }

    public static void main(String[] args) {
        SpringApplication.run(LabHarness.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("{} boot tz={}, host={}", TAG, ZoneId.systemDefault(), InetAddress.getLocalHost().getHostName());

        userProfileService.checkReplicaSetStatus();

        String serviceToRun = resolveService(args);
        switch (serviceToRun) {
            case "write-concern":
                log.info("{} run=write-concern", TAG);
                writeConcernMatrix.performWriteConcernExperiments();
                break;
            case "replication":
                log.info("{} run=replication", TAG);
                replicationProbe.performReplicationExperiments();
                break;
            case "consistency":
                log.info("{} run=consistency", TAG);
                consistencySweep.performConsistencyExperiments();
                break;
            default:
                log.warn("{} no valid service specified. Use: write-concern | replication | consistency", TAG);
        }
    }

    private String resolveService(String... args) {
        for (String arg : args) {
            if (arg != null && arg.startsWith("--service=")) {
                return arg.substring("--service=".length()).trim();
            }
        }
        String env = System.getenv("SERVICE");
        return env == null ? "" : env.trim();
    }
}