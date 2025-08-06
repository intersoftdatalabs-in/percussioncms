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

package com.percussion.rest.assets;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;
import java.util.Optional;

@XmlRootElement(name = "Flash")
@JsonInclude(Include.NON_NULL)
@Schema(description = "Represents a binary Flash file.")
public class Flash extends ImageInfo {

    private String flashVersion;
    private String usage;

    public Optional<String> getFlashVersion() {
        return Optional.ofNullable(flashVersion);
    }

    public void setFlashVersion(String flashVersion) {
        this.flashVersion = flashVersion;
    }

    public Optional<String> getUsage() {
        return Optional.ofNullable(usage);
    }

    public void setUsage(String usage) {
        this.usage = usage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Flash)) return false;
        if (!super.equals(o)) return false;
        var that = (Flash) o;
        return Objects.equals(flashVersion, that.flashVersion)
                && Objects.equals(usage, that.usage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), flashVersion, usage);
    }

    @Override
    public String toString() {
        return "Flash{" +
                "flashVersion='" + flashVersion + '\'' +
                ", usage='" + usage + '\'' +
                "} " + super.toString();
    }
}
