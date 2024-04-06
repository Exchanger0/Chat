module client {
    exports client to javafx.graphics;
    exports client.elements to javafx.graphics;

    requires shared;
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.controls;
}