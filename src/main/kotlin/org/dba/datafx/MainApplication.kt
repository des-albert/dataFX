package org.dba.datafx

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage

class MainApplication : Application() {

    override fun start(stage: Stage) {

        MongoManage.connect()

        val fxmlLoader = FXMLLoader(MainApplication::class.java.getResource("main.fxml"))
        val scene = Scene(fxmlLoader.load(), 1100.0, 700.0)
        val icon = Image(javaClass.getResourceAsStream("/img/dataFX.png"))
        stage.icons.add(icon)
        stage.title = "dataFX"
        stage.scene = scene
        stage.show()
    }
}


fun main() {
    Application.launch(MainApplication::class.java)
}

