package co.com.pragma.dynamodb;

import co.com.pragma.dynamodb.config.DynamoDbProperties;
import co.com.pragma.model.approvedloansreport.ApprovedLoansReport;
import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.state.State;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.math.BigDecimal;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

// The adapter against DynamoDB Local, Amazon's own emulator, in a container. No Spring context.
@Testcontainers(disabledWithoutDocker = true)
class DynamoDbApprovedLoansReportAdapterTest {

    private static final String TABLE = "approved-loans-report";

    @Container
    static final GenericContainer<?> DYNAMODB = new GenericContainer<>("amazon/dynamodb-local:2.6.1").withExposedPorts(8000);

    private DynamoDbAsyncClient client;
    private DynamoDbApprovedLoansReportAdapter adapter;

    @BeforeEach
    void createTheTable() {
        client = DynamoDbAsyncClient.builder()
                .endpointOverride(URI.create("http://" + DYNAMODB.getHost() + ":" + DYNAMODB.getFirstMappedPort()))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build();
        // The same table deployment/localstack/init.sh creates.
        client.createTable(t -> t.tableName(TABLE)
                .attributeDefinitions(a -> a.attributeName("pk").attributeType(ScalarAttributeType.S))
                .keySchema(k -> k.attributeName("pk").keyType(KeyType.HASH))
                .billingMode(BillingMode.PAY_PER_REQUEST)).join();
        adapter = new DynamoDbApprovedLoansReportAdapter(new DynamoDbProperties("us-east-1", null, TABLE), client);
    }

    @AfterEach
    void dropTheTable() {
        client.deleteTable(t -> t.tableName(TABLE)).join();
        client.close();
    }

    private Loan approved(long id, String amount) {
        return Loan.builder().id(id).amount(new BigDecimal(amount)).state(State.APPROVED).build();
    }

    @Test
    void beforeAnyApprovalTheReportIsZero() {
        assertThat(adapter.get().block()).isEqualTo(new ApprovedLoansReport(0, BigDecimal.ZERO));
    }

    @Test
    void approvedLoansAreCountedAndTheirAmountsAdded() {
        adapter.add(approved(1, "30000000.00")).block();
        adapter.add(approved(2, "2500000.50")).block();

        ApprovedLoansReport report = adapter.get().block();
        assertThat(report.count()).isEqualTo(2);
        assertThat(report.totalAmount()).isEqualByComparingTo("32500000.50");
    }

    @Test
    void theSameLoanDeliveredTwiceIsCountedOnce() {
        adapter.add(approved(1, "30000000.00")).block();
        adapter.add(approved(1, "30000000.00")).block();

        ApprovedLoansReport report = adapter.get().block();
        assertThat(report.count()).isEqualTo(1);
        assertThat(report.totalAmount()).isEqualByComparingTo("30000000");
    }
}
