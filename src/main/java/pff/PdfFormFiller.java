package pff;
/**
 * pdfformfiller 1.0-alpha is a command line utility for filling in Adobe PDF Forms. 
 * 
 * Well known pdftk utility can be used for filling in Adobe Pdf Forms. 
 * However, I was not able to get the version pdftk1.4 to work with UTF-8. 
 * It's XFDF format support UTF-8 encoding, however it assumes Adobe uses an UTF-8
 * font by default. Whereas, Adobe Readers (at least upto version X) do not, 
 * and UTF-8 text is entered by pdftk but is not shown in its form until user clicks 
 * on the form and edits it.
 * 
 * In PdfFormFiller, you can use the -font option to specify a UTF-8 font 
 * to use to fill in the forms to resolve this issue.
 * 
 * Also, our fields input file format is much simpler then XFDF of pdftk that
 * requires XML parsing.
 * 
 * Based on the Belgian iText library v. 5.2.0, http://www.itextpdf.com/
 *
 * (C) copyleft AGPL license, http://itextpdf.com/terms-of-use/agpl.php, Nikolay Kitsul.
 * 
 * @author Nikolay Kitsul
 * @version 1.0-alpha
 */


import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Map;

class WrongParamsExeption extends Exception {}

public class PdfFormFiller {
    static Boolean verbose;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args){
        String document, operation = "fill", fields = null, font = null, output = null;
        String encoding = getDefaultEncoding();
        Boolean flatten = false;
        verbose = false;
        
        try {
            if (args.length < 1)
                throw new WrongParamsExeption();
            document = args[0];
            for(int i=1; i<args.length; i++){
                if (args[i].equals("-v")){
                    verbose = true;
                }else if (args[i].equals("-flatten")){
                    flatten = true;
                }else if (args[i].equals("-l")){
                    operation = "list";
                }else if (args[i].equals("-f")){
                    if (i + 1 >= args.length)
                        throw new WrongParamsExeption();
                    fields = args[++i];
                }else if (args[i].equals("-e")){
                    if (i + 1 >= args.length)
                        throw new WrongParamsExeption();
                    encoding = args[++i];
                }else if (args[i].equals("-font")){
                    if (i + 1 >= args.length)
                        throw new WrongParamsExeption();
                    font = args[++i];
                }else if (i + 1 == args.length){
                    output = args[i];
                } else{
                    throw new WrongParamsExeption();
                }
            }

            fillPDFFile(document, output, fields, encoding, font, operation, flatten, verbose);
 
        } catch (WrongParamsExeption e){
            if (e.getMessage() != null)
                System.out.println(e.getMessage());
           System.out.println("USAGE: pdfformfiller document.pdf [ -l ] [ -v ] [ -f fields_filename ] [ -font font_file ] [ -flatten] [ output.pdf ]\n\n" +
                               "    document.pdf - name of source pdf file (required).\n" +
                               "    -l - only list availible fields in document.pdf.\n" +
                               "    -v - verbose. Use to debug the fields_filename file. \n" +
                               "    -f fields_filename - name of file with the list of fields values to apply to document.pdf. \n" + 
                               "                         if ommited, stdin is used.\n" +
                               "    -e encoding - encoding of the file with the list of fields values (current: " + encoding + ")\n" +
                               "    -font font_file - font to use. Needed UTF-8 support, e.g. cyrillic and non-latin alphabets.\n" + 
                               "    -flatten - Flatten pdf forms (convert them to text disabling editing in PDF Reader).\n" + 
                               "    output.pdf - name of output file. If omitted, the output if sent to stdout. \n\n" +
                               "fields_filename file can be in UTF-8 as is of the following format:\n" + 
                               "    On each line, one entry consists of 'field name' followed by value of that field without any quotes.\n" +
                               "    Any number of whitespaces allowed before 'field name', and one space separates 'field name' and its value.\n" +
                               "    In value, newline characters should be encoded as \"\\n\",\n" +
                               "    'U+2029 utf-8 E280A9 : PARAGRAPH SEPARATOR PS' should be encoded as \"\\p\",\n" +
                               "    and '\\' characters should be escaped as \"\\\\\".\n" +
                               "    For checkboxes, values are 'Yes'/'Off'.\n\n" +
                               "    Based on the Belgian iText library v. 5.2.0, http://www.itextpdf.com/\n"
                    );
           System.exit(1);
        }

    }

    public static void fillPDFFile(String pdf_filename_in, String pdf_filename_out, String fields_filename){
        fillPDFFile(pdf_filename_in, pdf_filename_out, fields_filename, getDefaultEncoding(), null, "fill", false, false);
    }

    public static void fillPDFFile(String pdf_filename_in, String pdf_filename_out, String fields_filename,
                                   String fields_encoding, String font_file, String op, Boolean flatten, Boolean verbose) {
        OutputStream os;
        PdfStamper stamp;
        try {
            PdfReader reader = new PdfReader(pdf_filename_in);

            if (pdf_filename_out != null) {
                os = new FileOutputStream(pdf_filename_out);
            } else {
                os = System.out;
            }

            stamp = new PdfStamper(reader, os, '\0');

            AcroFields form = stamp.getAcroFields();

            if (op.equals("list")){
                formList(form);
            } else {
                if (font_file != null){
                    BaseFont bf = BaseFont.createFont(font_file, BaseFont.IDENTITY_H, true);
                    form.addSubstitutionFont(bf);
                }
                FormFieldsReader fieldsReader = new FormFieldsReader(getFieldSource(fields_filename, fields_encoding));
                Map<String, String> fields = fieldsReader.read();
                for (Map.Entry<String, String> entry : fields.entrySet()) {
                    if (verbose)
                        System.out.println("Field name = '" + entry.getKey() + "', New field value: '" + entry.getValue() + "'");
                    form.setField(entry.getKey(), entry.getValue());
                }

                stamp.setFormFlattening(flatten);
                stamp.close();
            }
        } catch (FileNotFoundException e) {
            System.err.println("FileNotFoundException: " + e.getMessage());
            System.exit(2);
        } catch (IOException e) {
            System.err.println("Input output error: " + e.getMessage());
            System.exit(3);
        } catch (DocumentException e) {
            System.err.println("Error while processing document: " + e.getMessage());
            System.exit(4);
        }
    }

    private static Readable getFieldSource(String filename, String encoding) throws FileNotFoundException, UnsupportedEncodingException {
        if (filename != null)
            return new InputStreamReader(new FileInputStream(filename), encoding);
        else
            return new InputStreamReader(System.in);
    }

    public static void formList(AcroFields form){
            Map<String, AcroFields.Item> map = form.getFields();
            System.out.println("Field names:");
            for (Map.Entry<String, AcroFields.Item> entry : map.entrySet())
                System.out.println(entry.getKey());
            System.out.println("END: Field names");
    }

    private static String getDefaultEncoding() {
        return Charset.defaultCharset().name();
    }

}