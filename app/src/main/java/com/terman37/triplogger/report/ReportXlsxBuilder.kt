package com.terman37.triplogger.report

import com.terman37.triplogger.core.MapsUrl
import com.terman37.triplogger.data.Trip
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Builds the expense-report Excel file (.xlsx). Columns:
 *
 *   start date | end date | start address | end address | km | trip
 *
 * - Dates are written as true Excel date/time values (numeric serials) with a
 *   `dddd d mmmm, hh:mm` number format, so Excel can sort, compare and re-format
 *   them (Format Cells) and shows the weekday/month names in its own language.
 * - Address cells concatenate street + city and link to Google Maps; when no
 *   address is known the cell says "Address not found" (still linked when the
 *   trip has coordinates).
 * - "trip" links to the Google Maps directions from start to end.
 * - Header row: light gray, bold, frozen. All cells have 0.75 pt solid black
 *   borders. Dates and the km and trip columns are centered. Final
 *   Total row holds `=SUM(...)` over the km column and is bold on a
 *   light-yellow background.
 *
 * An .xlsx file is a ZIP of a few XML parts (Office Open XML). We write the
 * parts directly instead of using Apache POI (huge and not Android friendly)
 * or another library (they need StAX, absent from the Android SDK). This keeps
 * the builder pure JVM (only java.util.zip) and unit-testable.
 */
object ReportXlsxBuilder {

    // Style indexes into the cellXfs list in [STYLES_XML]. Keep in sync.
    private const val STYLE_HEADER = 1
    private const val STYLE_BODY = 2
    private const val STYLE_LINK = 3
    private const val STYLE_TOTAL = 4
    private const val STYLE_KM_BODY = 5
    private const val STYLE_KM_TOTAL = 6
    private const val STYLE_DATE = 7
    private const val STYLE_BODY_CENTER = 8
    private const val STYLE_LINK_CENTER = 9

    /**
     * Days between the Excel epoch (1899-12-30, the 1900 date system) and the
     * Unix epoch (1970-01-01). Added to a day count to get the serial number
     * Excel stores for a date.
     */
    private const val EXCEL_EPOCH_OFFSET_DAYS = 25569.0

    /** One external cell hyperlink: which cell and where it points. */
    private data class Hyperlink(val ref: String, val target: String)

    /**
     * @param trips ascending by start time (as the DAO returns them).
     * @param zone local time zone used for the date columns.
     * @param labels column headers and fixed labels, resolved from resources by
     *   the caller (this object stays Android-free; see [XlsxLabels]).
     */
    fun build(trips: List<Trip>, zone: ZoneId, labels: XlsxLabels): ByteArray {
        val links = mutableListOf<Hyperlink>()
        val rows = StringBuilder()

        // --- header row ----------------------------------------------------
        rows.append("<row r=\"1\">")
        labels.headers().forEachIndexed { column, title ->
            rows.append(inlineCell(ref(column, 1), STYLE_HEADER, title))
        }
        rows.append("</row>")

        // --- one row per trip ----------------------------------------------
        trips.forEachIndexed { index, trip ->
            val row = index + 2
            rows.append("<row r=\"$row\">")
            rows.append(dateCell(ref(0, row), trip.startEpochMillis, zone))
            rows.append(dateCell(ref(1, row), trip.endEpochMillis, zone))
            appendLinkCell(
                rows, links, labels, ref(2, row),
                addressText(trip.startStreet, trip.startCity),
                MapsUrl.place(trip.startStreet, trip.startCity, trip.startLat, trip.startLng),
            )
            appendLinkCell(
                rows, links, labels, ref(3, row),
                addressText(trip.endStreet, trip.endCity),
                MapsUrl.place(trip.endStreet, trip.endCity, trip.endLat, trip.endLng),
            )
            rows.append(numberCell(ref(4, row), STYLE_KM_BODY, trip.distanceKm))
            appendLinkCell(
                rows, links, labels, ref(5, row), labels.trip,
                MapsUrl.directions(
                    trip.startStreet, trip.startCity, trip.startLat, trip.startLng,
                    trip.endStreet, trip.endCity, trip.endLat, trip.endLng,
                ),
                STYLE_BODY_CENTER, STYLE_LINK_CENTER,
            )
            rows.append("</row>")
        }

        // --- total row (km sum under the km column) -------------------------
        val totalRow = trips.size + 2
        val totalKm = trips.sumOf { it.distanceKm }
        rows.append("<row r=\"$totalRow\">")
        rows.append(inlineCell(ref(0, totalRow), STYLE_TOTAL, labels.total))
        for (column in 1..3) {
            rows.append(inlineCell(ref(column, totalRow), STYLE_TOTAL, ""))
        }
        rows.append(totalCell(ref(4, totalRow), trips.size, totalKm))
        rows.append(inlineCell(ref(5, totalRow), STYLE_TOTAL, ""))
        rows.append("</row>")

        return zip(
            "[Content_Types].xml" to CONTENT_TYPES_XML,
            "_rels/.rels" to ROOT_RELS_XML,
            "xl/workbook.xml" to WORKBOOK_XML,
            "xl/_rels/workbook.xml.rels" to WORKBOOK_RELS_XML,
            "xl/styles.xml" to STYLES_XML,
            "xl/worksheets/sheet1.xml" to SHEET_XML.format(rows, linksXml(links)),
            "xl/worksheets/_rels/sheet1.xml.rels" to SHEET_RELS_XML.format(sheetRelsXml(links)),
        )
    }

    /** "street, city", or "" when neither is known. */
    private fun addressText(street: String?, city: String?): String =
        listOfNotNull(
            street?.trim()?.takeIf { it.isNotEmpty() },
            city?.trim()?.takeIf { it.isNotEmpty() },
        ).joinToString(", ")

    /**
     * Address/place cell: shows [display] (or [XlsxLabels.addressNotFound] when
     * empty) and, if
     * [target] is known, registers it as an external hyperlink.
     */
    private fun appendLinkCell(
        rows: StringBuilder,
        links: MutableList<Hyperlink>,
        labels: XlsxLabels,
        cell: String,
        display: String,
        target: String?,
        plainStyle: Int = STYLE_BODY,
        linkStyle: Int = STYLE_LINK,
    ) {
        val text = display.ifEmpty { labels.addressNotFound }
        if (target == null) {
            rows.append(inlineCell(cell, plainStyle, text))
        } else {
            links += Hyperlink(cell, target)
            rows.append(inlineCell(cell, linkStyle, text))
        }
    }

    private fun inlineCell(cell: String, style: Int, text: String): String =
        "<c r=\"$cell\" s=\"$style\" t=\"inlineStr\"><is>" +
            "<t xml:space=\"preserve\">${escapeXml(text)}</t></is></c>"

    /** Real Excel date/time value (serial) with the display-format cell style. */
    private fun dateCell(cell: String, epochMillis: Long, zone: ZoneId): String =
        "<c r=\"$cell\" s=\"$STYLE_DATE\"><v>${dateSerial(epochMillis, zone)}</v></c>"

    /**
     * Converts an instant to the Excel serial number for its local wall-clock
     * date/time in [zone]. Excel stores no zone: it keeps days since 1899-12-30
     * plus the fraction of the day, so real dates behave normally in Excel.
     */
    private fun dateSerial(epochMillis: Long, zone: ZoneId): Double {
        val local = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDateTime()
        val seconds = local.toEpochSecond(ZoneOffset.UTC)
        return seconds / 86_400.0 + EXCEL_EPOCH_OFFSET_DAYS
    }

    private fun numberCell(cell: String, style: Int, value: Double): String =
        "<c r=\"$cell\" s=\"$style\"><v>${kmText(value)}</v></c>"

    /**
     * Total km cell: `=SUM(E2:E<last data row>)` with a cached value for
     * viewers that do not recalculate, so deleting a trip row in Excel keeps
     * the total correct. An empty report just holds 0.0 (no valid range).
     */
    private fun totalCell(cell: String, dataRowCount: Int, totalKm: Double): String {
        if (dataRowCount == 0) return numberCell(cell, STYLE_KM_TOTAL, totalKm)
        val range = "${ref(4, 2)}:${ref(4, dataRowCount + 1)}"
        return "<c r=\"$cell\" s=\"$STYLE_KM_TOTAL\">" +
            "<f>SUM($range)</f><v>${kmText(totalKm)}</v></c>"
    }

    /** One decimal, dot separator (OOXML always uses the dot). */
    private fun kmText(km: Double): String = String.format(Locale.US, "%.1f", km)

    /** Spreadsheet reference like "C12" (column 0 = A). */
    private fun ref(column: Int, row: Int): String = "${('A' + column)}${row}"

    private fun linksXml(links: List<Hyperlink>): String =
        if (links.isEmpty()) {
            ""
        } else {
            links.mapIndexed { index, link ->
                "<hyperlink ref=\"${link.ref}\" r:id=\"rId${index + 1}\"/>"
            }.joinToString(prefix = "<hyperlinks>", postfix = "</hyperlinks>")
        }

    private fun sheetRelsXml(links: List<Hyperlink>): String =
        links.mapIndexed { index, link ->
            "<Relationship Id=\"rId${index + 1}\"" +
                " Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink\"" +
                " Target=\"${escapeXml(link.target)}\" TargetMode=\"External\"/>"
        }.joinToString("")

    /** XML text/attribute escaping. */
    private fun escapeXml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    /** Writes the given `name to content` pairs (in order) into a ZIP. */
    private fun zip(vararg entries: Pair<String, String>): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return bytes.toByteArray()
    }

    // --- static OOXML parts ------------------------------------------------

    private val WORKSHEET_HEADER =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"" +
            " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
            // Frozen first row: pane split at y=1, data starts in A2.
            "<sheetViews><sheetView workbookViewId=\"0\">" +
            "<pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/>" +
            "<selection pane=\"bottomLeft\" activeCell=\"A2\" sqref=\"A2\"/>" +
            "</sheetView></sheetViews>" +
            "<sheetFormatPr defaultRowHeight=\"15\"/>" +
            "<cols>" +
            "<col min=\"1\" max=\"2\" width=\"26\" customWidth=\"1\"/>" +
            "<col min=\"3\" max=\"4\" width=\"46\" customWidth=\"1\"/>" +
            "<col min=\"5\" max=\"5\" width=\"8\" customWidth=\"1\"/>" +
            "<col min=\"6\" max=\"6\" width=\"8\" customWidth=\"1\"/>" +
            "</cols>"

    private val SHEET_XML = WORKSHEET_HEADER +
        "<sheetData>%s</sheetData>" +
        "%s" +
        "</worksheet>"

    private val SHEET_RELS_XML =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "%s</Relationships>"

    private val STYLES_XML =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
            // 164 = our "0.0" number format, 165 = the date/time display
            // format (Excel renders dddd/mmmm names in its own language).
            "<numFmts count=\"2\">" +
            "<numFmt numFmtId=\"164\" formatCode=\"0.0\"/>" +
            "<numFmt numFmtId=\"165\" formatCode=\"dddd d mmmm, hh:mm\"/>" +
            "</numFmts>" +
            "<fonts count=\"4\">" +
            "<font><sz val=\"11\"/><name val=\"Calibri\"/></font>" +
            "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font>" +
            "<font><u/><sz val=\"11\"/><color rgb=\"FF0563C1\"/><name val=\"Calibri\"/></font>" +
            "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font>" +
            "</fonts>" +
            "<fills count=\"4\">" +
            "<fill><patternFill patternType=\"none\"/></fill>" +
            "<fill><patternFill patternType=\"gray125\"/></fill>" +
            // Light gray header fill.
            "<fill><patternFill patternType=\"solid\">" +
            "<fgColor rgb=\"FFD9D9D9\"/><bgColor indexed=\"64\"/></patternFill></fill>" +
            // Light yellow total-row fill.
            "<fill><patternFill patternType=\"solid\">" +
            "<fgColor rgb=\"FFFFF2CC\"/><bgColor indexed=\"64\"/></patternFill></fill>" +
            "</fills>" +
            "<borders count=\"2\">" +
            "<border><left/><right/><top/><bottom/><diagonal/></border>" +
            // Thin = 0.75 pt solid black; a light color made it invisible.
            "<border>" +
            "<left style=\"thin\"><color rgb=\"FF000000\"/></left>" +
            "<right style=\"thin\"><color rgb=\"FF000000\"/></right>" +
            "<top style=\"thin\"><color rgb=\"FF000000\"/></top>" +
            "<bottom style=\"thin\"><color rgb=\"FF000000\"/></bottom>" +
            "<diagonal/></border>" +
            "</borders>" +
            "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
            "<cellXfs count=\"10\">" +
            "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" +
            // 1 header
            "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"1\" xfId=\"0\"" +
            " applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\">" +
            "<alignment horizontal=\"center\" vertical=\"center\"/></xf>" +
            // 2 body text
            "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\"" +
            " applyBorder=\"1\" applyAlignment=\"1\"><alignment vertical=\"center\"/></xf>" +
            // 3 hyperlink
            "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"0\" borderId=\"1\" xfId=\"0\"" +
            " applyFont=\"1\" applyBorder=\"1\" applyAlignment=\"1\">" +
            "<alignment vertical=\"center\"/></xf>" +
            // 4 total text (bold, yellow)
            "<xf numFmtId=\"0\" fontId=\"3\" fillId=\"3\" borderId=\"1\" xfId=\"0\"" +
            " applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\">" +
            "<alignment vertical=\"center\"/></xf>" +
            // 5 km number
            "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\"" +
            " applyNumberFormat=\"1\" applyBorder=\"1\" applyAlignment=\"1\">" +
            "<alignment horizontal=\"center\" vertical=\"center\"/></xf>" +
            // 6 km number, total (bold, yellow)
            "<xf numFmtId=\"164\" fontId=\"3\" fillId=\"3\" borderId=\"1\" xfId=\"0\"" +
            " applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"" +
            " applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\"/></xf>" +
            // 7 date/time (true Excel date), centered
            "<xf numFmtId=\"165\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\"" +
            " applyNumberFormat=\"1\" applyBorder=\"1\" applyAlignment=\"1\">" +
            "<alignment horizontal=\"center\" vertical=\"center\"/></xf>" +
            // 8 plain text, centered (trip column without a link)
            "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\"" +
            " applyBorder=\"1\" applyAlignment=\"1\">" +
            "<alignment horizontal=\"center\" vertical=\"center\"/></xf>" +
            // 9 hyperlink, centered (trip column)
            "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"0\" borderId=\"1\" xfId=\"0\"" +
            " applyFont=\"1\" applyBorder=\"1\" applyAlignment=\"1\">" +
            "<alignment horizontal=\"center\" vertical=\"center\"/></xf>" +
            "</cellXfs>" +
            "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>" +
            "</styleSheet>"

    private val CONTENT_TYPES_XML =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
            "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
            "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
            "<Override PartName=\"/xl/workbook.xml\"" +
            " ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
            "<Override PartName=\"/xl/worksheets/sheet1.xml\"" +
            " ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
            "<Override PartName=\"/xl/styles.xml\"" +
            " ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>" +
            "</Types>"

    private val ROOT_RELS_XML =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\"" +
            " Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\"" +
            " Target=\"xl/workbook.xml\"/>" +
            "</Relationships>"

    private val WORKBOOK_XML =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"" +
            " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
            "<sheets><sheet name=\"Trips\" sheetId=\"1\" r:id=\"rId1\"/></sheets>" +
            // Recalculate formulas on open, so the cached total can never show
            // stale after a row was deleted in Excel.
            "<calcPr calcId=\"0\" fullCalcOnLoad=\"1\"/>" +
            "</workbook>"

    private val WORKBOOK_RELS_XML =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\"" +
            " Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\"" +
            " Target=\"worksheets/sheet1.xml\"/>" +
            "<Relationship Id=\"rId2\"" +
            " Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\"" +
            " Target=\"styles.xml\"/>" +
            "</Relationships>"
}
