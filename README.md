# Mathematica Converter

A Java-based desktop application for extracting text from images (using Tess4J) and managing mathematical queries and solutions in a database.

## Features:

* **Text Extraction:** Upload images to extract mathematical expressions or text using Tess4J OCR.
* **Search Functionality:** Input mathematical queries and save them to a database.
* **History Tracking:** View a chronological history of all image uploads and text queries.
* **Dark-Themed UI:** A modern and intuitive user interface.
* **Database Integration:** Stores history data using MySQL.

## Technologies Used:

* **Java Swing:** For the graphical user interface.
* **Tess4J:** Java wrapper for Tesseract OCR engine for text extraction.
* **MySQL:** Database for storing history.
* **JDBC:** Java Database Connectivity for interacting with MySQL.

### 1. Prerequisites:

* **Java Development Kit (JDK) 11 or higher:** [Download JDK](https://www.oracle.com/java/technologies/downloads/)
* **MySQL Server:** [Download MySQL Community Server](https://dev.mysql.com/downloads/mysql/)
* **MySQL JDBC Driver (Connector/J):** [Download Connector/J](https://dev.mysql.com/downloads/connector/j/) (Place the `.jar` file in your project's `lib` directory).
* **Tesseract OCR Engine:** [Install Tesseract](https://tesseract-ocr.github.io/tessdoc/Installation.html)
* **Tess4J Library:** [Download Tess4J](https://sourceforge.net/projects/tess4j/files/) (Extract and copy all `.jar` files from its `lib` directory into your project's `lib` directory).

### 2. Database Setup:

1.  **Create Database:** Open your MySQL client (e.g., MySQL Workbench, command line) and create a database named `mathematica`:
    ```sql
    CREATE DATABASE mathematica;
    USE mathematica;
    ```
2.  **Create Table:** Create the `HISTORY` table:
    ```sql
    CREATE TABLE HISTORY (
        id INT AUTO_INCREMENT PRIMARY KEY,
        FilePath VARCHAR(255) NOT NULL,
        Question TEXT NOT NULL,
        Graph_Plotted BOOLEAN NOT NULL,
        creation_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
    ```
3.  **Update Credentials:** In `CRUD.java`, update the MySQL username and password if they are different from `root` and `dedakira`:
    ```java
    // In CRUD.java
    crudManager = new CRUD("your_mysql_username", "your_mysql_password");
    ```

### 3. Project Setup:

1.  **Clone Repository:** Clone this GitHub repository to your local machine:
    ```bash
    git clone <your-repo-url>
    cd MathematicaConverter
    ```
2.  **Create `lib` Directory:** If it doesn't exist, create a `lib` directory inside your project's root folder (`MathematicaConverter/lib`).
3.  **Place JARs:**
    * Place the `mysql-connector-j-<version>.jar` file into the `lib` directory.
    * Place all `.jar` files from the downloaded Tess4J `lib` folder into your project's `lib` directory.
4.  **Tessdata Path:** In `TextExtract.java`, **ensure the `tesseract.setDatapath()` method points to your actual Tesseract `tessdata` directory** (e.g., `C:\\Tess4J\\tessdata`):
    ```java
    // In TextExtract.java
    tesseract.setDatapath("C:\\path\\to\\your\\tesseract\\tessdata");
    ```
