/*
 *     Percussion CMS
 *     Copyright (C) 1999-2021 Percussion Software, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     Mailing Address:
 *
 *      Percussion Software, Inc.
 *      PO Box 767
 *      Burlington, MA 01803, USA
 *      +01-781-438-9900
 *      support@percussion.com
 *      https://www.percussion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

package com.percussion.share.extension;

import com.percussion.delivery.service.impl.PSDeliveryInfoService;
import com.percussion.error.PSExceptionUtils;
import com.percussion.security.PSEncryptor;
import com.percussion.server.IPSStartupProcess;
import com.percussion.server.IPSStartupProcessManager;
import com.percussion.server.PSServer;
import com.percussion.utils.io.PathUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
     * Startup process that will check and rotate secureKey if Required
     *
     * @author Santosh Dhariwal
     *
     */
    public class PSRotateSecureKey extends TimerTask implements IPSStartupProcess {

        private static final Logger log = LogManager.getLogger(PSRotateSecureKey.class.getName());
        private static final String SECURE_KEY_ROTATION_TIME_PROP = "secureKeyRotationTime";
        private static final int SECURE_KEY_ROTATION_TIME_DEFAULT = 0;


        public PSRotateSecureKey(){

        }

        public void run()
        {
            rotateKey();
        }

        /***
         * Allow for running from the command line
         * @param args
         */
        public static void main(String[] args){
            Properties props = new Properties();
            props.setProperty(PSRotateSecureKey.class.getSimpleName(),"true");
            PSRotateSecureKey run = new PSRotateSecureKey();


            try {
                run.doStartupWork(props);
            } catch (Exception e) {
                log.error(PSExceptionUtils.getMessageForLog(e));
                log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            }
        }

    /**
     * This method triggers a rotateKey on PSEncryptor
     */
    private static void rotateKey(){
            try {
                PSEncryptor.rotateKey("AES",
                        PathUtils.getRxDir(null).getAbsolutePath().concat(PSEncryptor.SECURE_DIR));
                PSDeliveryInfoService.copySecureKeyToDeliveryServer(null);
             }catch (Exception e){
                log.error("Key rotation failed with error: {}", PSExceptionUtils.getMessageForLog(e));
                log.debug("PSRotateKey Failed ERROR: {}",PSExceptionUtils.getMessageForLog(e));
            }
        }

        @Override
        public void doStartupWork(Properties startupProps) {

            if (!"true".equalsIgnoreCase(startupProps.getProperty(getPropName()))) {
                log.info(getPropName() + " is set to false or missing from startup properties file. Nothing to run.");
                return;
            }
            try {
                String rotationTime = PSServer.getServerProps().getProperty(SECURE_KEY_ROTATION_TIME_PROP, String.valueOf(SECURE_KEY_ROTATION_TIME_DEFAULT));
                long rotationDays = Long.parseLong(rotationTime);
                //Don't do rotation
                if(rotationDays <= 0){
                    return;
                }
//                if (rotationDays == 0) {
//                    rotationDays = SECURE_KEY_ROTATION_TIME_DEFAULT;
//                }
                String keyLocation = PathUtils.getRxDir(null).getAbsolutePath().concat(PSEncryptor.SECURE_DIR);
                String SECURE_KEY_FILE = ".key";
                Path secureKeyFile = Paths.get(keyLocation + SECURE_KEY_FILE);
                BasicFileAttributes attr =
                        Files.readAttributes(secureKeyFile, BasicFileAttributes.class);
                FileTime lastModified = attr.lastModifiedTime();

                long ft = lastModified.toMillis();
                long now = System.currentTimeMillis();
                long diff = now - ft;
                long days = TimeUnit.MILLISECONDS.toDays(diff);
                if (days > rotationDays) {
                    log.info("Rotating the system security key based as it is {} days old based on the policy setting {}={} ...",days,SECURE_KEY_ROTATION_TIME_PROP, rotationDays);
                    rotateKey();
                }else{
                    //Set Timer for those many days to rotate
                    Timer timer = new Timer();
                    timer.schedule(this, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days));
                }
            }catch (Exception e){
                log.error("Key rotation failed with error: {}", PSExceptionUtils.getMessageForLog(e));
                log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            }
        }

        @Override
        public void setStartupProcessManager(IPSStartupProcessManager mgr) {
            mgr.addStartupProcess(this);
        }

        static String getPropName() {
            return PSRotateSecureKey.class.getSimpleName();
        }


}
