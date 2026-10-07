package com.terman37.triplogger.report

import com.terman37.triplogger.testtext.EnglishText
import com.terman37.triplogger.core.TripOrigin
import com.terman37.triplogger.data.Trip
import java.io.ByteArrayInputStream
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.zip.ZipInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportXlsxBuilderTest {

    private val utc = ZoneId.of("UTC")

    private fun epoch(utcTime: String): Long =
        LocalDateTime.parse(utcTime).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun trip(
        start: Long,
        end: Long,
        startCity: String? = "Paris",
        startStreet: String? = "12 Rue de Rivoli",
        endCity: String? = "Lyon",
        endStreet: String? = "1 Rue de la République",
        startLat: Double? = null, startLng: Double? = null,
        endLat: Double? = null, endLng: Double? = null,
        km: Double,
    ) = Trip(
        startEpochMillis = start,
        startLat = startLat, startLng = startLng,
        startStreet = startStreet, startCity = startCity,
        endEpochMillis = end,
        endLat = endLat, endLng = endLng,
        endStreet = endStreet, endCity = endCity,
        distanceKm = km,
        origin = TripOrigin.AUTO,
    )

    /** Unzips the .xlsx bytes into `entry name -> UTF-8 text`. */
    private fun entries(bytes: ByteArray): Map<String, String> {
        val result = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val read = zip.read(buffer)
                    if (read < 0) break
                    out.write(buffer, 0, read)
                }
                result[entry.name] = out.toString(Charsets.UTF_8.name())
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return result
    }

    private fun build(trips: List<Trip>): Map<String, String> =
        entries(ReportXlsxBuilder.build(trips, utc, EnglishText.xlsxLabels))

    /** Expected Excel serial (days since 1899-12-30) for a UTC local date/time. */
    private fun serial(local: LocalDateTime): Double =
        local.toEpochSecond(ZoneOffset.UTC) / 86_400.0 + 25569.0

    @Test
    fun zip_containsAllRequiredParts() {
        val parts = build(emptyList()).keys
        assertTrue(parts.containsAll(
            listOf(
                "[Content_Types].xml",
                "_rels/.rels",
                "xl/workbook.xml",
                "xl/_rels/workbook.xml.rels",
                "xl/styles.xml",
                "xl/worksheets/sheet1.xml",
                "xl/worksheets/_rels/sheet1.xml.rels",
            ),
        ))
    }

    @Test
    fun headers_matchLayout() {
        val sheet = build(emptyList())["xl/worksheets/sheet1.xml"]!!
        val headers = listOf("start date", "end date", "start address", "end address", "km", "trip")
        headers.forEachIndexed { column, title ->
            val cell = ('A' + column).toString()
            assertTrue(
                "missing header in $cell: $title",
                sheet.contains("<c r=\"${cell}1\" s=\"1\" t=\"inlineStr\"><is>" +
                    "<t xml:space=\"preserve\">$title</t></is></c>"),
            )
        }
    }

    @Test
    fun dates_areTrueExcelDates_withDisplayNumberFormat() {
        val t = trip(
            start = epoch("2026-08-06T14:32"), end = epoch("2026-08-06T15:15"), km = 1.0,
        )
        val parts = build(listOf(t))
        val sheet = parts["xl/worksheets/sheet1.xml"]!!

        // Numeric serials, not text, so Excel can sort/compare/re-format them.
        val start = serial(LocalDateTime.parse("2026-08-06T14:32"))
        val end = serial(LocalDateTime.parse("2026-08-06T15:15"))
        assertTrue(sheet.contains("<c r=\"A2\" s=\"7\"><v>$start</v></c>"))
        assertTrue(sheet.contains("<c r=\"B2\" s=\"7\"><v>$end</v></c>"))

        // The display format is defined by the date cell style; Excel renders
        // dddd/mmmm in the user's own language.
        val styles = parts["xl/styles.xml"]!!
        assertTrue(styles.contains("formatCode=\"dddd d mmmm, hh:mm\""))
    }

    @Test
    fun addresses_areConcatenatedAndHyperlinked() {
        val t = trip(
            start = epoch("2026-08-06T14:32"), end = epoch("2026-08-06T15:15"), km = 12.4,
        )
        val parts = build(listOf(t))
        val sheet = parts["xl/worksheets/sheet1.xml"]!!

        // street + city in one cell, styled as a link, on the trip's row (2).
        assertTrue(sheet.contains("<c r=\"C2\" s=\"3\" t=\"inlineStr\"><is>" +
            "<t xml:space=\"preserve\">12 Rue de Rivoli, Paris</t></is></c>"))
        assertTrue(sheet.contains("<c r=\"D2\" s=\"3\" t=\"inlineStr\"><is>" +
            "<t xml:space=\"preserve\">1 Rue de la République, Lyon</t></is></c>"))
        assertTrue(sheet.contains("<hyperlink ref=\"C2\" r:id=\"rId1\"/>"))
        assertTrue(sheet.contains("<hyperlink ref=\"D2\" r:id=\"rId2\"/>"))

        val rels = parts["xl/worksheets/_rels/sheet1.xml.rels"]!!
        assertTrue(rels.contains("Target=\"https://www.google.com/maps/search/?api=1&amp;" +
            "query=12+Rue+de+Rivoli%2C+Paris\" TargetMode=\"External\""))
        assertTrue(rels.contains("query=1+Rue+de+la+R%C3%A9publique%2C+Lyon"))
    }

    @Test
    fun tripCell_hyperlinksDirections() {
        val t = trip(
            start = epoch("2026-08-06T14:32"), end = epoch("2026-08-06T15:15"),
            startLat = 48.8566, startLng = 2.3522,
            endLat = 48.8738, endLng = 2.2950,
            km = 3.0,
        )
        val parts = build(listOf(t))
        val sheet = parts["xl/worksheets/sheet1.xml"]!!
        assertTrue(sheet.contains("<c r=\"F2\" s=\"9\" t=\"inlineStr\"><is>" +
            "<t xml:space=\"preserve\">trip</t></is></c>"))

        val rels = parts["xl/worksheets/_rels/sheet1.xml.rels"]!!
        assertTrue(rels.contains("/maps/dir/?api=1&amp;origin="))
        assertTrue(rels.contains("&amp;destination="))
        assertTrue(rels.contains("&amp;travelmode=driving"))
    }

    @Test
    fun missingAddress_showsNotFound_stillLinkedWhenCoordinatesExist() {
        val t = trip(
            start = epoch("2026-08-06T14:32"), end = epoch("2026-08-06T15:15"),
            startCity = null, startStreet = null,
            endCity = null, endStreet = null,
            startLat = 48.8566, startLng = 2.3522,
            // end has neither address nor coordinates
            km = 3.0,
        )
        val parts = build(listOf(t))
        val sheet = parts["xl/worksheets/sheet1.xml"]!!

        assertTrue(sheet.contains("<c r=\"C2\" s=\"3\" t=\"inlineStr\"><is>" +
            "<t xml:space=\"preserve\">${EnglishText.xlsxLabels.addressNotFound}</t></is></c>"))
        // No coordinates → plain text, not a link.
        assertTrue(sheet.contains("<c r=\"D2\" s=\"2\" t=\"inlineStr\"><is>" +
            "<t xml:space=\"preserve\">${EnglishText.xlsxLabels.addressNotFound}</t></is></c>"))
        assertTrue(parts["xl/worksheets/_rels/sheet1.xml.rels"]!!.contains("query=48.856600%2C2.352200"))
    }

    @Test
    fun totalRow_isYellowBold_withKmSumUnderKmColumn() {
        val t1 = trip(start = epoch("2026-08-06T14:32"), end = epoch("2026-08-06T15:15"), km = 12.4)
        val t2 = trip(start = epoch("2026-08-07T08:05"), end = epoch("2026-08-07T08:40"), km = 35.0)
        val sheet = build(listOf(t1, t2))["xl/worksheets/sheet1.xml"]!!

        // Total on row 4 (2 trips + header + total).
        assertTrue(sheet.contains("<c r=\"A4\" s=\"4\" t=\"inlineStr\"><is>" +
            "<t xml:space=\"preserve\">Total</t></is></c>"))
        assertTrue(sheet.contains("<c r=\"E4\" s=\"6\"><f>SUM(E2:E3)</f><v>47.4</v></c>"))
    }

    @Test
    fun styles_defineGrayHeaderYellowTotalBordersAndFrozenPane() {
        val parts = build(emptyList())
        val styles = parts["xl/styles.xml"]!!
        assertTrue(styles.contains("<fgColor rgb=\"FFD9D9D9\"/>")) // light gray header
        assertTrue(styles.contains("<fgColor rgb=\"FFFFF2CC\"/>")) // light yellow total
        assertTrue(styles.contains("<left style=\"thin\"><color rgb=\"FF000000\"/>")) // solid black border
        assertTrue(styles.contains("<u/>"))                        // underline font
        assertTrue(parts["xl/worksheets/sheet1.xml"]!!
            .contains("state=\"frozen\""))
    }

    @Test
    fun xmlSpecialCharacters_areEscaped() {
        val t = trip(
            start = epoch("2026-08-06T14:32"), end = epoch("2026-08-06T15:15"),
            startCity = "A & B", startStreet = "Rue <X>",
            km = 1.0,
        )
        val sheet = build(listOf(t))["xl/worksheets/sheet1.xml"]!!
        assertTrue(sheet.contains("Rue &lt;X&gt;, A &amp; B"))
    }

    @Test
    fun km_isNumericWithOneDecimalDot() {
        val t = trip(start = epoch("2026-08-06T14:32"), end = epoch("2026-08-06T15:15"), km = 12.5)
        val sheet = build(listOf(t))["xl/worksheets/sheet1.xml"]!!
        assertTrue(sheet.contains("<c r=\"E2\" s=\"5\"><v>12.5</v></c>"))
    }

    @Test
    fun timeZone_affectsTheDateValue() {
        val start = epoch("2026-08-06T22:30") // UTC 22:30
        val t = trip(start = start, end = start + 3_600_000, km = 5.0)
        val sheet = entries(
            ReportXlsxBuilder.build(listOf(t), ZoneId.of("Europe/Paris"), EnglishText.xlsxLabels),
        )["xl/worksheets/sheet1.xml"]!!
        // Paris is UTC+2 in August: 22:30 UTC → next day 00:30 local.
        val expected = serial(LocalDateTime.parse("2026-08-07T00:30"))
        assertTrue(sheet.contains("<c r=\"A2\" s=\"7\"><v>$expected</v></c>"))
    }

    @Test
    fun emptyList_hasOnlyHeaderAndZeroTotal() {
        val sheet = build(emptyList())["xl/worksheets/sheet1.xml"]!!
        assertTrue(sheet.contains("<c r=\"E2\" s=\"6\"><v>0.0</v></c>"))
        assertFalse(sheet.contains("<hyperlink"))
        // No data rows → no SUM range to write.
        assertFalse(sheet.contains("<f>"))
    }
}
