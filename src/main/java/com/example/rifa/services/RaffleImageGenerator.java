package com.example.rifa.services;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import javax.imageio.ImageIO;

public class RaffleImageGenerator {

    /**
     * Genera una imagen para la rifa con el diseño especificado.
     *
     * @param raffleName         Nombre de la rifa.
     * @param productName        Nombre del producto.
     * @param productDescription Descripción del producto.
     * @param precio             Precio de la rifa.
     * @param outputDir          Directorio de salida donde se guardará la imagen.
     * @return El nombre del archivo de imagen generado.
     * @throws IOException Si ocurre un error al escribir la imagen en disco.
     */

    public String generateRaffleImage(String raffleName, String productName, String productDescription,
                                      BigDecimal precio, String outputDir) throws IOException {
        int width = 600;
        int height = 400;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Habilitar antialiasing para mejorar la calidad del renderizado
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dibujar fondo con gradiente
        GradientPaint gp = new GradientPaint(0, 0, new Color(102, 163, 255), 0, height, Color.WHITE);
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, width, height);

        // Dibujar encabezado: "Super" y "RIFA!" rotado ligeramente
        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        g2d.setColor(Color.WHITE);
        g2d.drawString("Super", 20, 50);

        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        g2d.setColor(Color.YELLOW);
        g2d.rotate(Math.toRadians(-5), 150, 50);
        g2d.drawString("RIFA!", 100, 50);
        g2d.rotate(Math.toRadians(5), 150, 50); // Restablecer la rotación

        // Dibujar una caja de contenido con bordes redondeados
        int boxX = 50, boxY = 80, boxWidth = 500, boxHeight = 280;
        g2d.setColor(Color.WHITE);
        g2d.fill(new RoundRectangle2D.Float(boxX, boxY, boxWidth, boxHeight, 20, 20));
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.setStroke(new BasicStroke(2));
        g2d.draw(new RoundRectangle2D.Float(boxX, boxY, boxWidth, boxHeight, 20, 20));

        // Dibujar el título del producto centrado
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.setColor(new Color(59, 51, 0));
        FontMetrics fm = g2d.getFontMetrics();
        int titleX = boxX + (boxWidth - fm.stringWidth(productName)) / 2;
        g2d.drawString(productName, titleX, boxY + 40);

        // Dibujar la descripción del producto
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        fm = g2d.getFontMetrics();
        int descX = boxX + 20;
        int descY = boxY + 70;
        g2d.drawString(productDescription, descX, descY);

        // Dibujar el "price-box" con el precio
        int priceBoxWidth = 200, priceBoxHeight = 60;
        int priceBoxX = boxX + (boxWidth - priceBoxWidth) / 2;
        int priceBoxY = boxY + boxHeight - priceBoxHeight - 20;
        g2d.setColor(new Color(31, 114, 221));
        g2d.fillRoundRect(priceBoxX, priceBoxY, priceBoxWidth, priceBoxHeight, 30, 30);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        String priceText = "$ " + precio.toPlainString();
        fm = g2d.getFontMetrics();
        int priceX = priceBoxX + (priceBoxWidth - fm.stringWidth(priceText)) / 2;
        int priceY = priceBoxY + ((priceBoxHeight - fm.getHeight()) / 2) + fm.getAscent();
        g2d.drawString(priceText, priceX, priceY);

        // Liberar recursos
        g2d.dispose();

        // Guardar la imagen en disco y devolver el nombre del archivo
        String fileName = "raffle_" + System.currentTimeMillis() + ".png";
        File outputFile = new File(outputDir, fileName);
        ImageIO.write(image, "png", outputFile);
        return fileName;
    }
}
