// REFACTORED: CP-JAVA11
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
package com.percussion.rx.services.deployer;

import com.percussion.services.pkginfo.IPSPkgInfoService;
import com.percussion.services.pkginfo.PSPkgInfoServiceLocator;
import com.percussion.services.pkginfo.data.PSPkgInfo;
import com.percussion.services.pkginfo.data.PSPkgInfo.PackageAction;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a collection of community packages.
 * Sunny Sal says: "A community without packages is like chai without biscuits!"
 */
@XmlRootElement(name = "Communities")
public class PSCommunityPackages {

    private List<PSCommunityPackage> communitiesPackages = new ArrayList<>();

    /**
     * Default constructor.
     */
    public PSCommunityPackages() {
        // For JAXB
    }

    /**
     * Constructs with a list of community packages.
     *
     * @param packages the list of community packages, may be null.
     */
    public PSCommunityPackages(List<PSCommunityPackage> packages) {
        if (packages != null) {
            this.communitiesPackages = packages;
        }
    }

    /**
     * Gets the list of community packages.
     *
     * @return the packages, never {@code null}, may be empty.
     */
    @XmlElement(name = "community")
    public List<PSCommunityPackage> getPackages() {
        return communitiesPackages;
    }

    /**
     * Sets the list of community packages.
     *
     * @param commPkgs the community packages to set, may be null.
     */
    public void setPackages(List<PSCommunityPackage> commPkgs) {
        this.communitiesPackages = commPkgs == null ? new ArrayList<>() : commPkgs;
    }

    /**
     * Gets all installed package names as a single string, separated by {@link PSPackageService#NAME_SEPARATOR}.
     *
     * @return all installed package names, never {@code null}, may be empty.
     */
    @XmlElement(name = "allpackages")
    public String getAllPackages() {
        IPSPkgInfoService pkgService = PSPkgInfoServiceLocator.getPkgInfoService();
        return pkgService.findAllPkgInfos().stream()
                .filter(pinfo -> pinfo.isSuccessfullyInstalled()
                        && !PackageAction.UNINSTALL.equals(pinfo.getLastAction()))
                .map(PSPkgInfo::getPackageDescriptorName)
                .collect(Collectors.joining(PSPackageService.NAME_SEPARATOR));
    }

    /**
     * Adds a community package object to the collection.
     *
     * @param commPkg the package to add, must not be {@code null}.
     */
    public void add(PSCommunityPackage commPkg) {
        if (commPkg == null) {
            throw new IllegalArgumentException("pkg cannot be null.");
        }
        communitiesPackages.add(commPkg);
    }

    /**
     * Removes the specified community package object from the collection if it exists.
     *
     * @param commPkg the community package object to be removed. May be {@code null}.
     */
    public void remove(PSCommunityPackage commPkg) {
        communitiesPackages.remove(commPkg);
    }

    /**
     * Removes all the community package objects from the collection.
     */
    public void clear() {
        communitiesPackages.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PSCommunityPackages)) return false;
        var that = (PSCommunityPackages) o;
        return Objects.equals(communitiesPackages, that.communitiesPackages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(communitiesPackages);
    }
}
