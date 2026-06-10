module com.reiwaxr.cq.cqham {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.reiwaxr.cq.cqham to javafx.fxml;
    exports com.reiwaxr.cq.cqham;
}