# -*- coding: utf-8 -*-

from builtins import object
from PyQt5.QtWidgets import QTabWidget, QWidget, QGroupBox, QLabel, QComboBox, QPushButton, QCheckBox, QListWidget, \
    QTableView, QAbstractItemView
from PyQt5.QtWidgets import QTextBrowser, QPlainTextEdit, QLineEdit, QToolButton, QApplication
from PyQt5.QtCore import QRect, QMetaObject
from PyQt5.QtGui import QFont, QStandardItemModel
from qgis._gui import QgsAuthConfigSelect

_fromUtf8 = lambda s: s


class Ui_WCPSClient(object):
    def setupUi(self, WCPSClient):
        WCPSClient.setObjectName(_fromUtf8("WCPSClient"))
        WCPSClient.resize(675, 518)
        self.tabWidget_WCPSClient = QTabWidget(WCPSClient)
        self.tabWidget_WCPSClient.setGeometry(QRect(10, 10, 675, 518))
        self.tabWidget_WCPSClient.setObjectName(_fromUtf8("tabWidget_WCPSClient"))


        self.tab_Serv = QWidget()
        self.tab_Serv.setGeometry(10, 10, 641, 471)
        self.tab_Serv.setObjectName(_fromUtf8("tab_Serv"))
        self.groupBox = QGroupBox(self.tab_Serv)
        self.groupBox.setGeometry(QRect(10, 20, 611, 101))
        self.groupBox.setObjectName(_fromUtf8("groupBox"))
        self.cmbConnections_Serv = QComboBox(self.groupBox)
        self.cmbConnections_Serv.setGeometry(QRect(10, 30, 591, 31))
        self.cmbConnections_Serv.setObjectName(_fromUtf8("cmbConnections_Serv"))
        self.btnConnectServer_Serv = QPushButton(self.groupBox)
        self.btnConnectServer_Serv.setGeometry(QRect(130, 70, 111, 28))
        self.btnConnectServer_Serv.setObjectName(_fromUtf8("btnConnectServer_Serv"))
        self.btnNew_Serv = QPushButton(self.groupBox)
        self.btnNew_Serv.setGeometry(QRect(250, 70, 111, 28))
        self.btnNew_Serv.setObjectName(_fromUtf8("btnNew_Serv"))
        self.btnEdit_Serv = QPushButton(self.groupBox)
        self.btnEdit_Serv.setGeometry(QRect(370, 70, 111, 28))
        self.btnEdit_Serv.setObjectName(_fromUtf8("btnEdit_Serv"))
        self.btnDelete_Serv = QPushButton(self.groupBox)
        self.btnDelete_Serv.setGeometry(QRect(490, 70, 111, 28))
        self.btnDelete_Serv.setObjectName(_fromUtf8("btnDelete_Serv"))
        self.label_2 = QLabel(self.tab_Serv)
        self.label_2.setGeometry(QRect(10, 130, 601, 31))
        font = QFont()
        font.setBold(True)
        font.setItalic(True)
        font.setWeight(75)
        self.label_2.setFont(font)
        self.label_2.setObjectName(_fromUtf8("label_2"))
        self.textBrowser_Serv = QTextBrowser(self.tab_Serv)
        self.textBrowser_Serv.setGeometry(QRect(5, 161, 621, 81))
        self.textBrowser_Serv.setObjectName(_fromUtf8("textBrowser_Serv"))
        self.btnClose_Serv = QPushButton(self.tab_Serv)
        self.btnClose_Serv.setGeometry(QRect(520, 410, 93, 28))
        self.btnClose_Serv.setObjectName(_fromUtf8("btnClose_Serv"))
        self.groupBox_3 = QGroupBox(self.tab_Serv)
        self.groupBox_3.setGeometry(QRect(10, 250, 611, 111))
        self.groupBox_3.setObjectName(_fromUtf8("groupBox_3"))
        self.label_3 = QLabel(self.groupBox_3)
        self.label_3.setGeometry(QRect(10, 30, 61, 16))
        self.label_3.setObjectName(_fromUtf8("label_3"))
        self.label_4 = QLabel(self.groupBox_3)
        self.label_4.setGeometry(QRect(10, 60, 171, 21))
        self.label_4.setObjectName(_fromUtf8("label_4"))
        self.UserNameLine = QLineEdit(self.groupBox_3)
        self.UserNameLine.setGeometry(QRect(80, 30, 511, 22))
        self.UserNameLine.setObjectName(_fromUtf8("UserNameLine"))
        self.PasswordLine = QLineEdit(self.groupBox_3)
        self.PasswordLine.setGeometry(QRect(80, 60, 511, 22))
        self.PasswordLine.setObjectName(_fromUtf8("PasswordLine"))
        self.tabWidget_WCPSClient.addTab(self.tab_Serv, _fromUtf8(""))

        # Visual Query Editor Tab
        self.tab_VisualQuery = QWidget()
        self.tab_VisualQuery.setObjectName(_fromUtf8("tab_VisualQuery"))

        # Input Datacubes
        self.btnAddDatacube = QPushButton(self.tab_VisualQuery)
        self.btnAddDatacube.setGeometry(QRect(10, 10, 231, 28))
        self.btnAddDatacube.setObjectName(_fromUtf8("btnAddDatacube"))

        # Delete Datacubes
        self.btnDeleteDatacube = QPushButton(self.tab_VisualQuery)
        self.btnDeleteDatacube.setGeometry(QRect(390, 10, 231, 28))
        self.btnDeleteDatacube.setObjectName(_fromUtf8("btnDeleteDatacube"))

        # Label datacubes
        self.label_5 = QLabel(self.tab_VisualQuery)
        self.label_5.setGeometry(QRect(10, 40, 211, 16))
        self.label_5.setObjectName(_fromUtf8("label_5"))

        # List of Datacubes
        self.lstDatacubes = QListWidget(self.tab_VisualQuery)
        self.lstDatacubes.setGeometry(QRect(10, 60, 611, 100))
        self.lstDatacubes.setObjectName(_fromUtf8("lstDatacubes"))

        # Filter Datacubes
        self.btnSetFilter = QPushButton(self.tab_VisualQuery)
        self.btnSetFilter.setGeometry(QRect(10, 170, 231, 28))
        self.btnSetFilter.setObjectName(_fromUtf8("btnSetFilter"))

        # Label Result Expression
        self.label_6 = QLabel(self.tab_VisualQuery)
        self.label_6.setGeometry(QRect(10, 200, 241, 16))
        self.label_6.setObjectName(_fromUtf8("label_6"))

        # Result Expression
        self.ResultExpression = QPlainTextEdit(self.tab_VisualQuery)
        self.ResultExpression.setGeometry(QRect(10, 220, 611, 87))
        self.ResultExpression.setObjectName(_fromUtf8("ResultExpression"))

        # Label Result format
        self.label_7 = QLabel(self.tab_VisualQuery)
        self.label_7.setGeometry(QRect(10, 310, 231, 16))
        self.label_7.setObjectName(_fromUtf8("label_7"))

        # Result format
        self.ResultFormat = QComboBox(self.tab_VisualQuery)
        self.ResultFormat.setGeometry(QRect(10, 330, 601, 22))
        self.ResultFormat.setObjectName(_fromUtf8("ResultFormat"))

        # Format parameters
        self.btnFormatParameters = QPushButton(self.tab_VisualQuery)
        self.btnFormatParameters.setGeometry(QRect(10, 390, 231, 28))
        self.btnFormatParameters.setObjectName(_fromUtf8("btnFormatParameters"))

        # Custom format
        self.CustomFormatLine = QLineEdit(self.tab_VisualQuery)
        self.CustomFormatLine.setGeometry(QRect(10, 360, 601, 22))
        self.CustomFormatLine.setObjectName(_fromUtf8("CustomFormatLine"))

        #Evaluate Button
        self.btnEvaluate = QPushButton(self.tab_VisualQuery)
        self.btnEvaluate.setGeometry(QRect(502, 430, 121, 28))
        self.btnEvaluate.setObjectName(_fromUtf8("btnEvaluate"))



        # Add the Visual Query Editor tab to the tab widget
        self.tabWidget_WCPSClient.addTab(self.tab_VisualQuery, _fromUtf8("Visual Query Editor"))

        self.tab_PC = QWidget()
        self.tab_PC.setEnabled(False)
        self.tab_PC.setGeometry(QRect(10, 10, 675, 518))
        self.tab_PC.setObjectName(_fromUtf8("tab_PC"))
        self.groupBox_2 = QGroupBox(self.tab_PC)
        self.groupBox_2.setGeometry(QRect(10, 10, 621, 401))
        self.groupBox_2.setObjectName(_fromUtf8("groupBox_2"))
        self.plainTextEdit_PC = QPlainTextEdit(self.groupBox_2)
        self.plainTextEdit_PC.setGeometry(QRect(10, 30, 601, 361))
        self.plainTextEdit_PC.setObjectName(_fromUtf8("plainTextEdit_PC"))
        self.pushButton_PC = QPushButton(self.tab_PC)
        self.pushButton_PC.setGeometry(QRect(400, 420, 111, 28))
        self.pushButton_PC.setObjectName(_fromUtf8("pushButton_PC"))
        self.btnClose_PC = QPushButton(self.tab_PC)
        self.btnClose_PC.setGeometry(QRect(520, 420, 111, 28))
        self.btnClose_PC.setObjectName(_fromUtf8("btnClose_PC"))
        self.btnLoad_Query = QPushButton(self.tab_PC)
        self.btnLoad_Query.setGeometry(QRect(280, 420, 111, 28))
        self.btnLoad_Query.setObjectName(_fromUtf8("btnSave_Result"))
        self.tabWidget_WCPSClient.addTab(self.tab_PC, _fromUtf8(""))
        self.btnStore_Query = QPushButton(self.tab_PC)
        self.btnStore_Query.setGeometry(QRect(162, 420, 111, 28))
        self.btnStore_Query.setObjectName(_fromUtf8("btnSave_Result"))

        self.tab_CoveragesList = QWidget()
        self.tab_CoveragesList.setEnabled(False)
        self.tab_CoveragesList.setGeometry(QRect(10, 10, 675, 518))
        self.tab_CoveragesList.setObjectName(_fromUtf8("tab_CoveragesList"))
        self.tableView = QTableView(self.tab_CoveragesList)
        self.tableView.setGeometry(20, 20, 620, 450)
        self.tableView.setObjectName(_fromUtf8("tableView"))
        self.model = QStandardItemModel(self.tableView)
        self.tableView.setModel(self.model)
        self.model.setHorizontalHeaderLabels(["Name", "Dim", "Axes", "BBox", "CRS", "Size"])
        self.tableView.setSelectionBehavior(QAbstractItemView.SelectRows)
        self.tabWidget_WCPSClient.addTab(self.tab_CoveragesList, _fromUtf8(""))

        self.tabWidget_WCPSClient.addTab(self.tab_CoveragesList, _fromUtf8(""))

        self.retranslateUi(WCPSClient)
        self.tabWidget_WCPSClient.setCurrentIndex(2)

        self.btnClose_Serv.clicked.connect(WCPSClient.close)
        self.btnClose_PC.clicked.connect(WCPSClient.close)
        self.btnConnectServer_Serv.clicked.connect(WCPSClient.connectServer)
        self.btnNew_Serv.clicked.connect(WCPSClient.newServer)
        self.btnEdit_Serv.clicked.connect(WCPSClient.editServer)
        self.btnDelete_Serv.clicked.connect(WCPSClient.deleteServer)
        self.pushButton_PC.clicked.connect(WCPSClient.exeProcessCoverage)
        self.btnLoad_Query.clicked.connect(WCPSClient.loadQuery)
        self.btnStore_Query.clicked.connect(WCPSClient.storeQuery)
        self.btnAddDatacube.clicked.connect(WCPSClient.addDatacube)
        self.btnDeleteDatacube.clicked.connect(WCPSClient.deleteDatacube)
        self.btnSetFilter.clicked.connect(WCPSClient.setFilterCondition)
        self.btnFormatParameters.clicked.connect(WCPSClient.setFormatParameters)
        self.btnEvaluate.clicked.connect(WCPSClient.evaluateQuery)
        QMetaObject.connectSlotsByName(WCPSClient)

    def retranslateUi(self, WCPSClient):
        WCPSClient.setWindowTitle(QApplication.translate("WCPSClient", "WCPS datacube query", None))
        self.groupBox.setTitle(QApplication.translate("WCPSClient", "Server Connections:", None))
        self.btnConnectServer_Serv.setText(QApplication.translate("WCPSClient", "Connect", None))
        self.btnNew_Serv.setText(QApplication.translate("WCPSClient", "New", None))
        self.btnEdit_Serv.setText(QApplication.translate("WCPSClient", "Edit", None))
        self.btnDelete_Serv.setText(QApplication.translate("WCPSClient", "Delete", None))
        self.label_2.setText(QApplication.translate("WCPSClient", "Log", None))
        self.groupBox_3.setTitle(QApplication.translate("WCPSClient", "Authentication", None))
        self.label_3.setText(QApplication.translate("WCPSClient", "Username", None))
        self.label_4.setText(QApplication.translate("WCPSClient", "Password", None))
        self.btnClose_Serv.setText(QApplication.translate("WCPSClient", "Close", None))
        self.tabWidget_WCPSClient.setTabText(self.tabWidget_WCPSClient.indexOf(self.tab_Serv),
                                             QApplication.translate("WCPSClient", "Server", None))
        self.groupBox_2.setTitle(QApplication.translate("WCPSClient", "Queries Text ", None))
        self.pushButton_PC.setText(QApplication.translate("WCPSClient", "Submit", None))
        self.btnClose_PC.setText(QApplication.translate("WCPSClient", "Close", None))
        self.btnLoad_Query.setText(QApplication.translate("WCPSClient", "Load Query", None))
        self.tabWidget_WCPSClient.setTabText(self.tabWidget_WCPSClient.indexOf(self.tab_PC),
                                             QApplication.translate("WCPSClient", "WCPS query editor", None))
        self.btnStore_Query.setText(QApplication.translate("WCPSClient", "Store Query", None))
        self.tabWidget_WCPSClient.setTabText(self.tabWidget_WCPSClient.indexOf(self.tab_CoveragesList),
                                             QApplication.translate("WCPSClient", "Coverages List", None))

        self.tab_VisualQuery.setWindowTitle(QApplication.translate("WCPSClient", "Visual Query Editor", None))
        self.btnAddDatacube.setText(QApplication.translate("WCPSClient", "Add datacube", None))
        self.btnDeleteDatacube.setText(QApplication.translate("WCPSClient", "Delete datacube", None))
        self.label_5.setText(QApplication.translate("WCPSClient", "Selected Datacubes", None))
        self.btnSetFilter.setText(QApplication.translate("WCPSClient", "Set filter condition", None))
        self.label_6.setText(QApplication.translate("WCPSClient", "Result expression", None))
        self.label_7.setText(QApplication.translate("WCPSClient", "Result format", None))
        self.CustomFormatLine.setText(QApplication.translate("WCPSClient", "Custom format", None))
        self.btnFormatParameters.setText(QApplication.translate("WCPSClient", "Format parameters", None))
        self.btnEvaluate.setText(QApplication.translate("WCPSClient", "Evaluate", None))




