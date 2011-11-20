#!/bin/bash
dir_bin=`dirname $0`
path_cmd=$dir_bin/filebot
sudo ln -s "$path_cmd" /usr/bin/filebot
