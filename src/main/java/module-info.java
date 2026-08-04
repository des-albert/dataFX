module org.dba.datafx {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.mongodb.driver.kotlin.coroutine;
    requires kotlinx.coroutines.core;
    requires kotlinx.coroutines.javafx;
    requires org.slf4j;

    opens org.dba.datafx to javafx.fxml;
    exports org.dba.datafx;
}