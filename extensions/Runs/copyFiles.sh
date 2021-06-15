#!/bin/bash

mkdir -v ITNtoHouses2-2

cd ITNtoHouses2-2

echo Copying png files .....
sshpass -p 'B@cc!@nE10' scp fp36@dcc-slogin-01.oit.duke.edu:*.png .

wait

echo Copying out files .....
sshpass -p 'B@cc!@nE10' scp fp36@dcc-slogin-01.oit.duke.edu:*.out .

wait

echo Copying out files .....
sshpass -p 'B@cc!@nE10' scp fp36@dcc-slogin-01.oit.duke.edu:*.xls .

wait

echo File tranfert done ..................
