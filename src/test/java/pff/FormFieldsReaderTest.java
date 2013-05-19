package pff;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FormFieldsReaderTest {
    @Test
    public void noFields() {
        FormFieldsReader reader = new FormFieldsReader("");
        Map<String, String> fields = reader.read();
        assertTrue(fields.isEmpty());
    }

    @Test
    public void regularFieldName() {
        FormFieldsReader reader = new FormFieldsReader("field value");
        Map<String, String> fields = reader.read();
        assertContains(fields, "field", "value");
    }

    @Test
    public void fieldNameWithSpace() {
        FormFieldsReader reader = new FormFieldsReader("[field name] value");
        Map<String, String> fields = reader.read();
        assertContains(fields, "field name", "value");
    }

    @Test
    public void unescaping() {
        FormFieldsReader reader = new FormFieldsReader("field [\\n\\p\\\\]");
        Map<String, String> fields = reader.read();
        assertContains(fields, "field", "[\n\u2029\\]");
    }

    @Test
    public void multipleFields() {
        FormFieldsReader reader = new FormFieldsReader("field1 value1\nfield2 value2");
        Map<String, String> fields = reader.read();
        assertContains(fields, "field1", "value1");
        assertContains(fields, "field2", "value2");
    }

    @Test
    public void invalidEntries() {
        FormFieldsReader reader = new FormFieldsReader("field-value");
        Map<String, String> fields = reader.read();
        assertTrue(fields.isEmpty());
    }

    private void assertContains(Map<String, String> fields, String field, String expected) {
        assertTrue("Field " + field + " not found", fields.containsKey(field));
        String actual = fields.get(field);
        assertEquals("Invalid field " + field + " value", expected, actual);
    }


}
