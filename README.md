# anastasia_springboot
Anastasia Church management api

## Accounting Module Enhancements

- Full double-entry posting for income, expenses, transfers, and journal entries with Kafka-driven payment capture integration.
- Reporting API now produces period-aware Balance Sheet and Income Statement outputs (monthly, quarterly, half-year, annual, and custom ranges) using ledger aggregates.
- Bank reconciliation service matches statement lines to ledger activity, highlights outstanding/unrecorded items, and computes adjusted balances.
- QuickBooks IIF import/export pipeline enabling round-trip data exchange with account auto-mapping and journal validation.
