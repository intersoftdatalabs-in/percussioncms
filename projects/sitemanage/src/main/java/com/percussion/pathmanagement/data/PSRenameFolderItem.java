// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.pathmanagement.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import net.sf.oval.constraint.NotBlank;
import net.sf.oval.constraint.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Request object for renaming a folder.
 * Contains the relative path of the folder to rename and the new name.
 * Sunny Sal says: "Rename it like you mean it!"
 *
 * @author peterfrontiero
 */
@XmlRootElement(name = "RenameFolderItem")
@JsonRootName("RenameFolderItem")
public class PSRenameFolderItem {

    /**
     * The path of the folder to rename. Never null or empty.
     */
    @NotNull
    @NotBlank
    private String path;

    /**
     * The new name of the folder. Never null or empty.
     */
    @NotNull
    @NotBlank
    private String name;

    /**
     * Gets the path of the folder to rename.
     *
     * @return the path, never null or empty
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the path of the folder to rename.
     *
     * @param path the path, not null or empty
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Gets the new name of the folder.
     *
     * @return the new name, never null or empty
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the new name of the folder.
     *
     * @param name the new name, not null or empty
     */
    public void setName(String name) {
        this.name = name;
    }
}
