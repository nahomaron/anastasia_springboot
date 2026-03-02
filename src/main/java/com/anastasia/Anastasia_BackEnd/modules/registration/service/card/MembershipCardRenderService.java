package com.anastasia.Anastasia_BackEnd.modules.registration.service.card;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;

@Service
public class MembershipCardRenderService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    public byte[] renderCardImage(MembershipCardRenderModel model) {
        try {
            int width = 1100;
            int height = 680;
            BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = canvas.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Color primary = parseColor(model.primaryColor(), new Color(24, 54, 97));
            Color accent = parseColor(model.accentColor(), new Color(196, 155, 63));
            Color text = parseColor(model.textColor(), Color.WHITE);

            g.setPaint(new java.awt.GradientPaint(0, 0, primary, width, height, accent));
            g.fillRoundRect(0, 0, width, height, 36, 36);

            g.setColor(new Color(255, 255, 255, 28));
            g.fillOval(-180, -200, 560, 560);
            g.fillOval(760, 320, 420, 420);

            g.setComposite(AlphaComposite.SrcOver);
            g.setColor(text);

            drawChurchLogo(g, model.churchLogoUrl(), width - 180, 40, 120, 120);
            drawAvatar(g, model.memberAvatarUrl(), 50, 170, 220, 260);

            g.setFont(new Font("SansSerif", Font.BOLD, 44));
            g.drawString("Church Membership Card", 300, 95);

            g.setFont(new Font("SansSerif", Font.PLAIN, 22));
            g.drawString("Template: " + safe(model.templateDisplayName(), "Classic Gold"), 302, 130);

            g.setFont(new Font("SansSerif", Font.BOLD, 34));
            g.drawString(truncate(g, model.memberFullName(), 520), 300, 200);

            g.setFont(new Font("SansSerif", Font.PLAIN, 23));
            g.drawString("DOB: " + model.dateOfBirth().format(DATE_FORMAT), 300, 245);
            g.drawString("Church: " + model.churchName(), 300, 285);
            g.drawString("Member No: " + model.membershipNumber(), 300, 325);
            g.drawString("Issue Date: " + model.issueDate().format(DATE_FORMAT), 300, 365);
            g.drawString("Expiration: " + model.expirationDate().format(DATE_FORMAT), 300, 405);
            g.drawString("Card Serial: " + model.cardSerialNumber(), 300, 445);

            BufferedImage qrImage = generateQr(model.qrPayload(), 240, 240);
            g.setColor(new Color(255, 255, 255, 210));
            g.fillRoundRect(820, 390, 245, 245, 14, 14);
            g.drawImage(qrImage, 822, 392, null);

            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            g.setColor(new Color(245, 245, 245));
            g.drawString("Scan to verify membership status", 790, 655);

            g.dispose();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(canvas, "png", output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to render membership card image", ex);
        }
    }

    public byte[] renderCardPdf(MembershipCardRenderModel model, byte[] imageBytes) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(new PDRectangle(792, 612));
            document.addPage(page);

            PDImageXObject image = PDImageXObject.createFromByteArray(document, imageBytes, "membership-card");

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.drawImage(image, 36, 130, 720, 445);
                content.beginText();
                content.setFont(new PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                content.newLineAtOffset(40, 95);
                content.showText("Digital Membership Card");
                content.endText();

                content.beginText();
                content.setFont(new PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), 11);
                content.newLineAtOffset(40, 76);
                content.showText("Name: " + model.memberFullName());
                content.endText();

                content.beginText();
                content.setFont(new PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), 11);
                content.newLineAtOffset(40, 61);
                content.showText("Membership #: " + model.membershipNumber() + " | Card Serial: " + model.cardSerialNumber());
                content.endText();
            }

            document.save(output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to render membership card PDF", ex);
        }
    }

    private BufferedImage generateQr(String value, int width, int height) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix matrix = new MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, width, height, hints);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    private Color parseColor(String value, Color fallback) {
        try {
            if (value == null || value.isBlank()) {
                return fallback;
            }
            return Color.decode(value);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private void drawAvatar(Graphics2D g, String avatarUrl, int x, int y, int w, int h) {
        g.setColor(new Color(255, 255, 255, 190));
        g.fillRoundRect(x, y, w, h, 26, 26);
        BufferedImage avatar = loadImage(avatarUrl);
        if (avatar != null) {
            g.drawImage(avatar, x + 8, y + 8, w - 16, h - 16, null);
            return;
        }

        g.setColor(new Color(32, 62, 102));
        g.fill(new RoundRectangle2D.Double(x + 24, y + 28, w - 48, h - 56, 20, 20));
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString("PHOTO", x + 72, y + (h / 2));
    }

    private void drawChurchLogo(Graphics2D g, String logoUrl, int x, int y, int w, int h) {
        g.setColor(new Color(255, 255, 255, 220));
        g.fillRoundRect(x, y, w, h, 18, 18);

        BufferedImage logo = loadImage(logoUrl);
        if (logo != null) {
            g.drawImage(logo, x + 8, y + 8, w - 16, h - 16, null);
            return;
        }

        g.setColor(new Color(36, 42, 58));
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.drawString("CHURCH", x + 34, y + 58);
        g.drawString("SEAL", x + 45, y + 80);
    }

    private BufferedImage loadImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        try {
            if (imageUrl.startsWith("data:image")) {
                String base64 = imageUrl.substring(imageUrl.indexOf(',') + 1);
                byte[] bytes = java.util.Base64.getDecoder().decode(base64);
                return ImageIO.read(new ByteArrayInputStream(bytes));
            }
            return ImageIO.read(new URL(imageUrl));
        } catch (Exception ex) {
            return null;
        }
    }

    private String truncate(Graphics2D g, String text, int maxWidth) {
        String source = safe(text, "N/A");
        FontMetrics metrics = g.getFontMetrics();
        if (metrics.stringWidth(source) <= maxWidth) {
            return source;
        }
        String suffix = "...";
        int len = source.length();
        while (len > 0) {
            String candidate = source.substring(0, len) + suffix;
            if (metrics.stringWidth(candidate) <= maxWidth) {
                return candidate;
            }
            len--;
        }
        return suffix;
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
