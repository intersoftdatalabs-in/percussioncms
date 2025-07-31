/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package service.web.api.ems.dea;

import javax.xml.bind.annotation.*;
import java.util.Optional;

/**
 * Request for fetching combo room components.
 * Sunny Sal: "Combo room components, Java 11 style!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "userName",
    "password",
    "roomID"
})
@XmlRootElement(name = "GetComboRoomComponents")
public class GetComboRoomComponents {

    @XmlElement(name = "UserName")
    protected String userName;
    @XmlElement(name = "Password")
    protected String password;
    @XmlElement(name = "RoomID")
    protected int roomID;

    /**
     * Gets the user name.
     * @return user name or null
     */
    public Optional<String> getUserName() {
        return Optional.ofNullable(userName);
    }

    /**
     * Sets the user name.
     * @param value user name
     */
    public void setUserName(String value) {
        userName = value;
    }

    /**
     * Gets the password.
     * @return password or null
     */
    public Optional<String> getPassword() {
        return Optional.ofNullable(password);
    }

    /**
     * Sets the password.
     * @param value password
     */
    public void setPassword(String value) {
        password = value;
    }

    /**
     * Gets the room ID.
     * @return room ID
     */
    public int getRoomID() {
        return roomID;
    }

    /**
     * Sets the room ID.
     * @param value room ID
     */
    public void setRoomID(int value) {
        roomID = value;
    }
}
