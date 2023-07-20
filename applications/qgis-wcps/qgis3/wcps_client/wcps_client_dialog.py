# -*- coding: utf-8 -*-

from __future__ import print_function
from builtins import range
import os, sys, pickle
from glob import glob

from qgis.core import *
from qgis.gui import *

from PyQt5.QtWidgets import QProgressDialog, QDialog, QMessageBox, QFileDialog, QApplication, QPushButton
from PyQt5.QtGui import QCursor
from PyQt5.QtNetwork import QNetworkRequest, QNetworkAccessManager
from PyQt5.QtCore import Qt, QFileInfo
from PyQt5 import QtXml

from .succesful_query_dialog import succesful_query_dialog
from .wcps_client_dialog_base import Ui_WCPSClient
from .qgsnewhttpconnectionbasedialog import qgsnewhttpconnectionbase
from .display_txtdialog import display_txt



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
    saveQuerySignal = QtCore.pyqtSignal()

    def __init__(self, iface):
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
        flags = Qt.WindowTitleHint | Qt.WindowSystemMenuHint | Qt.WindowMinimizeButtonHint | Qt.WindowMaximizeButtonHint
        self.dlg = succesful_query_dialog(self, flags, toEdit=False, choice='')
        self.dlg.saveQuerySignal.connect(self.saveQuery)
        self.dlg.showQuerySignal.connect(self.showQuery)
        self.dlg.saveAndShowQuerySignal.connect(self.saveAndShowQuery)

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

    # ---------------
    # check if the url exist and if we get a respond to a simple OWS request
    @mouse_busy
    def connectServer(self):
        global config
        global serv

        selected_serv, selected_url = self.get_serv_url()
        print('You choose: ', selected_serv, "URL:", selected_url)

        msg = "Your choice:    " + selected_serv + "\n"
        msg = msg + "URL:                   " + selected_url + "\n"
        self.textBrowser_Serv.setText(msg)
        if not self.tab_PC.isEnabled():
            self.tab_PC.setEnabled(True)

        QApplication.changeOverrideCursor(Qt.ArrowCursor)

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
                print(mode)
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
        #selected_serv, selected_url = self.get_serv_url()

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

