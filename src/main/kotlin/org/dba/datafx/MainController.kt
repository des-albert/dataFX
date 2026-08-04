package org.dba.datafx

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Projections
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.javafx.JavaFx
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption


class MainController {

    @FXML
    lateinit var fileColumn: TableColumn<UcidDetails, String>

    @FXML
    lateinit var exportDateColumn: TableColumn<UcidDetails, String>

    @FXML
    lateinit var ucidColumn: TableColumn<UcidDetails, String>

    @FXML
    lateinit var ucidTableView: TableView<UcidDetails>

    @FXML
    lateinit var buttonDelete: Button

    @FXML
    lateinit var buttonStore: Button

    @FXML
    lateinit var buttonQuit: Button

    @FXML
    lateinit var buttonLoad: Button

    @FXML
    lateinit var buttonDetete: Button

    @FXML
    lateinit var opeSearchButton: Button


    @FXML
    lateinit var yearGroup: ToggleGroup

    @FXML
    lateinit var radio2024: RadioButton

    @FXML
    lateinit var radio2026: RadioButton

    @FXML
    lateinit var radio2025: RadioButton

    @FXML
    lateinit var opeTextField: TextField

    @FXML
    lateinit var textFieldDate: TextField

    @FXML
    lateinit var textFieldProduct: TextField

    @FXML
    lateinit var textFieldFileName: TextField

    @FXML
    lateinit var textFieldOPE: TextField

    @FXML
    lateinit var textFieldCustomer: TextField

    @FXML
    lateinit var textFieldUCID: TextField

    @FXML
    lateinit var textAreaResult: TextArea

    @FXML
    lateinit var tabPane: TabPane

    @FXML
    lateinit var tabMain: Tab

    @FXML
    lateinit var tabOpportunity: Tab

    @FXML
    lateinit var labelJavaFX: Label

    @FXML
    lateinit var labelJDK: Label


    private val controllerScope = CoroutineScope(Dispatchers.JavaFx + SupervisorJob())

    var ucidFileName: String = ""
    var ucidDirName: String = ""
    var currentYear: String = "2026"
    val archiveRootPath = "C:\\Users\\albertd\\OneDrive - Hewlett Packard Enterprise\\HPE\\Configs\\"
    var archiveYearPath = archiveRootPath + currentYear
    val downloadPath: Path = Paths.get("C:\\Users\\albertd\\Downloads")

    var products: List<String> = emptyList()

    companion object {
        val logger: Logger = LoggerFactory.getLogger("dataFX")
    }

    @FXML
    fun initialize() {

        MongoManage.select(currentYear)

        labelJDK.text = "Java SDK version: ${Runtime.version()}"
        labelJavaFX.text = "JavaFX version: ${System.getProperty("javafx.runtime.version")}"

        ucidColumn.cellValueFactory = PropertyValueFactory("ucid")
        exportDateColumn.cellValueFactory = PropertyValueFactory("exportDate")
        fileColumn.cellValueFactory = PropertyValueFactory("fileName")

    }

    @FXML
    fun yearSelect() {
        val selectedToggle = yearGroup.selectedToggle
        var selectedYear = ""
        if (selectedToggle != null) {
            val selectedRadio = selectedToggle as RadioButton
            selectedYear = selectedRadio.text
        }
        if (selectedYear != currentYear) {
            currentYear = selectedYear


            MongoManage.select(selectedYear)
            archiveYearPath = archiveRootPath + currentYear

            controllerScope.launch {
                val count = withContext(Dispatchers.IO) {
                    MongoManage.collection.countDocuments()

                }
                textAreaResult.text = "Year $currentYear selected count: $count"
            }
        }

    }

    @FXML
    fun loadData() {
        val ucidToFind = textFieldUCID.text
        if (ucidToFind.isBlank()) {
            textAreaResult.text = "Please enter a UCID to load."
            return
        }
        controllerScope.launch {

            val config = withContext(Dispatchers.IO) {
                MongoManage.collection.find(eq("ucid", ucidToFind)).firstOrNull()
            }
            if (config != null) {
                textFieldOPE.text = config.ope
                textFieldProduct.text = config.product
                textFieldFileName.text = config.fileName
                textFieldCustomer.text = config.customer
                textFieldDate.text = config.exportDate

                textAreaResult.text = "Successfully loaded data for customer: ${config.customer}"

                ucidDirName = config.fileName.split("\\").first()
                ucidFileName = config.fileName.split("\\").last()

            } else {
                textFieldOPE.clear()
                textFieldProduct.clear()
                textFieldFileName.clear()
                textFieldCustomer.clear()
                textFieldDate.clear()

                textAreaResult.text = "No data found for UCID: $ucidToFind"
            }
        }
    }

    @FXML
    fun deleteData() {
        val ucidToDelete = textFieldUCID.text
        if (ucidToDelete.isBlank()) {
            textAreaResult.text = "Please enter a UCID to load."
            return
        }
        controllerScope.launch {

            withContext(Dispatchers.IO) {

                val config = MongoManage.collection.find(eq("ucid", ucidToDelete)).firstOrNull()
                if (config != null) {
                    val fileToDelete = config.fileName


                    deleteFileFromArchive(fileToDelete, archiveYearPath)
                    MongoManage.collection.findOneAndDelete(eq("ucid", ucidToDelete))

                    textAreaResult.text = "Successfully deleted data for UCID: $ucidToDelete"

                } else {
                    textAreaResult.text = "No data found for UCID: $ucidToDelete"
                }
            }
        }
    }

    private fun deleteFileFromArchive(fileName: String, dirName: String) {
        try {
            val filePath = Paths.get(dirName, fileName)
            Files.deleteIfExists(filePath)
        } catch (e: IOException) {
            logger.error("Failed to delete file: $fileName", e)
        }
    }

    @FXML
    fun storeConfigData() {
        buttonStore.isDisable = true
        textAreaResult.text = "Starting to process files...\n"

        controllerScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    processExcelFiles()
                }
                textAreaResult.appendText("Finished processing all files.\n")
            } catch (e: Exception) {
                logger.error("A critical error occurred during file processing.", e)
                textAreaResult.appendText("ERROR: An unexpected error occurred. Check logs.\n")
            } finally {
                buttonStore.isDisable = false
            }
        }
    }


    private suspend fun processExcelFiles() {

        products = loadProductList()

        if (!Files.isDirectory(downloadPath)) {
            logger.error("Downloads directory not found: $downloadPath")
            withContext(Dispatchers.JavaFx) { textAreaResult.appendText("ERROR: Downloads directory not found.\n") }
            return
        }

        val excelFiles = findExcelFiles(downloadPath)
        withContext(Dispatchers.JavaFx) { textAreaResult.appendText("Found ${excelFiles.size} Excel files to process.\n") }

        for (filePath in excelFiles) {
            try {
                val (newConfig, dirName) = parseConfigFromFile(filePath) ?: continue

                MongoManage.collection.insertOne(newConfig)
                logger.info("Successfully inserted config from: ${newConfig.fileName}")

                moveFileToArchive(filePath, dirName)

                withContext(Dispatchers.JavaFx) {
                    textAreaResult.appendText("Processed and archived: ${filePath.fileName}\n")
                }
            } catch (e: Exception) {
                logger.error("Failed to process file $filePath", e)
                withContext(Dispatchers.JavaFx) {
                    textAreaResult.appendText("ERROR processing file: ${filePath.fileName}. See logs.\n")
                }
            }
        }
    }

    private fun loadProductList(): List<String> {
        val fileName = "/productList.txt"
        return try {
            val inputStream = MainController::class.java.getResourceAsStream(fileName)
                ?: throw IOException("Resource not found: $fileName")
            inputStream.bufferedReader().use { it.readLines() }

        } catch (e: IOException) {
            logger.error("Failed to load search list from file $fileName", e)
            return emptyList()
        }
    }

    private fun loadProducts() {
        controllerScope.launch {
            products = withContext(Dispatchers.IO) {
                MongoManage.collection.distinct<String>("product", Filters.ne("product", null)).toList().sorted()
            }

        }
    }


    private fun findExcelFiles(directoryPath: Path): List<Path> {
        return try {
            Files.walk(directoryPath, 1)
                .filter {
                    Files.isRegularFile(it) && (it.toString().endsWith(".xlsx") || it.toString().endsWith(".xls"))
                }
                .toList()
        } catch (e: IOException) {
            logger.error("Error walking directory $directoryPath", e)
            emptyList()
        }
    }

    private fun moveFileToArchive(sourcePath: Path, dirName: String) {
        try {
            val archiveDir = Paths.get(archiveYearPath, dirName)
            Files.createDirectories(archiveDir)
            val destinationPath = archiveDir.resolve(sourcePath.fileName)
            Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            logger.error("Failed to archive file: $sourcePath", e)
        }
    }

    private fun parseConfigFromFile(filePath: Path): Pair<Config, String>? {
        try {
            FileInputStream(filePath.toFile()).use { fis ->
                XSSFWorkbook(fis).use { workbook ->
                    val sheet = workbook.getSheetAt(0)
                    val firstCell = sheet.getRow(0)?.getCell(0)

                    // Guard Clause: Exit early if not a valid config file
                    if (firstCell?.cellType != CellType.STRING || !firstCell.stringCellValue.startsWith("Config")) {
                        return null
                    }

                    val headerWords = firstCell.stringCellValue.split(" ")
                    val product = headerWords.getOrNull(3) ?: "Unknown"
                    val dirName = headerWords.getOrNull(2) ?: "Misc"

                    // Using safe calls (?.) and elvis operators (?:) to prevent NullPointerExceptions
                    val ucid = sheet.getRow(1)?.getCell(0)?.stringCellValue?.split(" ")?.getOrNull(1) ?: ""
                    val exportDate = sheet.getRow(2)?.getCell(0)?.stringCellValue?.split(" ")?.getOrNull(2) ?: ""
                    val ope = sheet.getRow(3)?.getCell(0)?.stringCellValue?.substringAfter(": ") ?: ""
                    val customer = sheet.getRow(4)?.getCell(0)?.stringCellValue?.substringAfter(": ") ?: ""
                    val fileName = dirName + "\\" + filePath.fileName.toString()

                    val archiveDir = Paths.get(archiveYearPath, dirName)
                    val isUnique = if (Files.isDirectory(archiveDir)) "0" else "1"

                    if (!products.contains(product)) {
                        logger.error("Invalid product $product in $fileName")
                        return null
                    }

                    // Return a new, immutable Config object
                    val config = Config(
                        product = product, ucid = ucid, exportDate = exportDate, ope = ope,
                        customer = customer, fileName = fileName, unique = isUnique
                    )
                    return config to dirName
                }
            }
        } catch (e: IOException) {
            logger.error("Could not read or parse Excel file: $filePath", e)
            return null
        }
    }

    @FXML
    fun searchByOpe() {
        val opeToFind = opeTextField.text

        if (opeToFind.isNullOrBlank()) {
            ucidTableView.placeholder = Label("Please enter an OPE to search")
            return
        }

        opeSearchButton.isDisable = true
        ucidTableView.items.clear()
        ucidTableView.placeholder = Label("Loading details for OPE: $opeToFind...")

        controllerScope.launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    fetchUcidDetailsForOpe(opeToFind)
                }
                ucidTableView.items = FXCollections.observableArrayList(results)
                if (results.isEmpty()) {
                    ucidTableView.placeholder = Label("No data found for OPE: $opeToFind.")
                }
            } catch (e: Exception) {
                logger.error("Failed to fetch details for OPE: $opeToFind", e)
                ucidTableView.placeholder = Label("Error loading details. See logs for details.")
            } finally {
                opeSearchButton.isDisable = false
            }
        }

    }

    private suspend fun fetchUcidDetailsForOpe(ope: String): List<UcidDetails> {
        val mongoResults = MongoManage.collection
            .withDocumentClass<UcidDetails>()
            .find(eq("ope", ope))
            .projection(
                Projections.fields(
                    Projections.include("ucid", "exportDate","fileName"),
                    Projections.excludeId()
                )
            )
            .toList()
        return mongoResults.map { result ->
            UcidDetails(result.ucid, result.exportDate, result.fileName)
        }.sortedByDescending { it.exportDate }
    }


    @FXML
    fun quit() {
        Platform.exit()

    }


}