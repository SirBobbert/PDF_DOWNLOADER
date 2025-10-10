# PDF Downloader

## Description

The PDF Downloader is a Java application that allows users to download a list of PDF files from specified URLs in an
Excel file.

The application reads an Excel file containing the URLs, downloads each PDF, and saves them to a designated directory on
the user's computer.

Furthermore, the application generates a new Excel file that lists the names of the downloaded PDFs along with their
corresponding URLs for easy reference.

Additionally the generarted Excel file includes the ID (BRnum), the URL used, a message if it was the first or
alternative URl that was being used, status (success/error), reason for failure and error message.

In the project, the code is well-documented with comments to enhance readability and maintainability.

The dataset is located in the `src/main/java/org/example/util` directory, and the downloaded PDFs are saved in the
`download`

## Features

- Reads URLs from an Excel file
- Downloads PDF files from the provided URLs
- Saves downloaded PDFs to a specified directory
- Generates a new Excel file listing the names of downloaded PDFs and their URLs
- Handles errors and logs download status
- Supports alternative URLs if the primary URL fails
- Configurable download directory and input/output file paths
- Multi-threaded downloading for improved performance
- Progress tracking and reporting
- Logging and reporting features for monitoring download status
- Skips duplicate entries in the output Excel file
- Comprehensive code comments/documentation for improved readability

## Technologies

- Java
- Apache POI (for Excel file handling)
- Java's built-in libraries for networking and file handling
- Java Concurrency (for multi-threading)
- Maven (for project management and dependency handling)
- JUnit (for testing)
- SLF4J (for logging)

## Requirements/Dependencies

- Java Development Kit (JDK) 21
- Apache POI library
- Maven (for building the project)
- Internet connection (for downloading PDFs)

## Installation & Running

1. Clone the repository:
   ```bash
   git clone https://github.com/SirBobbert/PDF_DOWNLOADER.git
    ```
2. Navigate to the project directory:
3. ```bash
   cd PDF_DOWNLOADER
   ```
4. Build the project using Maven:
   ```bash
   mvn clean install
   ```
5. Run the application:
   ```bash
    java -jar target/PDF_DOWNLOADER-1.0-SNAPSHOT.jar
    ```
6. Follow the prompts to provide the input Excel file path, output directory, and output Excel file path.
7. The application will read the URLs from the input Excel file, download the PDFs, and save them to the specified
   directory. It will also generate a new Excel file listing the downloaded PDFs and their URLs.
8. Check the output directory for the downloaded PDFs and the generated Excel file.
9. Review the console output for any errors or status messages.
10. Enjoy using the PDF Downloader!

## Usage Example

1. Prepare an Excel file (`input.xlsx`) with a list of URLs in the first column.
2. Run the application and provide the path to `input.xlsx` in `src/main/java/org/example/app/Main.java`
3. Specify the directory where you want to save the downloaded PDFs (e.g., `C:\Downloads\PDFs`).
4. Provide the path for the output Excel file (e.g., `output.xlsx`).
5. The application will download the PDFs and create `output.xlsx` with the download details.

## Project Structure

- `src/main/java/org/example/app`: Contains the main application class (`Main.java`) that initiates the download
  process and also handles creation of threads.
- `src/main/java/org/example/domain`: Contains domain classes representing the data structures used in the application.
- `src/main/java/org/example/service/downloader`: Contains services responsible for downloading PDFs.
- `src/main/java/org/example/service/reader`: Contains services for reading URLs from the input Excel file.
- `src/main/java/org/example/service/report`: Contains services for generating the output Excel report.
- `src/main/java/org/example/util`: Contains the dataset (input Excel file) and may hold test data.
- `pom.xml`: Maven configuration file that manages project dependencies and build settings.

## UML Diagram

(WIP)

![UML Diagram]()

## Requirement Specification

(WIP)

![Requirement Specification]()

## Tests

The project includes unit tests to ensure the functionality of key components. Tests are located in the `src/test/java`

## Limitations/Future work

- Currently, the application does not support resuming interrupted downloads. Future versions could implement this
  feature.
- Implement a graphical user interface (GUI) for easier interaction.
- Add support for other file formats (e.g., DOCX, TXT).
- Exceptions based on:
    - Network issues
    - PDF is too large
    - PDF is encrypted
    - URL is corrupted

## Authors

Robert Pallesen - [GitHub](github.com/SirBobbert)

## License

This project is licensed under the MIT License - see

the [LICENSE](https://github.com/SirBobbert/PDF_DOWNLOADER/blob/main/LICENSE) file for details.

## Version History

- 0.0.1
    - Initial release with core functionality for downloading PDFs from URLs in an Excel file and generating a report.

## Acknowledgments

- Copilot for helping with code snippets and suggestions.
- ChatGPT for helping with my understanding of threads and concurrency in Java.
- baeldung.com for understanding POI and Excel handling in Java.
- Coffee for keeping me awake during troubleshooting and writing comments.
