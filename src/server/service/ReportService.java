package server.service;

import common.model.Ticket;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import server.util.ServerLogger;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportService {
    private static final String REPORT_TEMPLATE_PATH = "/server/reports/ticket_report.jrxml";
    private final ServerLogger logger = ServerLogger.getInstance();

    public byte[] generateTicketReport(List<Ticket> tickets) throws JRException {
        logger.info("Iniciando generación de reporte para " + tickets.size() + " tickets");

        // Load report template
        InputStream reportStream = getClass().getResourceAsStream(REPORT_TEMPLATE_PATH);
        if (reportStream == null) {
            String error = "No se encontró la plantilla del reporte: " + REPORT_TEMPLATE_PATH;
            logger.error(error, null);
            throw new JRException(error);
        }

        try {
            // Compile the report
            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

            // Prepare data source
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(tickets);

            // Parameters (none for now, but map is required)
            Map<String, Object> parameters = new HashMap<>();

            // Fill the report
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

            // Export to PDF
            return JasperExportManager.exportReportToPdf(jasperPrint);

        } catch (JRException e) {
            logger.error("Error generando el reporte PDF", e);
            throw e;
        }
    }
}
