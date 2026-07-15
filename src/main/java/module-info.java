module com.reiwaxr.cq.cqham {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    // SQLite驱动模块名
    requires org.xerial.sqlitejdbc;
    requires deepl.java;

    opens com.reiwaxr.cq.cqham to javafx.fxml;
    exports com.reiwaxr.cq.cqham;
    exports com.reiwaxr.cq.cqham.controller;
    opens com.reiwaxr.cq.cqham.controller to javafx.fxml;
    opens com.reiwaxr.cq.cqham.entity to javafx.base;
}