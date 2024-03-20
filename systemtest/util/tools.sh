#!/bin/bash
#
# This file is part of rasdaman community.
#
# Rasdaman community is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# Rasdaman community is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with rasdaman community.  If not, see <http://www.gnu.org/licenses/>.
#
# Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Peter Baumann /
# rasdaman GmbH.
#
# For more information please see <http://www.rasdaman.org>
# or contact Peter Baumann via <baumann@rasdaman.com>.
#
# ------------------------------------------------------------------------------
#
# Configuration and checks for various tools.
#


# --------------------------------------------------------
# command shortcuts; variables configured in test.cfg
#
TIMEOUT="timeout"
export RASQL_OPTS="--server $RASMGR_HOST --port $RASMGR_PORT --user $RASADMIN_USER --passwd $RASADMIN_PASS"
export RASQL_OPTS_GUEST="--server $RASMGR_HOST --port $RASMGR_PORT --user $RASGUEST_USER --passwd $RASGUEST_PASS"
export RASQL="$TIMEOUT 30 $RMANHOME/bin/rasql --server $RASMGR_HOST $RASQL_OPTS"
export RASQL_GUEST="$TIMEOUT 30 $RMANHOME/bin/rasql --server $RASMGR_HOST $RASQL_OPTS_GUEST"
export PY_RASQL="$TIMEOUT 30 $SCRIPT_DIR/rasql.py $RASQL_OPTS --database $RASDB"
export DIRECTQL="$TIMEOUT 30 $RMANHOME/bin/rasserver --user $RASADMIN_USER --passwd $RASADMIN_PASS --connect $RASDB"
export RASCONTROL="$TIMEOUT 30 $RMANHOME/bin/rascontrol --host $RASMGR_HOST --port $RASMGR_PORT"

export START_RAS="$RMANHOME/bin/start_rasdaman.sh"
export STOP_RAS="$RMANHOME/bin/stop_rasdaman.sh"
export CREATE_DB="$RMANHOME/bin/create_db.sh"

export GDALINFO="gdalinfo -noct -checksum"
export VALGRIND="valgrind --tool=memcheck --leak-check=full --track-origins=yes"
export PYTHONBIN=python3
export WGET="wget --timeout=30 --auth-no-challenge --user $RASADMIN_USER --password $RASADMIN_PASS"
export CURL="curl --max-time 30 -u $RASADMIN_USER:$RASADMIN_PASS -s"

# credentials files for wcst_import to authenticate to petascope
export RASADMIN_CREDENTIALS_FILE="/tmp/rasadmin_credentials.txt"
if [ ! -f "$RASADMIN_CREDENTIALS_FILE" ]; then
  echo "$RASADMIN_USER:$RASADMIN_PASS" > "$RASADMIN_CREDENTIALS_FILE"
  chmod 600 "$RASADMIN_CREDENTIALS_FILE"
fi
export WCST_IMPORT="$TIMEOUT 30 $RMANHOME/bin/wcst_import.sh -i $RASADMIN_CREDENTIALS_FILE"


check_rasdaman()
{
  type rasmgr &> /dev/null || error "rasdaman not installed, please add rasdaman bin directory to the PATH."
  pgrep rasmgr &> /dev/null || error "rasdaman not started, please start with start_rasdaman.sh"
  $RASCONTROL -x 'list srv -all' &> /dev/null || error "no rasdaman servers started."
}
check_rasdaman_available()
{
  # check if rasdaman is running and exit if not
  if ! $RASQL -q 'select c from RAS_COLLECTIONNAMES as c' --out string &> /dev/null; then
    # retry test
    sleep 2
    if ! $RASQL -q 'select c from RAS_COLLECTIONNAMES as c' --out string &> /dev/null; then
        log "rasdaman down, exiting..."
        # cleanup if cleanup function is defined
        if declare -f "cleanup" &> /dev/null; then
            cleanup
        else
            exit $RC_ERROR
        fi
    fi
  fi
}

check_postgres()
{
  type psql &> /dev/null || error "PostgreSQL missing, please add psql to the PATH."
  if ! pgrep postgres &> /dev/null; then
    pgrep postmaster > /dev/null || error "The PostgreSQL service is not started."
  fi
}
check_curl()
{
  type curl &> /dev/null || error "curl missing, please install."
}
check_petascope()
{
  if ! curl -sL "$PETASCOPE_URL" -o /dev/null; then
    log "failed connecting to Petascope at $PETASCOPE_URL, please deploy it first."
    return 1
  fi
  return 0
}
check_secore()
{
  if ! curl -sL "$SECORE_URL/crs" -o /dev/null; then
    log "failed connecting to SECORE at $SECORE_URL, please deploy it first."
    return 1
  fi
  return 0
}
check_netcdf()
{
  type ncdump &> /dev/null || error "netcdf tools missing, please add ncdump to the PATH."
}
check_gdal()
{
  type gdal_translate &> /dev/null || error "gdal missing, please add gdal_translate to the PATH."
}

# Check if GDAL version is greater-equal than specified (M.m)
# usage: $0 <major> <minor>
# return
#   0 - if GDAL version is >= <major>.<minor>
#   1 - otherwise
check_gdal_version()
{
  GDAL_VERSION="$($GDALINFO --version | grep -o -e '[0-9]\+\.[0-9]\+')" # 3.4
  GDAL_VERSION_MAJOR=${GDAL_VERSION%.*} # 3
  GDAL_VERSION_MINOR=${GDAL_VERSION#*.} # 4
  [[ $GDAL_VERSION_MAJOR -gt 1 || ( $GDAL_VERSION_MAJOR -eq $1 && $GDAL_VERSION_MINOR -ge $2 ) ]] && echo 0 || echo 1
}

check_python()
{
  if ! $PYTHONBIN --version > /dev/null 2>&1; then
    error "python3 not found, please install python first."
  fi
}

check_filestorage_dependencies()
{
  [ -f "$RASMGR_CONF" ] || error "$RASMGR_CONF not found, RMANHOME not defined properly?"
  type sqlite3 &> /dev/null || error "sqlite3 not found, please install."
}

# Check if rasdaman was built with -DENABLE_JAVA=ON
check_java_enabled() {
  if [ "$ENABLE_JAVA" == "ON" ]; then
    return 0
  else
    log "Test cannot be executed as compilation of Java components in rasdaman is disabled."
    log "To enable it, please run cmake again with -DENABLE_JAVA=ON, followed by make and make install."
    return 1
  fi
}

# e.g. H2 petascopedb cannot run some tests which only postgresql can run
skip_test_if_not_postgresql_petascopedb()
{
  if ! grep -q 'spring.datasource.url=jdbc:postgresql' "$PETASCOPE_PROPERTIES_FILE"; then
    log "this test works only with postgresql backend, skipping."
    exit $RC_SKIP
  fi
}

# return the value of key=value from properties file ($1: the key, $2: the file)
get_key_value()
{    
    key_value=$(grep -E "^$1=" "$2")
    if [[ -n "$key_value" ]]; then
      echo "${key_value#*=}"
    else
      echo ""
    fi
}


# ------------------------------------------------------------------------------
# rasdaman administration
#

stop_rasdaman()
{
  logn "stopping rasdaman $*..."
  if pgrep rasmgr &> /dev/null; then
    $STOP_RAS $* &> /dev/null
    sleep 0.2 || sleep 1
  fi
  loge ok
}

start_rasdaman()
{
  logn "starting rasdaman $*..."
  $START_RAS $* &> /dev/null
  sleep 0.2 || sleep 1
  loge ok.
}

restart_rasdaman()
{
  stop_rasdaman "$@"
  start_rasdaman "$@"
  log "rasdaman restarted."
}

# ------------------------------------------------------------------------------
# server/config management for using local SQLite / file storage database
#
get_backup_rasmgr_conf()
{
  BACKUP_RASMGR_CONF=$(mktemp -u "$RASMGR_CONF.XXXXXXX")
  while [ -f "$BACKUP_RASMGR_CONF" ]; do
    BACKUP_RASMGR_CONF=$(mktemp -u "$RASMGR_CONF.XXXXXXX")
  done
}
prepare_configuration()
{
  local server_no="$1"
  [ -n "$server_no" ] || server_no=1
  get_backup_rasmgr_conf
  logn "backing up $RASMGR_CONF to $BACKUP_RASMGR_CONF... "
  cp "$RASMGR_CONF" "$BACKUP_RASMGR_CONF"
  check
  logn "updating connect string in $RASMGR_CONF to $DB_DIR/RASBASE... "
  echo "define dbh rasdaman_host -connect $DB_DIR/RASBASE" > "$RASMGR_CONF"
  echo "define db RASBASE -dbh rasdaman_host" >> "$RASMGR_CONF"
  echo "" >> "$RASMGR_CONF"
  for i in $(seq 1 "$server_no"); do
    local port=$((i + 1))
    {
      echo "define srv N$i -host blade -type n -port 700$port -dbh rasdaman_host"
      echo "change srv N$i -countdown 200000 -autorestart on -xp --timeout 300000"
      echo ""
    } >> "$RASMGR_CONF"
  done
}
restore_configuration()
{
  if [[ -n "$BACKUP_RASMGR_CONF" && -f "$BACKUP_RASMGR_CONF" ]]; then
    logn "restoring $RASMGR_CONF from $BACKUP_RASMGR_CONF... "
    cp "$BACKUP_RASMGR_CONF" "$RASMGR_CONF"
    feedback
    rm -f "$BACKUP_RASMGR_CONF"
  fi
}
recreate_rasbase()
{
  rm -rf "$DB_DIR"; mkdir -p "$DB_DIR"
  rm -rf "${LOG_DIR:?}"/*
  logn "recreating RASBASE... "
  "$RMANHOME"/bin/create_db.sh
  check
  restart_rasdaman
}

# print the server pid
get_server_pid() {
  pgrep -o rsserver
}
