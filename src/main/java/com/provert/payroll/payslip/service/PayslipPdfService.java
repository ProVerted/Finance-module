package com.provert.payroll.payslip.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.provert.payroll.payslip.entity.Payslip;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Month;
import java.time.format.DateTimeFormatter;

@Service
public class PayslipPdfService {

    public byte[] generatePayslipPdf(Payslip payslip) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(30, 41, 59));
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 11, new Color(100, 116, 139));
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(30, 41, 59));
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font netPayFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(22, 101, 52));

            // Header
            Paragraph title = new Paragraph("PROVERT PAYROLL SERVICES", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("MONTHLY SALARY SLIP — " + Month.of(payslip.getMonth()).name() + " " + payslip.getYear(), subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(15);
            document.add(subtitle);

            // Employee Information Table
            PdfPTable infoTable = new PdfPTable(4);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(15);
            infoTable.setWidths(new float[]{20, 30, 20, 30});

            addInfoCell(infoTable, "Employee Code:", boldFont);
            addInfoCell(infoTable, payslip.getEmployee().getEmployeeCode(), regularFont);
            addInfoCell(infoTable, "Full Name:", boldFont);
            addInfoCell(infoTable, payslip.getEmployee().getFullName(), regularFont);

            addInfoCell(infoTable, "Department:", boldFont);
            addInfoCell(infoTable, payslip.getEmployee().getDepartment().getName(), regularFont);
            addInfoCell(infoTable, "Designation:", boldFont);
            addInfoCell(infoTable, payslip.getEmployee().getDesignation() != null ? payslip.getEmployee().getDesignation() : "N/A", regularFont);

            addInfoCell(infoTable, "Pay Period:", boldFont);
            addInfoCell(infoTable, Month.of(payslip.getMonth()).name() + " " + payslip.getYear(), regularFont);
            addInfoCell(infoTable, "Status:", boldFont);
            addInfoCell(infoTable, Boolean.TRUE.equals(payslip.getFinalized()) ? "FINALIZED (LOCKED)" : "PROVISIONAL", regularFont);

            document.add(infoTable);

            // Earnings & Deductions Tables (side by side in 2 columns)
            PdfPTable breakdownTable = new PdfPTable(2);
            breakdownTable.setWidthPercentage(100);
            breakdownTable.setSpacingAfter(15);
            breakdownTable.setWidths(new float[]{50, 50});

            // Earnings Column
            PdfPTable earningsTable = new PdfPTable(2);
            earningsTable.setWidthPercentage(100);
            earningsTable.setWidths(new float[]{65, 35});

            PdfPCell earningsHeader = new PdfPCell(new Phrase("EARNINGS", sectionFont));
            earningsHeader.setColspan(2);
            earningsHeader.setBackgroundColor(new Color(241, 245, 249));
            earningsHeader.setPadding(6);
            earningsTable.addCell(earningsHeader);

            addTableRow(earningsTable, "Basic Salary", String.format("Rs. %,.2f", payslip.getBasicSalary()), regularFont);
            addTableRow(earningsTable, "Overtime (OT) Pay", String.format("Rs. %,.2f", payslip.getOtAmount()), regularFont);
            addTableRow(earningsTable, "Gross Earnings", String.format("Rs. %,.2f", payslip.getGrossPay()), boldFont);

            // Deductions Column
            PdfPTable deductionsTable = new PdfPTable(2);
            deductionsTable.setWidthPercentage(100);
            deductionsTable.setWidths(new float[]{65, 35});

            PdfPCell deductionsHeader = new PdfPCell(new Phrase("DEDUCTIONS", sectionFont));
            deductionsHeader.setColspan(2);
            deductionsHeader.setBackgroundColor(new Color(241, 245, 249));
            deductionsHeader.setPadding(6);
            deductionsTable.addCell(deductionsHeader);

            addTableRow(deductionsTable, "Employee EPF (8%)", String.format("Rs. %,.2f", payslip.getEmployeeEpf()), regularFont);
            addTableRow(deductionsTable, "Other Deductions", String.format("Rs. %,.2f", payslip.getDeductions()), regularFont);
            addTableRow(deductionsTable, "Total Deductions", String.format("Rs. %,.2f", payslip.getTotalDeductions()), boldFont);

            // Put both into parent table
            PdfPCell c1 = new PdfPCell(earningsTable);
            c1.setBorder(Rectangle.NO_BORDER);
            c1.setPaddingRight(5);
            breakdownTable.addCell(c1);

            PdfPCell c2 = new PdfPCell(deductionsTable);
            c2.setBorder(Rectangle.NO_BORDER);
            c2.setPaddingLeft(5);
            breakdownTable.addCell(c2);

            document.add(breakdownTable);

            // Employer Statutory Contributions Table
            PdfPTable employerTable = new PdfPTable(2);
            employerTable.setWidthPercentage(100);
            employerTable.setSpacingAfter(15);
            employerTable.setWidths(new float[]{65, 35});

            PdfPCell empHeader = new PdfPCell(new Phrase("EMPLOYER STATUTORY CONTRIBUTIONS (NOT DEDUCTED FROM SALARY)", sectionFont));
            empHeader.setColspan(2);
            empHeader.setBackgroundColor(new Color(241, 245, 249));
            empHeader.setPadding(6);
            employerTable.addCell(empHeader);

            addTableRow(employerTable, "Employer EPF Contribution (12%)", String.format("Rs. %,.2f", payslip.getEmployerEpf()), regularFont);
            addTableRow(employerTable, "Employer ETF Contribution (3%)", String.format("Rs. %,.2f", payslip.getEmployerEtf()), regularFont);

            document.add(employerTable);

            // Net Payable Highlight Box
            PdfPTable netPayTable = new PdfPTable(1);
            netPayTable.setWidthPercentage(100);
            netPayTable.setSpacingAfter(20);

            PdfPCell netCell = new PdfPCell();
            netCell.setBackgroundColor(new Color(240, 253, 244));
            netCell.setPadding(12);
            netCell.setBorderColor(new Color(187, 247, 208));

            Paragraph netText = new Paragraph("NET PAYABLE AMOUNT: " + String.format("Rs. %,.2f", payslip.getNetPay()), netPayFont);
            netText.setAlignment(Element.ALIGN_CENTER);
            netCell.addElement(netText);
            netPayTable.addCell(netCell);

            document.add(netPayTable);

            // Footer Timestamp
            Paragraph footer = new Paragraph(
                    "Generated at: " + payslip.getGeneratedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
                    " | This is a computer-generated payslip and requires no physical signature.",
                    subtitleFont
            );
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Error generating payslip PDF: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    private void addInfoCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(4);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    private void addTableRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setPadding(5);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }
}
