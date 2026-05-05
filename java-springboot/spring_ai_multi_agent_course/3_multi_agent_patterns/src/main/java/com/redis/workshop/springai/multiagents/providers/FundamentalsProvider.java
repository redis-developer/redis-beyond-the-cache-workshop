package com.redis.workshop.springai.multiagents.providers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.redis.workshop.springai.multiagents.agent.fundamentalsagent.FundamentalsSnapshot;

import tools.jackson.databind.JsonNode;

@Component
public class FundamentalsProvider {

    private static final Set<String> ANNUAL_FORMS = Set.of("10-K", "10-K/A", "20-F", "20-F/A", "40-F", "40-F/A");

    private final RestClient restClient;
    private final String dataBaseUrl;
    private final String tickerFileUrl;

    public FundamentalsProvider(
            @Value("${stock-analysis.sec.data-base-url}") String dataBaseUrl,
            @Value("${stock-analysis.sec.ticker-file-url}") String tickerFileUrl,
            @Value("${stock-analysis.sec.user-agent}") String userAgent
    ) {
        this.restClient = RestClient.builder()
                .defaultHeader("User-Agent", userAgent)
                .defaultHeader("Accept-Encoding", "gzip, deflate")
                .build();
        this.dataBaseUrl = dataBaseUrl.replaceAll("/$", "");
        this.tickerFileUrl = tickerFileUrl;
    }

    public FundamentalsSnapshot fetchSnapshot(String ticker) {
        String symbol = normalize(ticker);
        SecCompanyReference company = resolveCompany(symbol);
        JsonNode facts = fetchCompanyFacts(company).path("facts");

        Fact revenue = latestAnnualFact(facts, List.of(
                "RevenueFromContractWithCustomerExcludingAssessedTax",
                "SalesRevenueNet",
                "Revenues"
        ), "USD");
        Fact previousRevenue = previousAnnualFact(facts, List.of(
                "RevenueFromContractWithCustomerExcludingAssessedTax",
                "SalesRevenueNet",
                "Revenues"
        ), "USD");
        Fact grossProfit = latestAnnualFact(facts, List.of("GrossProfit"), "USD");
        Fact operatingIncome = latestAnnualFact(facts, List.of("OperatingIncomeLoss"), "USD");
        Fact operatingCashFlow = latestAnnualFact(facts, List.of(
                "NetCashProvidedByUsedInOperatingActivities",
                "NetCashProvidedByUsedInOperatingActivitiesContinuingOperations"
        ), "USD");
        Fact capitalExpenditure = latestAnnualFact(facts, List.of("PaymentsToAcquirePropertyPlantAndEquipment"), "USD");

        return new FundamentalsSnapshot(
                company.ticker(),
                company.companyName(),
                growthPercent(revenue, previousRevenue),
                marginPercent(grossProfit, revenue),
                marginPercent(operatingIncome, revenue),
                freeCashFlowMarginPercent(operatingCashFlow, capitalExpenditure, revenue),
                summary(company, revenue, operatingIncome),
                asOf(revenue, grossProfit, operatingIncome, operatingCashFlow),
                "sec"
        );
    }

    private SecCompanyReference resolveCompany(String ticker) {
        JsonNode payload = restClient.get()
                .uri(tickerFileUrl)
                .retrieve()
                .body(JsonNode.class);

        if (payload == null || payload.size() == 0) {
            throw new IllegalStateException("SEC ticker lookup returned an empty response.");
        }

        return payload.valueStream()
                .map(this::companyReference)
                .filter(company -> ticker.equals(company.ticker()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("SEC ticker lookup returned no CIK for ticker " + ticker + "."));
    }

    private JsonNode fetchCompanyFacts(SecCompanyReference company) {
        JsonNode payload = restClient.get()
                .uri(dataBaseUrl + "/api/xbrl/companyfacts/CIK{cik}.json", company.paddedCik())
                .retrieve()
                .body(JsonNode.class);

        if (payload == null || payload.size() == 0) {
            throw new IllegalStateException("SEC company facts returned an empty response for " + company.ticker() + ".");
        }

        return payload;
    }

    private SecCompanyReference companyReference(JsonNode node) {
        int cik = node.path("cik_str").asInt();
        return new SecCompanyReference(
                node.path("ticker").asText("").toUpperCase(Locale.ROOT),
                node.path("title").asText(""),
                "%010d".formatted(cik)
        );
    }

    private Fact latestAnnualFact(JsonNode facts, List<String> concepts, String unitKey) {
        return annualFacts(facts, concepts, unitKey).stream()
                .max(Comparator.comparing(Fact::filed).thenComparing(Fact::end))
                .orElse(null);
    }

    private Fact previousAnnualFact(JsonNode facts, List<String> concepts, String unitKey) {
        List<Fact> values = annualFacts(facts, concepts, unitKey).stream()
                .sorted(Comparator.comparing(Fact::filed).thenComparing(Fact::end).reversed())
                .toList();

        return values.size() > 1 ? values.get(1) : null;
    }

    private List<Fact> annualFacts(JsonNode facts, List<String> concepts, String unitKey) {
        return concepts.stream()
                .flatMap(concept -> factNodes(facts, concept, unitKey))
                .map(this::toFact)
                .filter(Objects::nonNull)
                .filter(this::isAnnualDurationFact)
                .toList();
    }

    private Stream<JsonNode> factNodes(JsonNode facts, String concept, String unitKey) {
        JsonNode values = facts.path("us-gaap").path(concept).path("units").path(unitKey);
        return values.isArray() ? values.valueStream() : Stream.empty();
    }

    private Fact toFact(JsonNode node) {
        String value = optionalText(node, "val");
        String end = optionalText(node, "end");
        String filed = optionalText(node, "filed");

        if (value.isBlank() || end.isBlank() || filed.isBlank()) {
            return null;
        }

        String start = optionalText(node, "start");
        return new Fact(
                new BigDecimal(value),
                LocalDate.parse(end),
                LocalDate.parse(filed),
                start.isBlank() ? null : LocalDate.parse(start),
                optionalText(node, "form"),
                optionalText(node, "fp")
        );
    }

    private boolean isAnnualDurationFact(Fact fact) {
        if (!ANNUAL_FORMS.contains(fact.form())) {
            return false;
        }

        if (fact.start() == null) {
            return "FY".equalsIgnoreCase(fact.filingPeriod());
        }

        long days = fact.end().toEpochDay() - fact.start().toEpochDay();
        return "FY".equalsIgnoreCase(fact.filingPeriod()) || (days >= 300 && days <= 380);
    }

    private BigDecimal growthPercent(Fact latest, Fact previous) {
        if (latest == null || previous == null || previous.value().compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return latest.value()
                .subtract(previous.value())
                .divide(previous.value(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal marginPercent(Fact numerator, Fact revenue) {
        if (numerator == null || revenue == null || revenue.value().compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return numerator.value()
                .divide(revenue.value(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal freeCashFlowMarginPercent(Fact operatingCashFlow, Fact capitalExpenditure, Fact revenue) {
        if (operatingCashFlow == null || capitalExpenditure == null || revenue == null || revenue.value().compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        BigDecimal freeCashFlow = operatingCashFlow.value().subtract(capitalExpenditure.value().abs());
        return freeCashFlow
                .divide(revenue.value(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private OffsetDateTime asOf(Fact... facts) {
        return Arrays.stream(facts)
                .filter(Objects::nonNull)
                .map(Fact::filed)
                .max(Comparator.naturalOrder())
                .orElse(LocalDate.now(ZoneOffset.UTC))
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC);
    }

    private String summary(SecCompanyReference company, Fact revenue, Fact operatingIncome) {
        return "SEC company facts for %s. Latest annual revenue is %s and operating income is %s."
                .formatted(company.companyName(), valueText(revenue), valueText(operatingIncome));
    }

    private String valueText(Fact fact) {
        return fact == null ? "not available" : fact.value().setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String normalize(String ticker) {
        return ticker == null || ticker.isBlank()
                ? "RDIS"
                : ticker.trim().toUpperCase(Locale.ROOT);
    }

    private String optionalText(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field == null || field.isNull() || field.isMissingNode()
                ? ""
                : field.asText("");
    }

    private record SecCompanyReference(String ticker, String companyName, String paddedCik) {
    }

    private record Fact(
            BigDecimal value,
            LocalDate end,
            LocalDate filed,
            LocalDate start,
            String form,
            String filingPeriod
    ) {
    }
}
