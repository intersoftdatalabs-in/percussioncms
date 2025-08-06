/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

// REFACTORED: CP-JAVA11
package com.percussion.sitemanage.importer.utils;

import com.percussion.sitemanage.importer.PSLink;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Harness for extracting links from a list of URLs and writing them to a CSV file.
 * Usage: java LinkExtractorHarness <inputFile> <outputFile>
 * Each line in inputFile should be a URL.
 * Output CSV columns: linkText, linkPath, absoluteLink, pageName
 */
public final class LinkExtractorHarness {

    private static final String COMMA = ",";
    private static final String CRLF = "\r\n";
    private static final Logger log = LogManager.getLogger(LinkExtractorHarness.class);

    private LinkExtractorHarness() {
        // Utility class; prevent instantiation
    }

    public static void main(final String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: java LinkExtractorHarness <inputFile> <outputFile>");
            System.exit(1);
        }
        var urls = FileUtils.readLines(new File(args[0]));
        var outputFile = new File(args[1]);
        try (var writer = new BufferedWriter(new FileWriter(outputFile))) {
            for (var site : urls) {
                try {
                    System.out.print("Evaluating: " + site + System.lineSeparator());
                    Document doc = Jsoup.connect(site)
                            .userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:12.0) Gecko/20100101 Firefox/12.0")
                            .referrer("http://www.google.com")
                            .get();
                    var links = PSLinkExtractor.getLinksForDocument(doc, null, null, site, null);
                    for (var link : links) {
                        writer.write(link.getLinkText() + COMMA
                                + link.getLinkPath() + COMMA
                                + link.getAbsoluteLink() + COMMA
                                + link.getPageName() + CRLF);
                    }
                } catch (IOException e) {
                    System.out.print(e.getMessage() + System.lineSeparator());
                    log.warn("Failed to process site: {}", site, e);
                }
            }
        }
    }
}
