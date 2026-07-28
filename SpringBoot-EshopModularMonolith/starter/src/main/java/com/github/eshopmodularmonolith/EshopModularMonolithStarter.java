package com.github.eshopmodularmonolith;


import ch.qos.logback.core.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration;
import org.springframework.context.event.EventListener;
import org.springframework.data.util.Pair;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//Exclude all configs as each module will define its own.
@SpringBootApplication(exclude = {
        DataMongoAutoConfiguration.class,
        DataMongoRepositoriesAutoConfiguration.class,
        DataJpaRepositoriesAutoConfiguration.class
})
public class EshopModularMonolithStarter {

    private static final Logger log = LoggerFactory.getLogger(EshopModularMonolithStarter.class);

    private final String name;

    public EshopModularMonolithStarter(@Value("${spring.application.name} ") String name) {
        this.name = name;
    }

    static void main(String[] args) {
        SpringApplication.run(EshopModularMonolithStarter.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("{} application started", name);
    }


    class TransactionLineParser {

        List<String> lines;

        public TransactionLineParser(List<String> lines){
            if (lines == null){
                throw new IllegalArgumentException("Lines can not be null");
            }
            this.lines = lines;
        }

        public Map<String, Long> parse(Currency currency){
            return lines.stream()
                    .flatMap(this::parseLine)
                    .filter(l -> Currency.CHF.equals(l.currency()))
                    .collect(Collectors.toMap(
                            l -> l.accountId,
                            l -> l.amount_cents
                    ));
        }

        private Stream<Line> parseLine(String line){
            if(line == null){
                throw new NullPointerException("Line is null");
            }
            var values = line.split(",");

             return Line.from(values).stream();
        }

    }

    record Line(LocalDateTime timestamp, String accountId, Long amount_cents, Currency currency){

        static int LINE_SIZE = 4;
        private static int TIMESTAMP = 0;
        private static int ACCOUNT_ID = 1;
        private static int AMOUNT_CENTS = 2;
        private static int CURRENCY = 3;

        public static Optional<Line> from(String[] values){
            if (values.length != Line.LINE_SIZE) {
                return Optional.empty();
            }

            try {
                return Optional.of(new Line(
                        parseTimestamp(values),
                        parseAccountId(values),
                        parseAmountCents(values),
                        parseCurrency(values)));
            } catch (Exception e){
              return Optional.empty();
            }

        }

        private static LocalDateTime parseTimestamp(String[] values) throws Exception{
            try {
                return LocalDateTime.parse(values[TIMESTAMP], DateTimeFormatter.ISO_DATE_TIME);
            }catch (Exception e){
                throw new LineParsingError(e);
            }
        }

        private static String parseAccountId(String[] values) throws Exception{
                var accountId = values[ACCOUNT_ID];
                if(StringUtil.isNullOrEmpty(accountId)){
                    throw new LineParsingError("account is null or missing");
                }
                return accountId;
        }

        private static Long parseAmountCents(String[] values) throws Exception {
                var amountCents = values[AMOUNT_CENTS];
                if(StringUtil.isNullOrEmpty(amountCents)){
                    throw new LineParsingError("amount is null or missing");
                }

                return Long.parseLong(amountCents);
        }

        private static Currency parseCurrency(String[] values) throws Exception{
                var currency = values[CURRENCY];
                if(StringUtil.isNullOrEmpty(currency)){
                    throw new LineParsingError("currency is null or missing");
                }

                return Currency.valueOf(currency);
        }

    }

    static class LineParsingError extends Exception {
        public LineParsingError(String message) {
            super(message);
        }

        public LineParsingError(Throwable cause) {
            super(cause);
        }
    }
    enum Currency{
        CHF,
        EUR
    }


    record PaymentEvent(String id, String account, long amount_cents, long ts){}
    class DuplicatePaymentDetector {

        public List<Pair<String, String>> findDuplicates(List<PaymentEvent> events){
            if(CollectionUtils.isEmpty(events)) return List.of();
            var timeSorted = events.stream().sorted(Comparator.comparingLong(PaymentEvent::ts)).toList();

            List<Pair<String, String>> duplications = new ArrayList<>();

            for(int i = 0; i < timeSorted.size() - 1; i++){
                var current = timeSorted.get(i);
                for(int j = i + 1; j < timeSorted.size(); j++){
                    var next = timeSorted.get(j);
                    if(Objects.equals(current.account, next.account) && Math.abs(current.ts - next.ts) <= 5) {
                        duplications.add(Pair.of(current.id, next.id));
                    }
                }
            }
            return duplications;
        }

    }

}
