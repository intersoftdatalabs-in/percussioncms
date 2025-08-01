// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.assetmanagement.service.impl;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Servlet responsible for converting supplied text to an image and serving it as an image.
 */
public class PSTextToImageServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        var imageText = Optional.ofNullable(request.getParameter("imageText"))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .orElse(DEFAULT_IMAGE_TEXT);

        // Use a temporary image to get FontMetrics
        var font = new Font("Verdana", Font.PLAIN, 11);
        var tempImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        var tempGraphics = tempImg.createGraphics();
        tempGraphics.setFont(font);
        var metrics = tempGraphics.getFontMetrics();
        Rectangle2D bounds = metrics.getStringBounds(imageText, tempGraphics);
        int widthInPixels = (int) Math.ceil(bounds.getWidth()) + 2;
        int heightInPixels = (int) Math.ceil(bounds.getHeight());
        tempGraphics.dispose();

        // Create the actual image
        var buffer = new BufferedImage(widthInPixels, heightInPixels, BufferedImage.TYPE_INT_ARGB);
        var graphics = buffer.createGraphics();
        graphics.setFont(font);
        graphics.setColor(new Color(255, 102, 0));
        graphics.drawString(imageText, 1, metrics.getAscent());
        graphics.dispose();

        // Write the image to response
        response.setContentType("image/png");
        try (OutputStream os = response.getOutputStream()) {
            ImageIO.setUseCache(false);
            ImageIO.write(buffer, "png", os);
        }
    }

    /**
     * The logger.
     */
    private static final Logger ms_logger = LogManager.getLogger(PSTextToImageServlet.class);

    private static final String DEFAULT_IMAGE_TEXT = "";
}
