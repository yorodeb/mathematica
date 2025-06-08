import java.io.File;
import net.sourceforge.tess4j.*;

public class TextExtract{
    private String extractedText;

    public String perform(String ImagePath){
        File ImageFile = new File(ImagePath);
        ITesseract tesseract = new Tesseract();

        tesseract.setDatapath("C:\\Tess4J\\tessdata");
        tesseract.setLanguage("eng+equ"); 
        tesseract.setTessVariable("load_system_dawg", "false");
        tesseract.setTessVariable("load_freq_dawg", "false");
        this.extractedText = "";

        try{
            if(!ImageFile.exists()){
                System.err.println("File::Error");
                this.extractedText = "File::Error";
            } else{
                this.extractedText = tesseract.doOCR(ImageFile);
            }
        } catch(TesseractException except){
            System.err.println(except.getMessage());
        }
        return this.extractedText;
    }
    
    public String getExtractedText(){
        return this.extractedText;
    }
    
    TextExtract(String ImagePath){
        perform(ImagePath);
    }
}
    
