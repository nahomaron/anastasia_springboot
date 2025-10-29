package com.anastasia.Anastasia_BackEnd.modules.accounting.util;

import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.ReportAccountLine;
import com.anastasia.Anastasia_BackEnd.modules.accounting.enums.ReportPeriod;
import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Account;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ReportHelper {

    private ReportHelper() {
    }

    public static ReportWindow resolveWindow(ReportPeriod period, LocalDate requestedStart, LocalDate endDate) {
        Objects.requireNonNull(period, "Report period is required");
        Objects.requireNonNull(endDate, "End date is required");

        LocalDate startDate;
        switch (period) {
            case MONTHLY -> startDate = endDate.withDayOfMonth(1);
            case QUARTERLY -> {
                int quarter = ((endDate.getMonthValue() - 1) / 3) * 3 + 1;
                startDate = LocalDate.of(endDate.getYear(), quarter, 1);
            }
            case HALF_YEAR -> {
                startDate = endDate.getMonthValue() <= 6
                        ? LocalDate.of(endDate.getYear(), 1, 1)
                        : LocalDate.of(endDate.getYear(), 7, 1);
            }
            case ANNUAL -> startDate = LocalDate.of(endDate.getYear(), 1, 1);
            case CUSTOM -> {
                if (requestedStart == null) {
                    throw new IllegalArgumentException("Custom reports require a start date");
                }
                startDate = requestedStart;
            }
            default -> startDate = requestedStart != null ? requestedStart : LocalDate.MIN;
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Report start date cannot be after end date");
        }

        return new ReportWindow(startDate, endDate);
    }

    public static List<ReportAccountLine> buildAccountLines(List<Account> accounts, Map<Long, BigDecimal> amounts) {
        if (accounts.isEmpty()) {
            return List.of();
        }

        Map<Long, Account> accountsById = accounts.stream()
                .collect(Collectors.toMap(Account::getId, a -> a));

        Map<Long, List<Account>> children = new HashMap<>();
        List<Account> roots = new ArrayList<>();

        for (Account account : accounts) {
            Long parentId = account.getParentAccount() != null ? account.getParentAccount().getId() : null;
            if (parentId == null || !accountsById.containsKey(parentId)) {
                roots.add(account);
            }
            children.computeIfAbsent(parentId, key -> new ArrayList<>()).add(account);
        }

        Comparator<Account> sorter = Comparator.comparing(Account::getCode, Comparator.nullsLast(String::compareToIgnoreCase));
        children.values().forEach(list -> list.sort(sorter));
        roots.sort(sorter);

        List<ReportAccountLine> lines = new ArrayList<>();
        for (Account root : roots) {
            ReportAccountLine line = buildLine(root, amounts, children);
            if (line != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static ReportAccountLine buildLine(Account account,
                                               Map<Long, BigDecimal> amounts,
                                               Map<Long, List<Account>> children) {

        List<ReportAccountLine> childLines = children.getOrDefault(account.getId(), List.of()).stream()
                .map(child -> buildLine(child, amounts, children))
                .filter(Objects::nonNull)
                .toList();

        BigDecimal amount = amounts.getOrDefault(account.getId(), BigDecimal.ZERO);
        BigDecimal total = amount;
        for (ReportAccountLine child : childLines) {
            total = total.add(child.getAmount());
        }

        if (childLines.isEmpty() && total.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        total = total.setScale(Math.max(total.scale(), 2), RoundingMode.HALF_UP);

        return ReportAccountLine.builder()
                .accountName(account.getName())
                .accountCode(account.getCode())
                .amount(total)
                .children(childLines)
                .build();
    }

    public record ReportWindow(LocalDate startDate, LocalDate endDate) {
    }
}
