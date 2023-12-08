from PyQt5.QtCore import Qt
from PyQt5.QtWidgets import QDialog, QTableWidget, QTableWidgetItem, QVBoxLayout, QPushButton, QLineEdit


class CoverageSelectionDialog(QDialog):
    def __init__(self, coverages, parent=None):
        super(CoverageSelectionDialog, self).__init__(parent)
        self.setWindowTitle("Select Coverages")

        self.resize(600, 400)
        self.layout = QVBoxLayout(self)
        self.input_field = QLineEdit(self)
        self.layout.addWidget(self.input_field)
        self.table = QTableWidget(self)
        self.layout.addWidget(self.table)

        self.ok_button = QPushButton("OK", self)
        self.cancel_button = QPushButton("Cancel", self)
        self.ok_button.clicked.connect(self.accept)
        self.cancel_button.clicked.connect(self.reject)
        self.layout.addWidget(self.ok_button)
        self.layout.addWidget(self.cancel_button)

        self.table.setColumnCount(1)
        self.table.setHorizontalHeaderLabels(["Coverages"])
        self.table.setRowCount(len(coverages))
        self.table.horizontalHeader().setStretchLastSection(True)
        for i, coverage in enumerate(coverages):
            item = QTableWidgetItem(coverage)
            item.setFlags(item.flags() | Qt.ItemIsUserCheckable)
            item.setCheckState(Qt.Unchecked)
            self.table.setItem(i, 0, item)

    def getSelectedCoverages(self):
        selected_coverages = []
        # selected_coverages.append(self.input_field.text())
        itervar = self.input_field.text()

        for i in range(self.table.rowCount()):
            if self.table.item(i, 0).checkState() == Qt.Checked:
                selected_coverages.append(self.table.item(i, 0).text())

        return selected_coverages, itervar
