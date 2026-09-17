package com.carefinder.backend.hospital;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ComparisonReportService {

    private final HospitalService hospitalService;

    public ComparisonReportService(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @Transactional(readOnly = true)
    public byte[] create(List<Long> ids) {
        List<Hospital> hospitals = hospitalService.hospitalsInRequestedOrder(ids);
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDRectangle landscapeA4 = new PDRectangle(
                    PDRectangle.A4.getHeight(),
                    PDRectangle.A4.getWidth());

            PDPage page = new PDPage(landscapeA4);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = page.getMediaBox().getHeight() - 48;
                write(content, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 20, 44, y,
                        "CareFinder Hospital Comparison");
                y -= 30;
                write(content, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9, 44, y,
                        "Verify insurance participation and service availability directly before treatment.");
                y -= 36;
                for (Hospital hospital : hospitals) {
                    write(content, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 13, 44, y,
                            hospital.getName());
                    y -= 17;
                    write(content, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9, 54, y,
                            hospital.getCity() + ", " + hospital.getState()
                                    + " | Rating " + hospital.getRating()
                                    + " | " + hospital.getBeds() + " beds");
                    y -= 14;
                    write(content, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9, 54, y,
                            "Emergency: " + yesNo(hospital.isEmergency())
                                    + " | Open 24x7: " + yesNo(hospital.isOpen24x7())
                                    + " | " + hospital.getAccreditation());
                    y -= 14;
                    write(content, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9, 54, y,
                            "Specialties: " + String.join(", ", hospital.getSpecialties()));
                    y -= 26;
                }
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to generate comparison report.", exception);
        }
    }

    private void write(
            PDPageContentStream content,
            PDType1Font font,
            float fontSize,
            float x,
            float y,
            String text) throws IOException {
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        content.showText(safeText(text));
        content.endText();
    }

    private String safeText(String value) {
        return value.replaceAll("[^\\x20-\\x7E]", " ");
    }

    private String yesNo(boolean value) {
        return value ? "Yes" : "No";
    }
}
