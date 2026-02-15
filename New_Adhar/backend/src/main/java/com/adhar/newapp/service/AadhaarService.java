package com.adhar.newapp.service;

import com.adhar.newapp.model.User;
import com.adhar.newapp.repository.UserRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

@Service
public class AadhaarService {

    @Autowired
    private UserRepository userRepository;

    private final Path cardStorageLocation = Paths.get("aadhaar_cards");

    public AadhaarService() {
        try {
            Files.createDirectories(cardStorageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize card storage!", e);
        }
    }

    public void generateAadhaar(User user) {
        if (user.getAadhaarNumber() != null && !user.getAadhaarNumber().isEmpty()) {
            return; // Already generated
        }

        // 1. Generate 12-digit Aadhaar Number
        String aadhaarNumber = generateUniqueAadhaarNumber();
        user.setAadhaarNumber(aadhaarNumber);

        // 2. Generate PDF
        String filename = user.getId() + "_newaadhaar.pdf";
        Path pdfPath = cardStorageLocation.resolve(filename);

        try {
            createPdf(user, pdfPath.toString());
            user.setAadhaarPdfPath(filename);
            userRepository.save(user); // Save both number and PDF path
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private String generateUniqueAadhaarNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) { // 3 blocks of 4 digits
            sb.append(String.format("%04d", random.nextInt(10000)));
            if (i < 2)
                sb.append(" ");
        }
        return sb.toString();
    }

    private void createPdf(User user, String dest) throws IOException {
        PdfWriter writer = new PdfWriter(dest);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Main Card Container (Bordered)
        Table mainTable = new Table(UnitValue.createPercentArray(new float[] { 1 }));
        mainTable.setWidth(UnitValue.createPercentValue(100));

        // Header (Orange/White/Green bar simulation)
        Cell headerCell = new Cell().add(new Paragraph("GOVERNMENT OF INDIA")
                .setFontSize(14).setBold().setFontColor(ColorConstants.BLACK)
                .setTextAlignment(TextAlignment.CENTER));
        headerCell.setBorder(null);
        headerCell.setBackgroundColor(new DeviceRgb(255, 153, 51)); // Saffronish
        headerCell.setPadding(10);
        mainTable.addCell(headerCell);

        Cell subHeaderCell = new Cell().add(new Paragraph("Aadhaar - Common Man's Right")
                .setFontSize(10).setFontColor(ColorConstants.BLACK)); // Reset color
        subHeaderCell.setBorder(null);
        subHeaderCell.setTextAlignment(TextAlignment.CENTER);
        subHeaderCell.setPadding(5);
        mainTable.addCell(subHeaderCell);

        // Content Row (Photo + Details + QR)
        Table contentTable = new Table(UnitValue.createPercentArray(new float[] { 25, 50, 25 }));
        contentTable.setWidth(UnitValue.createPercentValue(100));

        // 1. Photo Section
        Cell photoCell = new Cell();
        photoCell.setBorder(null);
        photoCell.setPadding(10);

        // Load Profile Photo if exists (simplified for now, using placeholder if path
        // invalid)
        try {
            // In a real scenario, we'd load user.getPhotoPath(). For now, using a text
            // placeholder or simple box
            Paragraph photoPlaceholder = new Paragraph("PHOTO")
                    .setHeight(80).setWidth(80)
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE);
            photoCell.add(photoPlaceholder);
        } catch (Exception e) {
            photoCell.add(new Paragraph("[Photo]"));
        }
        contentTable.addCell(photoCell);

        // 2. Details Section
        Cell detailsCell = new Cell();
        detailsCell.setBorder(null);
        detailsCell.setPadding(10);

        detailsCell.add(new Paragraph("Name: " + user.getFullName()).setBold().setFontSize(12));
        detailsCell.add(new Paragraph("DOB: " + user.getDob()).setFontSize(10));
        detailsCell.add(new Paragraph("Gender: " + user.getGender()).setFontSize(10));
        detailsCell.add(new Paragraph("Address:").setBold().setFontSize(10));
        detailsCell.add(new Paragraph(user.getAddressLine1() + ", " + user.getCity()).setFontSize(9));
        detailsCell.add(new Paragraph(user.getState() + " - " + user.getPincode()).setFontSize(9));

        // Aadhaar Number (Big and Red)
        detailsCell.add(new Paragraph("\n" + user.getAadhaarNumber())
                .setFontSize(18).setBold().setFontColor(ColorConstants.BLACK)
                .setTextAlignment(TextAlignment.CENTER)
                .setBorderTop(new com.itextpdf.layout.borders.SolidBorder(1))
                .setMarginTop(10).setPaddingTop(5));

        contentTable.addCell(detailsCell);

        // 3. QR Code Section (Placeholder)
        Cell qrCell = new Cell();
        qrCell.setBorder(null);
        qrCell.setPadding(10);
        qrCell.setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE);

        Paragraph qrPlaceholder = new Paragraph("QR CODE")
                .setHeight(80).setWidth(80)
                .setBackgroundColor(ColorConstants.GRAY) // Darker gray
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE);

        qrCell.add(qrPlaceholder);
        contentTable.addCell(qrCell);

        // Add Content Table to Main Table
        Cell contentContainer = new Cell().add(contentTable);
        contentContainer.setBorder(null);
        mainTable.addCell(contentContainer);

        // Footer (Bottom Bar)
        Cell footerCell = new Cell().add(new Paragraph("Mera Aadhaar, Meri Pehchan"))
                .setBackgroundColor(new DeviceRgb(221, 221, 221))
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(5);
        footerCell.setBorder(null);
        mainTable.addCell(footerCell);

        // Add Main Table to Document
        document.add(mainTable);

        // Add "Cut Here" line
        document.add(new Paragraph("\n\n------------------------------ Cut Here ------------------------------")
                .setTextAlignment(TextAlignment.CENTER).setFontSize(8).setItalic());

        document.close();
    }

    private void addCell(Table table, String text, boolean isHeader) {
        // Deprecated helper in this new design, keeping for backward compatibility if
        // needed,
        // or just removing it if I replace the whole block effectively.
        // Since I'm replacing the block including this method, I can just omit it or
        // leave valid code.
    }

    public Path getPdfPath(String filename) {
        return cardStorageLocation.resolve(filename);
    }
}
