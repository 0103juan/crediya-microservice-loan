package co.com.pragma.dynamodb;

import co.com.pragma.dynamodb.config.DynamoDbProperties;
import co.com.pragma.model.approvedloansreport.ApprovedLoansReport;
import co.com.pragma.model.approvedloansreport.gateways.ApprovedLoansReportRepository;
import co.com.pragma.model.loan.Loan;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;
import software.amazon.awssdk.services.dynamodb.model.Update;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Una tabla, dos tipos de ítem: {@code LOAN#<id>} marca cada préstamo ya contado y {@code REPORT} lleva los totales.
 */
@Repository
@Log4j2
@RequiredArgsConstructor
public class DynamoDbApprovedLoansReportAdapter implements ApprovedLoansReportRepository {
    private static final String KEY = "pk";
    private static final String COUNT = "loanCount";
    private static final String TOTAL = "totalAmount";
    private static final Map<String, AttributeValue> REPORT_KEY = Map.of(KEY, AttributeValue.fromS("REPORT"));

    private final DynamoDbProperties properties;
    private final DynamoDbAsyncClient client;

    @Override
    public Mono<Void> add(Loan approvedLoan) {
        String amount = approvedLoan.getAmount().toPlainString();

        // Las dos escrituras van en una transacción: si el préstamo ya tiene su marca, la condición falla
        // y los totales no se tocan. Así un evento entregado dos veces por Kafka se cuenta una sola vez.
        TransactWriteItem markLoan = TransactWriteItem.builder().put(Put.builder()
                .tableName(properties.table())
                .item(Map.of(KEY, AttributeValue.fromS("LOAN#" + approvedLoan.getId()), "amount", AttributeValue.fromN(amount)))
                .conditionExpression("attribute_not_exists(" + KEY + ")")
                .build()).build();
        TransactWriteItem addToTotals = TransactWriteItem.builder().update(Update.builder()
                .tableName(properties.table())
                .key(REPORT_KEY)
                .updateExpression("ADD " + COUNT + " :one, " + TOTAL + " :amount")
                .expressionAttributeValues(Map.of(":one", AttributeValue.fromN("1"), ":amount", AttributeValue.fromN(amount)))
                .build()).build();

        return Mono.fromFuture(() -> client.transactWriteItems(r -> r.transactItems(markLoan, addToTotals)))
                .then()
                .onErrorResume(TransactionCanceledException.class, e -> {
                    if (!"ConditionalCheckFailed".equals(e.cancellationReasons().getFirst().code())) {
                        return Mono.error(e);
                    }
                    log.info("El préstamo {} ya estaba en el reporte; no se cuenta de nuevo.", approvedLoan.getId());
                    return Mono.empty();
                });
    }

    @Override
    public Mono<ApprovedLoansReport> get() {
        return Mono.fromFuture(() -> client.getItem(r -> r.tableName(properties.table()).key(REPORT_KEY).consistentRead(true)))
                .map(response -> response.hasItem()
                        ? new ApprovedLoansReport(Long.parseLong(response.item().get(COUNT).n()),
                                new BigDecimal(response.item().get(TOTAL).n()))
                        : new ApprovedLoansReport(0, BigDecimal.ZERO));
    }
}
