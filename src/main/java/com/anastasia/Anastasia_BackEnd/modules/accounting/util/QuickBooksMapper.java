package com.anastasia.Anastasia_BackEnd.modules.accounting.util;

import com.anastasia.Anastasia_BackEnd.modules.accounting.model.LedgerEntry;
import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Transaction;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class QuickBooksMapper {

    private static final DateTimeFormatter IIF_DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final String TRNSTYPE = "GENERAL JOURNAL";

    private QuickBooksMapper() {
    }

    public static void writeTransactionsAsIif(List<Transaction> transactions, Writer writer) throws IOException {
        writer.write(String.format("!TRNS\tTRNSID\tTRNSTYPE\tDATE\tACCNT\tAMOUNT\tMEMO%n"));
        writer.write(String.format("!SPL\tSPLID\tTRNSTYPE\tDATE\tACCNT\tAMOUNT\tMEMO%n"));
        writer.write(String.format("!ENDTRNS%n"));

        for (Transaction transaction : transactions) {
            List<LedgerEntry> entries = new ArrayList<>(transaction.getLedgerEntries());
            if (entries.isEmpty()) {
                continue;
            }

            LedgerEntry primary = choosePrimary(entries);
            List<LedgerEntry> remainder = entries.stream()
                    .filter(entry -> entry != primary)
                    .collect(Collectors.toList());

            writer.write(String.format("TRNS\t\t%s\t%s\t%s\t%s\t%s%n",
                    TRNSTYPE,
                    IIF_DATE.format(transaction.getDate()),
                    sanitizeAccount(primary),
                    qbAmount(primary),
                    sanitizeMemo(transaction.getDescription())));

            for (LedgerEntry entry : remainder) {
                writer.write(String.format("SPL\t\t%s\t%s\t%s\t%s\t%s%n",
                        TRNSTYPE,
                        IIF_DATE.format(transaction.getDate()),
                        sanitizeAccount(entry),
                        qbAmount(entry),
                        sanitizeMemo(transaction.getDescription())));
            }

            writer.write(String.format("ENDTRNS%n"));
        }
    }

    public static List<JournalEntry> readJournalEntries(Reader reader) throws IOException {
        List<JournalEntry> entries = new ArrayList<>();
        try (BufferedReader buffered = new BufferedReader(reader)) {
            String line;
            JournalEntry current = null;
            while ((line = buffered.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                if (line.startsWith("!")) {
                    continue; // header lines
                }

                String[] tokens = line.split("\t");
                String recordType = tokens[0];

                if ("TRNS".equalsIgnoreCase(recordType)) {
                    current = new JournalEntry();
                    current.date = LocalDate.parse(tokens[3], IIF_DATE);
                    current.memo = tokens.length > 6 ? tokens[6] : "";
                    current.lines.add(parseLine(tokens));
                } else if ("SPL".equalsIgnoreCase(recordType) && current != null) {
                    current.lines.add(parseLine(tokens));
                } else if ("ENDTRNS".equalsIgnoreCase(recordType)) {
                    if (current != null) {
                        entries.add(current);
                        current = null;
                    }
                }
            }
        }
        return entries;
    }

    private static JournalEntry.Line parseLine(String[] tokens) {
        String accountName = tokens.length > 4 ? tokens[4] : "";
        String amountToken = tokens.length > 5 ? tokens[5].trim() : "0";
        BigDecimal amount = amountToken.isEmpty() ? BigDecimal.ZERO : new BigDecimal(amountToken).setScale(2, RoundingMode.HALF_UP);
        return new JournalEntry.Line(accountName, amount);
    }

    private static LedgerEntry choosePrimary(List<LedgerEntry> entries) {
        return entries.stream()
                .filter(entry -> entry.getDebit().compareTo(entry.getCredit()) > 0)
                .findFirst()
                .orElse(entries.get(0));
    }

    private static String sanitizeAccount(LedgerEntry entry) {
        return entry.getAccount().getName();
    }

    private static String sanitizeMemo(String memo) {
        if (memo == null) {
            return "";
        }
        return memo.replace('\t', ' ');
    }

    private static BigDecimal qbAmount(LedgerEntry entry) {
        return entry.getDebit().subtract(entry.getCredit()).setScale(2, RoundingMode.HALF_UP);
    }

    public static class JournalEntry {
        private LocalDate date;
        private String memo;
        private final List<Line> lines = new ArrayList<>();

        public LocalDate getDate() {
            return date;
        }

        public String getMemo() {
            return memo;
        }

        public List<Line> getLines() {
            return List.copyOf(lines);
        }

        public static class Line {
            private final String accountName;
            private final BigDecimal amount;

            public Line(String accountName, BigDecimal amount) {
                this.accountName = accountName;
                this.amount = amount;
            }

            public String getAccountName() {
                return accountName;
            }

            public BigDecimal getAmount() {
                return amount;
            }
        }
    }
}
