package uk.gov.moj.cpp.subscriptions.material.client;

import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link MaterialUrlGenerator}, the subscriptions-owned copy of material's URL-building helper
 * that was introduced when subscriptions was decoupled from the material-client JAR. Verifies the composed
 * download URL strings for the plain and PDF-stream variants.
 */
public class MaterialUrlGeneratorTest {

    private static final String BASE_URI = "http://localhost:8080/material-query-api/query/api/rest/material";
    private static final String MATERIAL_REQUEST_PATH = "/material/";
    private static final String PDF_PARAMETERS = "?stream=true&requestPdf=true";

    private final MaterialUrlGenerator materialUrlGenerator = new MaterialUrlGenerator();

    @Test
    public void shouldBuildPlainFileStreamUrlForMaterialId() {

        final UUID materialId = fromString("11111111-2222-3333-4444-555555555555");

        final String url = materialUrlGenerator.fileStreamUrlFor(materialId);

        assertThat(url, is(BASE_URI + MATERIAL_REQUEST_PATH + materialId));
    }

    @Test
    public void shouldBuildPdfFileStreamUrlForMaterialId() {

        final UUID materialId = fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

        final String url = materialUrlGenerator.pdfFileStreamUrlFor(materialId);

        assertThat(url, is(BASE_URI + MATERIAL_REQUEST_PATH + materialId + PDF_PARAMETERS));
    }

    @Test
    public void shouldBuildPdfFileStreamUrlWhenPdfStreamFlagIsTrue() {

        final UUID materialId = fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

        final String url = materialUrlGenerator.fileStreamUrlFor(materialId, true);

        assertThat(url, is(BASE_URI + MATERIAL_REQUEST_PATH + materialId + PDF_PARAMETERS));
    }

    @Test
    public void shouldBuildPlainFileStreamUrlWhenPdfStreamFlagIsFalse() {

        final UUID materialId = fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

        final String url = materialUrlGenerator.fileStreamUrlFor(materialId, false);

        assertThat(url, is(BASE_URI + MATERIAL_REQUEST_PATH + materialId));
    }
}
