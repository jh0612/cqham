module com.reiwaxr.cq.cqham {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.graphics;
    requires static lombok;
    // SQLite驱动模块名
    requires org.xerial.sqlitejdbc;
    requires deepl.java;
    requires okhttp3;
    requires com.fasterxml.jackson.databind;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires java.desktop;

    opens com.reiwaxr.cq.cqham to javafx.fxml;
    exports com.reiwaxr.cq.cqham;
    exports com.reiwaxr.cq.cqham.controller;
    exports com.reiwaxr.cq.cqham.entity;
    opens com.reiwaxr.cq.cqham.controller to javafx.fxml;
    opens com.reiwaxr.cq.cqham.entity to javafx.base;

}