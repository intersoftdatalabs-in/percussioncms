#!/bin/sh
java -cp rxmisctools.jar:rxclient.jar:sharetest.jar:log4j.jar com.percussion.test.util.itemcreator.PSItemCreator $1
