package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToString
import androidx.test.core.app.ApplicationProvider
import com.example.domain.importer.DocxTextExtractor
import com.example.domain.importer.SongParserEngine
import com.example.domain.model.SongLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appName_matchesMhopSongs() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MHOP Songs", appName)
    }

    @Test
    fun docxExtractor_extractsParagraphsAndLinksFromValidDocxArchive() {
        val documentXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                        xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
              <w:body>
                <w:p>
                  <w:pPr><w:pStyle w:val="Heading1"/></w:pPr>
                  <w:r><w:t>స్తుతి గీతం</w:t></w:r>
                </w:p>
                <w:p>
                  <w:r><w:t>పల్లవి:</w:t></w:r>
                </w:p>
                <w:p>
                  <w:r><w:t>స్తోత్రార్హుడా యేసయ్యా</w:t></w:r>
                </w:p>
                <w:p>
                  <w:hyperlink r:id="rId1"><w:r><w:t>Watch Video</w:t></w:r></w:hyperlink>
                </w:p>
              </w:body>
            </w:document>
        """.trimIndent()

        val relsXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Target="https://youtube.com/watch?v=test123" TargetMode="External"/>
            </Relationships>
        """.trimIndent()

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            zip.putNextEntry(ZipEntry("word/document.xml"))
            zip.write(documentXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
            zip.write(relsXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }

        val extracted = DocxTextExtractor.extract(ByteArrayInputStream(baos.toByteArray()))
        assertTrue(extracted.text.contains("స్తుతి గీతం"))
        assertTrue(extracted.links.contains("https://youtube.com/watch?v=test123"))

        val drafts = SongParserEngine.parseDocumentIntoSongs(
            fileName = "telugu_song.docx",
            rawText = extracted.text,
            documentLinks = extracted.links,
        )
        assertEquals(1, drafts.size)
        assertEquals(SongLanguage.TELUGU, drafts[0].language)
        assertEquals("https://youtube.com/watch?v=test123", drafts[0].videoUrl)
    }

    @Test
    fun criticalUserJourney_adminSignIn_addSong_searchAndReadWithChords() {
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodes(androidx.compose.ui.test.hasText("తెలుగు  |  हिन्दी  |  English"))
                .fetchSemanticsNodes()
                .isEmpty()
        }
        composeTestRule.waitForIdle()

        // Home screen displays MHOP Songs and 3 languages
        composeTestRule.onNodeWithTag("language_card_te").assertIsDisplayed()
        composeTestRule.onNodeWithTag("language_card_hi").assertIsDisplayed()
        composeTestRule.onNodeWithTag("language_card_en").assertIsDisplayed()

        // Navigate to Settings and sign in as Local Admin Preview
        composeTestRule.onNodeWithTag("nav_settings").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("settings_list")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("settings_admin_sign_in_button"))
        composeTestRule.onNodeWithTag("settings_admin_sign_in_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("settings_list")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("admin_signin_name_input"))
        composeTestRule.onNodeWithTag("admin_signin_name_input").performTextInput("Worship Admin")
        composeTestRule.onNodeWithTag("settings_list")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("admin_signin_confirm_button"))
        composeTestRule.onNodeWithTag("admin_signin_confirm_button").performClick()
        composeTestRule.waitForIdle()

        // Open Admin Dashboard and add a song manually
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodes(androidx.compose.ui.test.hasTestTag("nav_admin"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithTag("settings_list")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("settings_open_admin_dashboard"))
        composeTestRule.onNodeWithTag("settings_open_admin_dashboard").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("admin_add_song_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("editor_lang_en").performClick()
        composeTestRule.onNodeWithTag("editor_title_input").performTextInput("Psalm 23 Worship")
        composeTestRule.onNodeWithTag("editor_lyrics_input")
            .performTextInput("Chorus:\n[G]The Lord is my [C]shepherd [G]always")
        composeTestRule.onNodeWithTag("editor_save_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        // Navigate back from Admin Dashboard to Settings (where BottomBar is visible), then open Search
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodes(androidx.compose.ui.test.hasText("Psalm 23 Worship"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("nav_search").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodes(androidx.compose.ui.test.hasText("Psalm 23 Worship"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("Psalm 23 Worship").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodes(androidx.compose.ui.test.hasTestTag("reader_song_title"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithTag("reader_song_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("transpose_up_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("+1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("reader_favourite_button").performClick()
        composeTestRule.waitForIdle()
    }
}
