import java.io.File;
import net.sourceforge.tess4j.*;

public class TextExtract{
    private String extractedText; //Resultant Text after Extraction.

		/*perform() --> Method performs OCR on given images.
		 * -- returns <string> 'extractedText'*/
    public String perform(String ImagePath){
        File ImageFile = new File(ImagePath);
        ITesseract tesseract = new Tesseract();
 
        tesseract.setDatapath("C:\\Tess4J\\tessdata"); //getting dataset for tesseract
        tesseract.setLanguage("eng+equ"); //configuring for reading equations
        tesseract.setTessVariable("load_system_dawg", "false"); //optimizing for mathematical expressions
        tesseract.setTessVariable("load_freq_dawg", "false");
        this.extractedText = "";

        try{
						//Validating - ImagePath Exists.
            if(!ImageFile.exists()){
                System.err.println("File::Error");
                this.extractedText = "File::Error";
            } else{
                this.extractedText = tesseract.doOCR(ImageFile); //performing OCR..
            }
        } catch(TesseractException except){
            System.err.println(except.getMessage());
        }
        return this.extractedText;
    }
    
		//getExtractedText() --> Getter Method for `private variable` -> extractedText
    public String getExtractedText(){
        return this.extractedText;
    }
    
		/*TextExtract() --> Constructor for Class -- 'TextExtract'
		 * -- calls the 'perform()' method.*/
    TextExtract(String ImagePath){
        perform(ImagePath);
    }
}
    
