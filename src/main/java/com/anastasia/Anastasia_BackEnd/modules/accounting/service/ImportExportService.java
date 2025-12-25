package com.anastasia.Anastasia_BackEnd.modules.accounting.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;

public interface ImportExportService {

    /**
     * Exports accounting data in a QuickBooks compatible format (e.g., IIF or CSV).
     *
     * @param tenantId The tenant's ID.
     * @param startDate The start date for the export.
     * @param endDate The end date for the export.
     * @param outputStream The stream to write the file content to.
     */
    void exportToQuickBooks(java.util.UUID tenantId, LocalDate startDate, LocalDate endDate, OutputStream outputStream);

    /**
     * Imports data from a QuickBooks file.
     *
     * @param tenantId The tenant's ID.
     * @param inputStream The stream containing the file content.
     */
    void importFromQuickBooks(java.util.UUID tenantId, InputStream inputStream);
}
