/*
 * This file is part of rasdaman community.
 *
 * Rasdaman community is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Rasdaman community is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with rasdaman community.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2003 - 2019 Peter Baumann /
 rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */

///<reference path="../../_all.ts"/>

module rasdaman {
    /**
     * This class holds the state of the OWS client and orchestrates
     * interaction between the tabs and state transfer.
     * All the other Admin controllers will inherit 
     * this ****controller's scope****.
     */
    export class RootController {
        public static $inject = ["$http", "$q", "$scope", "$rootScope",
            "$state", "rasdaman.WCSSettingsService", "rasdaman.ErrorHandlingService",
            "rasdaman.CredentialService", "$window", "rasdaman.LoginService"
        ];

        public constructor(private $http: angular.IHttpService,
            private $q: angular.IQService,
            private $scope: RootControllerScope,
            private $rootScope: angular.IRootScopeService,
            private $state: any,
            private settings: rasdaman.WCSSettingsService,
            private errorHandlingService: rasdaman.ErrorHandlingService,
            private credentialService: rasdaman.CredentialService,
            private $window: ng.IWindowService,
            private loginService: rasdaman.LoginService ) {

            this.initializeViews($scope);

            $scope.selectedView = $scope.login;
            
            // After user logged in then it has more roles
            this.$rootScope.userLoggedInRoles = [];

            $rootScope.homeLoggedIn = null;
            $rootScope.usernameLoggedIn = "";
            $rootScope.authenticationEnabled = false;

            $scope.displayAdminTab = false;

            // When logged in in the first page, then shows the main WSClient
            // NOTE: this works only
            $rootScope.$watch("homeLoggedIn", (newValue:any) => {
                if (newValue === true) {             
                    // Check if Admin tab should be displayed 
                    $scope.displayAdminTab = AdminService.hasRole($rootScope.userLoggedInRoles, AdminService.PRIV_OWS_UPDATE_SRV);

                    $scope.showView($scope.wsclient, "services");
                }
            });

            // Show target view
            $scope.showView = function(viewState:ViewState, stateName:string): void {                
                $scope.selectedView = viewState;
                $state.go(stateName);
            }

            // Logout and go to login page, clear storage
            $scope.homeLogOutEvent = function() {
                credentialService.clearStorage();
                $rootScope.homeLoggedIn = null;
                // $scope.showView($scope.login, "login");
                location.reload();
            }

            // Show login page when user didn't have to login before
            $scope.homeLogInEvent = function() {
                // set to false, then it doesn't show the Login button at the top right corner
                $rootScope.homeLoggedIn = null; 
                $scope.showView($scope.login, "login");
            }            

            // -------------------- invoke functions -------------------
            
            // Check which form should be shown
            loginService.checkPetascopeEnableAuthentication()
                .then(function(data) {
                    $rootScope.authenticationEnabled = data["basic_authentication_header_enabled"];

                    if (data["basic_authentication_header_enabled"] == true && data["rasdaman_user"] == "") {
                        // NOTE: Petascope with enabled authentication and no default user is set -> show login form
                        $rootScope.homeLoggedIn = null;           
                        
                        // Check if the stored credentials are still valid
                        let result = loginService.checkStoredRadamanCredentials();
                        if (result == true) {
                            $scope.showView($scope.wsclient, "services");
                        } else {
                            // username and password changed, need to reauthenticate
                            $scope.showView($scope.login, "login");
                        }
                    } else {
                        // no need to authenticate if authentication not enabled or basic header is enabled but there is an implicit user enabled for that                        
                        $rootScope.homeLoggedIn = false;

                        // In case user has logged in with a different username before, then try it instead of using the default user from petascope
                        let result = loginService.checkStoredRadamanCredentials();
                        
                        if (result == true) {
                            // 1. stored credentials are good -> use them
                            $rootScope.wcsReloadServerCapabilities = true;
                            $rootScope.wmsReloadServerCapabilities = true;

                            $scope.showView($scope.wsclient, "services");
                        } else if (result == false) {
                            // 2. password in server changed, stored passwd on local is not correct anymore, then it needs to relogin
                            $rootScope.homeLoggedIn = null;

                            $scope.showView($scope.login, "login");
                        } else {
                            // 3. there is no stored credentials, just use the default user set by petascope
                            $rootScope.wcsReloadServerCapabilities = true;
                            $rootScope.wmsReloadServerCapabilities = true;
                            
                            $scope.showView($scope.wsclient, "services");
                        }
                    }
                });
        }

        private initializeViews($scope: RootControllerScope) {
            $scope.login = {
                view: "login"
            };
            $scope.wsclient = {
                view: "wsclient"
            };
        }

    }

    export interface RootControllerScope extends angular.IScope {
        homeLoggedIn: false;
        login: ViewState;
        wsclient: ViewState;
        selectedView: ViewState;
        
        checkPetascopeEnableAuthentication(): angular.IPromise<any>;
        checkRadamanCredentials(): void;
        showView(view:ViewState, stateName:string): void;
        homeLogOutEvent(): void;
    }

    interface ViewState {
        view: string;
    }
}
