# -*- coding: utf-8 -*-

from __future__ import print_function

import urllib.request
from base64 import b64encode
from builtins import range
import os, sys, pickle
from glob import glob

from qgis.core import *
from qgis.gui import *

from PyQt5.QtWidgets import QProgressDialog, QDialog, QMessageBox, QFileDialog, QApplication, QPushButton, QInputDialog
from PyQt5.QtGui import QCursor, QStandardItemModel, QStandardItem
from PyQt5.QtNetwork import QNetworkRequest, QNetworkAccessManager
from PyQt5.QtCore import Qt, QFileInfo
from PyQt5 import QtXml
import xmltodict

from .CoveragesSelectionDialog import CoverageSelectionDialog
from .succesful_query_dialog import succesful_query_dialog
from .wcps_client_dialog_base import Ui_WCPSClient
from .qgsnewhttpconnectionbasedialog import qgsnewhttpconnectionbase
from .display_txtdialog import display_txt
import xml.etree.ElementTree as ET

from .wcps_client_utilities import WCPSUtil

# global setttings and saved server list
global config
from . import config

global mode


# ---------------
# running clock icon
def mouse_busy(function):
    """
        set the mouse icon to show clock
    """

    def new_function(self):
        """
            set the mouse icon to show clock
        """
        QApplication.setOverrideCursor(QCursor(Qt.WaitCursor))
        function(self)
        QApplication.restoreOverrideCursor()

    return new_function


# ---------------
# provide a pop-up warning message
def warning_msg(msg):
    """
        present a message in a popup dialog-box
    """
    msgBox = QMessageBox()
    msgBox.setText(msg)
    msgBox.addButton(QPushButton('OK'), QMessageBox.YesRole)
    msgBox.exec_()


# ---------------


class WCPSClientDialog(QDialog, Ui_WCPSClient):
    global mode
    global selected_server
    global selected_server_url
    global coverages
    global filterCondition
    global formatParameters
    global selected_coverages
    global variables
    saveQuerySignal = QtCore.pyqtSignal()
    selected_coverages = []
    variables = []

    def __init__(self, iface):
        global formatParameters
        global filterCondition
        """Constructor."""
        QDialog.__init__(self)

        # Set up the user interface from Designer.
        # After setupUI you can access any designer object by doing
        # self.<objectname>, and you can use autoconnect slots - see
        # http://qt-project.org/doc/qt-4.8/designer-using-a-ui-file.html
        # #widgets-and-dialogs-with-auto-connect

        self.setupUi(self)
        self.iface = iface
        print(config.srv_list)
        if len(config.srv_list['servers']) > 0:
            self.btnEdit_Serv.setEnabled(True)
            self.btnDelete_Serv.setEnabled(True)
            self.updateServerListing()

        self.myWCPS = WCPSUtil()
        self.tabWidget_WCPSClient.setCurrentIndex(0)
        global mode
        mode = ""
        formatParameters = ""
        filterCondition = ""
        flags = Qt.WindowTitleHint | Qt.WindowSystemMenuHint | Qt.WindowMinimizeButtonHint | Qt.WindowMaximizeButtonHint
        self.dlg = succesful_query_dialog(self, flags, toEdit=False, choice='')
        self.dlg.saveQuerySignal.connect(self.saveQuery)
        self.dlg.showQuerySignal.connect(self.showQuery)
        self.dlg.saveAndShowQuerySignal.connect(self.saveAndShowQuery)
        self.ResultFormat.addItems(["none", "png", "jpg", "tiff", "netcdf", "json", "custom"])

    # ---------------
    # add a new server to the list
    def newServer(self):
        global config

        # print('btnNew: I am adding a New ServerName/URL')
        flags = Qt.WindowTitleHint | Qt.WindowSystemMenuHint | Qt.WindowMinimizeButtonHint | Qt.WindowMaximizeButtonHint
        dlgNew = qgsnewhttpconnectionbase(self, flags, toEdit=False, choice='')
        dlgNew.show()
        self.btnConnectServer_Serv.setFocus(True)

    ##TODO -- sort the srv_list

    # ---------------
    # read the selected server/url params
    def get_serv_url(self):
        global serv

        sel_serv = self.cmbConnections_Serv.currentText()
        idx = serv.index(sel_serv)
        sel_url = config.srv_list['servers'][idx][1]
        return sel_serv, sel_url

    def basic_auth(self, username, password):
        token = b64encode(f"{username}:{password}".encode('utf-8')).decode("ascii")
        return f'Basic {token}'

    # ---------------
    # check if the url exist and if we get a respond to a simple OWS request
    @mouse_busy
    def connectServer(self):
        global config
        global serv

        selected_serv, selected_url = self.get_serv_url()
        print(selected_serv, "URL:", selected_url)

        msg = "Selected server:    " + selected_serv + "\n"
        msg = msg + "URL:                   " + selected_url + "\n"
        self.textBrowser_Serv.setText(msg)

        # Send GetCapabilities request to get the coverages list
        capabilities_url = selected_url + "?service=WCS&version=2.0.1&request=GetCapabilities"
        username, password = self.get_auth_credentials()

        try:
            if username != "" and password != "":
                response = urllib.request.urlopen(urllib.request.Request(capabilities_url, headers={
                    'Authorization': self.basic_auth(username=username, password=password)}))
            else:
                response = urllib.request.urlopen(urllib.request.Request(capabilities_url))

            data = response.read()
            if data is not None:
                self.handleGetCapabilitiesResponse(data)
            else:
                warning_msg("Error retrieving coverages list: No data received.")
        except Exception as e:
            warning_msg("Error retrieving coverages list: " + str(e))

        if not self.tab_PC.isEnabled():
            self.tab_PC.setEnabled(True)

        QApplication.changeOverrideCursor(Qt.ArrowCursor)

    def handleGetCapabilitiesResponse(self, data):
        global coverages
        try:
            if data is None:
                warning_msg("Error retrieving coverages list: No data received.")
                return

            try:
                dom = xmltodict.parse(data)
                if 'wcs:Capabilities' in dom and 'wcs:Contents' in dom['wcs:Capabilities'] and 'wcs:CoverageSummary' in \
                        dom['wcs:Capabilities']['wcs:Contents']:
                    coverages = dom['wcs:Capabilities']['wcs:Contents']['wcs:CoverageSummary']

                    # Create a QStandardItemModel and set it as the model for the tableView
                    model = QStandardItemModel(self.tableView)
                    self.tableView.setModel(model)

                    # Set the column headers for the tableView
                    model.setHorizontalHeaderLabels(["Name", "Dim", "Axes", "BBox", "CRS", "Size"])

                    # Add coverage information to the tableView
                    for coverage in coverages:
                        if 'wcs:CoverageId' in coverage:
                            name = QStandardItem(coverage['wcs:CoverageId'])

                            # Extracting values for dim, axes, bbox, crs, and size
                            bbox = coverage.get('ows:BoundingBox', {})
                            lower_corner = bbox.get('ows:LowerCorner', '') if isinstance(bbox, dict) else ''
                            upper_corner = bbox.get('ows:UpperCorner', '') if isinstance(bbox, dict) else ''
                            dim = bbox.get('@dimensions', '') if isinstance(bbox, dict) else ''
                            crs = bbox.get('@crs', '') if isinstance(bbox, dict) else ''

                            axes_param = next((param for param in coverage.get('ows:AdditionalParameters', {}).get(
                                'ows:AdditionalParameter', []) if param.get('ows:Name') == 'axisList'), None)
                            axes = axes_param.get('ows:Value', '') if axes_param else ''

                            size_in_bytes_param = next((param for param in
                                                        coverage.get('ows:AdditionalParameters', {}).get(
                                                            'ows:AdditionalParameter', []) if
                                                        param.get('ows:Name') == 'sizeInBytes'), None)
                            size_in_bytes = size_in_bytes_param.get('ows:Value', '') if size_in_bytes_param else ''

                            try:
                                size_gb = f"{float(size_in_bytes) / 1e9:.2f} GB" if size_in_bytes else "Unknown"
                            except ValueError:
                                size_gb = "Unknown"  # Set a default value or error message for size if it's not a valid number

                            model.appendRow([
                                name,
                                QStandardItem(dim),
                                QStandardItem(axes),
                                QStandardItem(f"{lower_corner} {upper_corner}"),
                                QStandardItem(crs),
                                QStandardItem(size_gb)
                            ])

                    self.tab_CoveragesList.setEnabled(True)

                    # Resize the columns to fit the contents
                    self.tableView.resizeColumnToContents(1)  # Dim column
                    self.tableView.resizeColumnToContents(2)  # Axes column
                    self.tableView.resizeColumnToContents(5)  # Size column

                else:
                    warning_msg("Error parsing XML: Unexpected XML structure.")
            except Exception as e:
                warning_msg("Error parsing XML: " + str(e))
        except Exception as e:
            warning_msg("Error processing coverages list: " + str(e))

    # modify a server entry
    def editServer(self):
        global config
        flags = Qt.WindowTitleHint | Qt.WindowSystemMenuHint | Qt.WindowMinimizeButtonHint | Qt.WindowMaximizeButtonHint

        idx = self.cmbConnections_Serv.currentIndex()
        if idx < len(config.srv_list['servers']):
            select_serv = config.srv_list['servers'][idx]

            print("Selection: ", idx, " -- ", select_serv, " -- Check: ", serv[idx])

            dlgEdit = qgsnewhttpconnectionbase(self, flags, toEdit=True, choice=idx)
            dlgEdit.txt_NewSrvName.setText(select_serv[0])
            dlgEdit.txt_NewSrvUrl.setText(select_serv[1])
            dlgEdit.show()
            self.btnConnectServer_Serv.setFocus(True)

    # ---------------

    # ---------------
    # delete a server entry
    def deleteServer(self):
        global config
        idx = self.cmbConnections_Serv.currentIndex()
        if idx < len(config.srv_list['servers']):
            config.srv_list['servers'].pop(idx)

        self.write_srv_list()
        self.updateServerListing()
        self.btnConnectServer_Serv.setFocus(True)

    # ---------------

    # ---------------
    # update the server-listing shown in the selectionBar
    def updateServerListing(self):
        global serv
        global config

        # print("btnUpdateServerListing:  here we are updating the ServerList....")
        serv = []
        config.srv_list = config.read_srv_list()
        for ii in range(len(config.srv_list['servers'])):
            serv.append(config.srv_list['servers'][ii][0][:])

        self.cmbConnections_Serv.clear()
        self.cmbConnections_Serv.addItems(serv)

    # ---------------

    # ---------------
    # write the sever names/urls to a file
    @mouse_busy
    def write_srv_list(self):
        plugin_dir = os.path.dirname(os.path.realpath(__file__))
        outsrvlst = os.path.join(plugin_dir, 'config_srvlist.pkl')
        fo = open(outsrvlst, 'wb')
        pickle.dump(config.srv_list, fo, 0)
        fo.close()

    # ---------------

    # ---------------
    # get the path where the downloaded datasets shall be stored
    @mouse_busy
    def get_outputLoc(self):
        global req_outputLoc

        start_dir = os.getenv("HOME")
        req_outputLoc = QFileDialog.getExistingDirectory(self, "Select Output Path", start_dir)
        if len(req_outputLoc) > 0:
            if not req_outputLoc.endswith(os.sep):
                req_outputLoc = req_outputLoc + os.sep

    def get_auth_credentials(self):
        username = self.UserNameLine.text()
        password = self.PasswordLine.text()
        return username, password

    ## ====== End of Server section ======
    @mouse_busy
    def exeProcessCoverage(self):
        global req_outputLoc
        global authMgr
        global mode
        global selected_serv
        global selected_url

        self.get_auth_credentials()

        selected_serv, selected_url = self.get_serv_url()
        query = self.plainTextEdit_PC.toPlainText()
        if query is None:
            msg = "Please enter a query"
            warning_msg(msg)
            return
        req_outputLoc = ""
        input_param = {'query': query,
                       'outputDir': req_outputLoc,
                       'serv_url': selected_url
                       }

        username, password = self.get_auth_credentials()
        process_output = self.myWCPS.ProcessCoverage(input_param, username, password)

        outLoc = ''
        mimetype = ''
        dialogMessage = ''
        status = process_output.get('status', -1)

        if ('outfile' in process_output):
            outLoc = process_output['outfile']

        if ('mimetype' in process_output):
            mimetype = process_output['mimetype']

        if status == 200:
            datatype = mimetype.split('/')
            if datatype[0] == "image" or datatype[0] == "application":
                self.dlg.show()
            else:
                showData = open(outLoc, 'r')
                dialogMessage = showData.read()
                myDisplay_txt = display_txt(self)
                myDisplay_txt.textBrowser_Disp.setText(dialogMessage)
                myDisplay_txt.show()
        else:
            myDisplay_txt = display_txt(self)
            print(process_output['message'])
            myDisplay_txt.textBrowser_Disp.setText(process_output['message'])
            myDisplay_txt.show()

    ## ====== Add data to Map Canvas ======
    # read the the downloaded datasets, register them and show them in the QGis MapCanvas
    def add_to_map(self, req_params):

        self.canvas = self.iface.mapCanvas()

        fileID = req_params['outfile']
        disp_image = glob(fileID)
        print(disp_image)
        # check if there is a loadable coverage availabel (and not eg. an multipart/related gml) or an error occurred
        if len(disp_image) > 0:
            imgInfo = QFileInfo(disp_image[-1])
            img_baseName = imgInfo.baseName()
            img_layer = QgsRasterLayer(disp_image[-1], img_baseName)
            if not img_layer.isValid():
                warning_msg("Layer failed to load!")
        else:
            msg = "Could not load file"
            warning_msg(msg)

        QgsProject.instance().addMapLayer(img_layer)

    # load Query from file
    def loadQuery(self):
        start_dir = os.getenv("HOME")
        queryFilePath = QFileDialog.getOpenFileName(self, "Select Query File", start_dir)
        self.plainTextEdit_PC.setPlainText(open(queryFilePath[0], 'r').read())

    # save Query to file
    def storeQuery(self):
        query = self.plainTextEdit_PC.toPlainText()
        start_dir = os.getenv("HOME")
        queryFilePath = QFileDialog.getSaveFileName(self, "Save Query File", start_dir)
        if len(queryFilePath[0]) > 0:
            open(queryFilePath[0], 'w').write(query)

    ## ====== End of Add data to Map Canvas ======
    @mouse_busy
    def saveQuery(self):
        global req_outputLoc
        global selected_serv
        global selected_url
        # selected_serv, selected_url = self.get_serv_url()

        query = self.plainTextEdit_PC.toPlainText()
        self.get_outputLoc()
        input_param = {'query': query,
                       'outputDir': req_outputLoc,
                       'serv_url': selected_url
                       }

        username, password = self.get_auth_credentials()
        self.myWCPS.ProcessCoverage(input_param, username, password)

    def showQuery(self):
        global req_outputLoc
        global selected_serv
        global selected_url
        query = self.plainTextEdit_PC.toPlainText()
        input_param = {'query': query,
                       'outputDir': req_outputLoc,
                       'serv_url': selected_url
                       }
        username, password = self.get_auth_credentials()
        self.add_to_map(self.myWCPS.ProcessCoverage(input_param, username, password))

    def saveAndShowQuery(self):
        global req_outputLoc
        self.get_outputLoc()
        self.showQuery()

    def addDatacube(self):
        global coverages
        global selected_coverages
        global variables
        coverages_names = []
        for coverage in coverages:
            if 'wcs:CoverageId' in coverage:
                coverages_names.append(coverage['wcs:CoverageId'])

        dialog = CoverageSelectionDialog(coverages_names, self)
        result = dialog.exec_()
        if result == QDialog.Accepted:
            selected_coverages_temp, itervar = dialog.getSelectedCoverages()
            variables.append(itervar)
            self.lstDatacubes.clear()
            self.lstDatacubes.addItems(selected_coverages_temp)
            selected_coverages.append(selected_coverages_temp)

    def deleteDatacube(self):
        global selected_coverages
        selected_items = self.lstDatacubes.selectedItems()
        if not selected_items:  # No items selected
            return

        for item in selected_items:
            row = self.lstDatacubes.row(item)
            self.lstDatacubes.takeItem(row)
            selected_coverages[len(selected_coverages) - 1].remove(item.text())

    def setFilterCondition(self):
        global filterCondition
        filterCondition = ""
        filterCondition, ok = QInputDialog.getText(self, 'Set Filter Condition', 'Enter Filter Condition:', text="")

    def setFormatParameters(self):
        global formatParameters
        formatParameters = ""
        formatParameters, ok = QInputDialog.getText(self, 'Set Format Parameters', 'Enter Format Parameters:', text="")

    def evaluateQuery(self):
        global selected_coverages
        global formatParameters
        global filterCondition
        global variables

        # 1. Input Datacubes (the for clause)
        for_clause = ""
        if len(selected_coverages) == 1:
            for_clause = f"for {variables[0]} in ({', '.join(selected_coverages[0])})\n"
        else:
            for_clause = f"for {variables[0]} in ({', '.join(selected_coverages[0])}),\n"
        for i in range(1, len(selected_coverages)):
            if i == len(selected_coverages) - 1:
                for_clause += f"     {variables[i]} in ({', '.join(selected_coverages[i])})\n"
            else:
                for_clause += f"     {variables[i]} in ({', '.join(selected_coverages[i])}),\n"
        # 2. Filter Datacubes (the where clause)
        # Assuming filterCondition is a string or None
        where_clause = ""
        if filterCondition not in ["", None]:
            where_clause = f"where {filterCondition}"

        # 3. Result Expression (the return clause)
        return_expr = self.ResultExpression.toPlainText()

        # 4. Result Format
        resultType = self.ResultFormat.currentText()
        if resultType.lower() == 'none':
            format_clause = ""
        elif resultType.lower() == 'custom':
            format_clause = f', "{self.CustomFormatLine.text()}"'
        else:
            format_clause = f", {resultType}"

        # Append format parameters if any
        if formatParameters not in ["", None]:
            format_clause += f',  "{formatParameters}"'

        # Construct the final query
        if (resultType.lower() != 'none'):
            query = f"{for_clause}{where_clause}\nreturn encode({return_expr}{format_clause})"
        else:
            query = f"{for_clause}{where_clause}\nreturn {return_expr}"

        self.plainTextEdit_PC.setPlainText(query)
        self.exeProcessCoverage()
